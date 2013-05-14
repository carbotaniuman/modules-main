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
package de.cubeisland.cubeengine.conomy.commands;

import java.util.Collection;

import de.cubeisland.cubeengine.core.command.CommandContext;
import de.cubeisland.cubeengine.core.command.CommandResult;
import de.cubeisland.cubeengine.core.command.ContainerCommand;
import de.cubeisland.cubeengine.core.command.parameterized.Flag;
import de.cubeisland.cubeengine.core.command.parameterized.Param;
import de.cubeisland.cubeengine.core.command.parameterized.ParameterizedContext;
import de.cubeisland.cubeengine.core.command.reflected.Alias;
import de.cubeisland.cubeengine.core.command.reflected.Command;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.core.util.StringUtils;
import de.cubeisland.cubeengine.conomy.Conomy;
import de.cubeisland.cubeengine.conomy.ConomyPermissions;
import de.cubeisland.cubeengine.conomy.Currency;
import de.cubeisland.cubeengine.conomy.account.Account;
import de.cubeisland.cubeengine.conomy.account.AccountManager;
import de.cubeisland.cubeengine.conomy.account.storage.AccountModel;

public class MoneyCommand extends ContainerCommand
{
    private Conomy module;
    private AccountManager manager;
    private Currency currency;

    public MoneyCommand(Conomy module)
    {
        super(module, "money", "Manages your money.");
        this.module = module;
        this.manager = module.getManager();
        this.currency = this.manager.getCurrency();
    }

    @Override
    public CommandResult run(CommandContext context) throws Exception
    {
        if (context.hasArg(0))
        {
            return super.run(context);
        }
        else
        {
            this.balance((ParameterizedContext)context);
            return null;
        }
    }

    @Alias(names = {
        "balance", "moneybalance"
    })
    @Command(desc = "Shows your balance",
             usage = "[player]",
             flags = @Flag(longName = "showHidden", name = "f"),
             max = 1)
    public void balance(ParameterizedContext context)
    {
        User user;
        boolean showHidden = context.hasFlag("f") && ConomyPermissions.ACCOUNT_SHOWHIDDEN.isAuthorized(context.getSender());
        if (context.hasArg(0))
        {
            user = context.getUser(0);
            if (user == null)
            {
                context.sendTranslated("&cUser %s not found!", context.getString(0));
                return;
            }
        }
        else
        {
            if (!(context.getSender() instanceof User))
            {
                context.sendTranslated("&cYou are out of money! Better go work than typing silly commands in the console.");
                return;
            }
            user = (User)context.getSender();
        }
        Account account = this.manager.getUserAccount(user.getName(), false);
        if (account != null)
        {
            if (!account.isHidden() || showHidden)
            {
                context.sendTranslated("&2%s's &aBalance: &6%s", user.getName(), currency.format(account.balance()));
                return;
            }
        }
        context.sendTranslated("&cNo account found for &2%s&c!", user.getName());
    }

    @Alias(names = {"toplist", "balancetop", "topmoney"})
    @Command(desc = "Shows the players with the highest balance.",
             usage = "[[fromRank]-ToRank]", max = 1,
             flags = @Flag(longName = "showhidden", name = "f"))
    public void top(ParameterizedContext context)
    {
        boolean showHidden = context.hasFlag("f");
        if (showHidden)
        {
            if (!ConomyPermissions.ACCOUNT_SHOWHIDDEN.isAuthorized(context.getSender()))
                showHidden = false;
        }
        int fromRank = 1;
        int toRank = 10;
        if (context.hasArg(0))
        {
            try
            {
                String range = context.getString(0);
                if (range.contains("-"))
                {
                    fromRank = Integer.parseInt(range.substring(0, range.indexOf("-")));
                    range = range.substring(range.indexOf("-") + 1);
                }
                toRank = Integer.parseInt(range);
            }
            catch (NumberFormatException e)
            {
                context.sendTranslated("&cInvalid rank!");
                return;
            }
        }
        Collection<AccountModel> models = this.manager.getTopUserAccounts(fromRank, toRank, showHidden);
        int i = fromRank;
        if (fromRank == 1)
        {
            context.sendTranslated("&aTop Balance &f(&6%d&f)", models.size());
        }
        else
        {
            context.sendTranslated("&aTop Balance from &6%d &ato &6%d", fromRank, fromRank + models.size() -1);
        }
        for (AccountModel account : models)
        {
            context.sendTranslated("&a%d &f- &2%s&f: &6%s", i++,
                    this.module.getCore().getUserManager().getUser(account.user_id).getName(), currency.format(account.value / currency.fractionalDigitsFactor()));
        }
    }

    @Alias(names = {
        "pay"
    })
    @Command(names = {"pay", "give"},
             desc = "Transfer the given amount to another account.",
             usage = "<player> [as <player>] <amount>",
             params = @Param(names = "as", type = User.class),
             flags = @Flag(longName = "force", name = "f"),
             min = 2, max = 2)
    public void pay(ParameterizedContext context)
    {
        String amountString = context.getString(1);
        Double amount = currency.parse(amountString);
        if (amount == null)
        {
            context.sendTranslated("&cCould not parse amount!");
            return;
        }
        String format = currency.format(amount);
        User sender;
        boolean asSomeOneElse = false;
        if (context.hasParam("as"))
        {
            sender = context.getUser("as");
            if (sender == null)
            {
                context.sendTranslated("&cUser %s not found!", context.getString("as"));
                return;
            }
            asSomeOneElse = true;
        }
        else
        {
            if (!(context.getSender() instanceof User))
            {
                context.sendTranslated("&cPlease specify a user to use his account.");
                return;
            }
            sender = (User)context.getSender();
        }
        Account source = this.manager.getUserAccount(sender.getName(), false);
        if (source == null)
        {
            context.sendTranslated("&2%s &cdoes not have an account for &6%s&c!",
                                   sender.getName(), currency.getName());
            return;
        }
        String[] users = StringUtils.explode(",", context.getString(0));
        for (String userString : users)
        {
            User user = this.module.getCore().getUserManager().findUser(userString);
            if (user == null)
            {
                context.sendTranslated("&cUser %s not found!", context.getString(0));
                continue;
            }
            Account target = this.manager.getUserAccount(user.getName(), false);
            if (target == null)
            {
                context.sendTranslated("&2%s &cdoes not have an account for &6%s&c!",
                                       sender.getName(), currency.getName());
                continue;
            }
            if (!(context.hasFlag("f") && ConomyPermissions.COMMAND_PAY_FORCE.isAuthorized(context.getSender()))) //force allowed
            {
                if (!source.has(amount))
                {
                    if (asSomeOneElse)
                    {
                        context.sendTranslated("&2%s&c cannot afford &6%s&c!", sender.getName(), format);
                    }
                    else
                    {
                        context.sendTranslated("&cYou cannot afford &6%s&c!", format);
                    }
                    return;
                }
            }
            if (this.manager.transaction(source, target, amount, false))
            {
                if (asSomeOneElse)
                {
                    context.sendTranslated("&6%s&a transferred from &2%s's&a to &2%s's&a account!", format, sender.getName(), user.getName());
                }
                else
                {
                    context.sendTranslated("&6%s&a transferred to &2%s's&a account!", format, user.getName());
                }
                user.sendTranslated("&2%s&a just payed you &6%s!", sender.getName(), format);
            }
            else
            {
                context.sendTranslated("&cThe Transaction was not successful!");
            }
        }
    }
}
