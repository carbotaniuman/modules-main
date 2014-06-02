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
package de.cubeisland.engine.module.roles.commands;

import org.bukkit.World;

import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.command.ContainerCommand;
import de.cubeisland.engine.core.command.CubeContext;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.core.world.WorldManager;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.role.RolesAttachment;
import de.cubeisland.engine.module.roles.role.RolesManager;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

public class UserCommandHelper extends ContainerCommand
{
    protected final RolesManager manager;
    protected final WorldManager worldManager;
    protected final Roles module;

    protected final String LISTELEM = "- " + ChatFormat.YELLOW + "%s";
    protected final String LISTELEM_VALUE = "- " + ChatFormat.YELLOW + "%s" + ChatFormat.WHITE + ": " + ChatFormat.GOLD + "%s";

    public UserCommandHelper(Roles module)
    {
        super(module, "user", "Manage users.");
        this.manager = module.getRolesManager();
        this.worldManager = module.getCore().getWorldManager();
        this.module = module;
    }

    protected User getUser(CubeContext context, int pos)
    {
        User user = null;
        if (context.hasIndexed(pos))
        {
            user = context.getArg(pos);
        }
        else
        {
            if (context.getSender() instanceof User)
            {
                user = (User)context.getSender();
            }
            if (user == null)
            {
                context.sendTranslated(NEGATIVE, "You have to specify a player.");
                return null;
            }
        }
        return user;
    }

    /**
     * Returns the world defined with named param "in" or the users world
     *
     * @param context
     * @return
     */
    protected World getWorld(CubeContext context)
    {
        World world;
        if (context.hasNamed("in"))
        {
            world = context.getArg("in");
            if (world == null)
            {
                context.sendTranslated(NEGATIVE, "World {world} not found!", context.getString("in"));
            }
            return world;
        }
        CommandSender sender = context.getSender();
        if (sender instanceof User)
        {
            User user = (User)sender;
            world = user.attachOrGet(RolesAttachment.class, this.module).getWorkingWorld();
            if (world == null)
            {
                world = user.getWorld();
            }
            else
            {
                context.sendTranslated(NEUTRAL, "You are using {world} as current world.", world);
            }
            return world;
        }
        if (ManagementCommands.curWorldOfConsole == null)
        {
            context.sendTranslated(NEUTRAL, "Please provide a world.");
            context.sendTranslated(POSITIVE, "You can define a world with {text:/roles admin defaultworld <world>}");
            return null;
        }
        world = ManagementCommands.curWorldOfConsole;
        context.sendTranslated(NEUTRAL, "You are using {world} as current world.", world);
        return world;
    }
}