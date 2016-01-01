/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.conomy;

import org.cubeengine.module.conomy.storage.AccountModel;
import org.cubeengine.module.conomy.storage.BalanceModel;
import org.cubeengine.service.database.Database;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.economy.transaction.TransferResult;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toMap;
import static org.cubeengine.module.conomy.storage.TableBalance.TABLE_BALANCE;
import static org.spongepowered.api.service.economy.transaction.ResultType.CONTEXT_MISMATCH;
import static org.spongepowered.api.service.economy.transaction.ResultType.SUCCESS;
import static org.spongepowered.api.service.economy.transaction.TransactionTypes.DEPOSIT;
import static org.spongepowered.api.service.economy.transaction.TransactionTypes.TRANSFER;
import static org.spongepowered.api.service.economy.transaction.TransactionTypes.WITHDRAW;

public abstract class BaseAccount implements Account
{
    // TODO bank-account access
    // member;admin;owner
    // deposit;withdraw;manage
    // permission based? <- may be too slow when getting all users /w perm for it
    // ce.conomy.access.<id>.depoosit
    // ce.conomy.access.<id>.withdraw
    // ce.conomy.access.<id>.manage
    // when useracc do not check for permission

    /* TODO top-accs
     public Collection<AccountModel> getTopAccounts(boolean user, boolean bank, int fromRank, int toRank,
                                                   boolean showHidden)
     return this.dsl.selectFrom(TABLE_ACCOUNT).
            where(DSL.condition("((mask & 1) = 0 OR ((mask & 1) = 1) = " + showHidden + ")")).
                           and(DSL.condition("(name IS NULL) = " + user + " OR " + "(user_id IS NULL) = " + bank)).
                           orderBy(TABLE_ACCOUNT.VALUE.desc()).limit(fromRank - 1, toRank - fromRank + 1).
                           fetch();
     */

    // TODO parse currency // TODO filter currency Names / Symbols http://git.cubeisland.de/cubeengine/cubeengine/issues/250
    // rename bank / player(on name change)

    private ConomyService service;
    private AccountModel account;
    private Database db;
    private Map<Currency, Map<Context, BalanceModel>> balance = new HashMap<>();

    public BaseAccount(ConomyService service, AccountModel account, Database db)
    {
        this.service = service;
        this.account = account;
        this.db = db;
    }

    private Optional<BalanceModel> getModel(ConfigCurrency currency, Set<Context> ctxs)
    {
        Map<Context, BalanceModel> balances = this.balance.get(currency);
        Optional<Context> relevantCtx = currency.getRelevantContext(ctxs);
        if (!relevantCtx.isPresent())
        {
            return Optional.empty();
        }
        BalanceModel balanceModel = balances.get(relevantCtx.get());
        if (balanceModel == null)
        {
            balanceModel = db.getDSL().newRecord(TABLE_BALANCE).newBalance(account, currency, relevantCtx.get(), getDefaultBalance(currency));
            balanceModel.storeAsync();
            balances.put(relevantCtx.get(), balanceModel);
        }

        return Optional.of(balanceModel);
    }

    private ConfigCurrency cast(Currency currency)
    {
        if (currency instanceof ConfigCurrency)
        {
            return ((ConfigCurrency) currency);
        }
        throw new IllegalArgumentException("Unknown Currency: " + currency.getDisplayName() + " " + currency.getClass().getName());
    }

    @Override
    public BigDecimal getDefaultBalance(Currency currency)
    {
        return ((ConfigCurrency) currency).getDefaultBalance(this);
    }

    @Override
    public boolean hasBalance(Currency currency, Set<Context> contexts)
    {
        ConfigCurrency cur = cast(currency);
        return getModel(cur, contexts).isPresent();
    }

    @Override
    public BigDecimal getBalance(Currency currency, Set<Context> contexts)
    {
        // TODO javadocs are unclear what to do if currency is not found in context
        // If no Balance available then ZERO
        ConfigCurrency cur = cast(currency);
        return getModel(cur, contexts)
                .map(BalanceModel::getBalance)
                .map(cur::fromLong)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public Map<Currency, BigDecimal> getBalances(Set<Context> ctxs)
    {
        return service.getCurrencies().stream()
                .collect(toMap(c -> c, c -> getBalance(c, ctxs)));
    }

    @Override
    public TransactionResult setBalance(Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts)
    {
        ConfigCurrency cur = cast(currency);
        Optional<BalanceModel> model = getModel(cur, contexts);
        if (model.isPresent())
        {
            model.get().setBalance(cur.toLong(amount));
            return new Result(this, currency, amount, contexts, SUCCESS, DEPOSIT, cause); // TODO TransactionType?
        }
        return new Result(this, currency, amount, contexts, CONTEXT_MISMATCH, DEPOSIT, cause);  // TODO TransactionType?
    }

    @Override
    public TransactionResult resetBalances(Cause cause, Set<Context> contexts)
    {
        // TODO what is the result supposed to be?
        TransactionResult last = null;
        for (Currency currency : service.getCurrencies())
        {
            last = resetBalance(currency, cause, contexts);
        }
        return last;
    }

    @Override
    public TransactionResult resetBalance(Currency currency, Cause cause, Set<Context> contexts)
    {
        return setBalance(currency, getDefaultBalance(currency), cause, contexts);
    }

    @Override
    public TransactionResult deposit(Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts)
    {
        ConfigCurrency cur = cast(currency);
        Optional<BalanceModel> model = getModel(cur, contexts);
        if (model.isPresent())
        {
            model.get().setBalance(model.get().getBalance() + cur.toLong(amount));
            // TODO check max
            return new Result(this, currency, amount, contexts, SUCCESS, DEPOSIT, cause);
        }
        return new Result(this, currency, amount, contexts, CONTEXT_MISMATCH, DEPOSIT, cause);
    }

    @Override
    public TransactionResult withdraw(Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts)
    {
        ConfigCurrency cur = cast(currency);
        Optional<BalanceModel> model = getModel(cur, contexts);
        if (model.isPresent())
        {
            model.get().setBalance(model.get().getBalance() - cur.toLong(amount));
            // TODO check min
            return new Result(this, currency, amount, contexts, SUCCESS, WITHDRAW, cause);
        }
        return new Result(this, currency, amount, contexts, CONTEXT_MISMATCH, WITHDRAW, cause);
    }

    @Override
    public TransferResult transfer(Account to, Currency currency, BigDecimal amount, Cause cause, Set<Context> contexts)
    {
        // TODO disallow user -> bank if not at least member of bank
        // TODO disallow bank -> user if cause is not admin?
        // TODO check for visibility of account for causer
        TransactionResult result = this.withdraw(currency, amount, cause, contexts);
        if (result.getResult() == SUCCESS)
        {
            result = to.deposit(currency, amount, cause, contexts);
            if (result.getResult() == SUCCESS)
            {
                return new Result.Transfer(this, to, currency, amount, contexts, SUCCESS, TRANSFER, cause);
            }
            else
            {
                this.deposit(currency, amount, cause, contexts); // rollback withdraw action
                return new Result.Transfer(this, to, currency, amount, contexts, result.getResult(), TRANSFER, cause);
            }
        }
        else
        {
            return new Result.Transfer(this, to, currency, amount, contexts, result.getResult(), TRANSFER, cause);
        }
    }

    @Override
    public Set<Context> getActiveContexts()
    {
        return service.getActiveContexts(this);
    }
}
