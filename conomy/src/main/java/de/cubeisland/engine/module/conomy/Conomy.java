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
package de.cubeisland.engine.module.conomy;

import de.cubeisland.engine.module.conomy.account.BankAccount;
import de.cubeisland.engine.module.conomy.account.ConomyManager;
import de.cubeisland.engine.module.conomy.account.storage.TableAccount;
import de.cubeisland.engine.module.conomy.account.storage.TableBankAccess;
import de.cubeisland.engine.module.conomy.commands.BankCommands;
import de.cubeisland.engine.module.conomy.commands.BankOrAllReader;
import de.cubeisland.engine.module.conomy.commands.BankReader;
import de.cubeisland.engine.module.conomy.commands.EcoBankCommands;
import de.cubeisland.engine.module.conomy.commands.EcoCommands;
import de.cubeisland.engine.module.conomy.commands.MoneyCommand;
import de.cubeisland.engine.core.command.ArgumentReader;
import de.cubeisland.engine.core.command.CommandManager;
import de.cubeisland.engine.core.module.Module;
import de.cubeisland.engine.core.module.service.Economy;
import de.cubeisland.engine.core.storage.database.Database;

public class Conomy extends Module
{
    private ConomyConfiguration config;
    private ConomyManager manager;
    private ConomyPermissions perms;

    @Override
    public void onEnable()
    {
        Database db = this.getCore().getDB();
        db.registerTable(TableAccount.class);
        db.registerTable(TableBankAccess.class);

        this.config = this.loadConfig(ConomyConfiguration.class);
        this.manager = new ConomyManager(this);
        perms = new ConomyPermissions(this);
        final CommandManager cm = this.getCore().getCommandManager();
        cm.registerCommand(new MoneyCommand(this));
        cm.registerCommand(new EcoCommands(this));

        ArgumentReader.registerReader(new BankReader(this.manager), BankAccount.class);
        ArgumentReader.registerReader(new BankOrAllReader());
        cm.registerCommand(new BankCommands(this));
        cm.registerCommand(new EcoBankCommands(this), "eco");
        this.getCore().getModuleManager().getServiceManager().registerService(this, Economy.class, manager.getInterface());
    }

    public ConomyConfiguration getConfig()
    {
        return this.config;
    }

    public ConomyManager getManager()
    {
        return manager;
    }

    public ConomyPermissions perms()
    {
        return this.perms;
    }
}