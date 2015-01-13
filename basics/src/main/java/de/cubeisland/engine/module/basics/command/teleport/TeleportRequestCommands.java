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
package de.cubeisland.engine.module.basics.command.teleport;

import java.util.UUID;

import org.bukkit.Bukkit;

import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

/**
 * Contains Teleport-Request commands.
 * /tpa
 * /tpahere
 * /tpaccept
 * /tpdeny
 */
public class TeleportRequestCommands
{
    private final Basics module;

    public TeleportRequestCommands(Basics module)
    {
        this.module = module;
    }

    @Command(desc = "Requests to teleport to a player.")
    @Params(positional = @Param(label = "player", type = User.class))
    public void tpa(CommandContext context)
    {
        User sender = null;
        if (context.getSource() instanceof User)
        {
            sender = (User)context.getSource();
        }
        if (sender == null)
        {
            context.sendTranslated(NEGATIVE, "{text:Pro Tip}: Teleport does not work IRL!");
            return;
        }
        sender.get(BasicsAttachment.class).removeTpRequestCancelTask();
        final User user = context.get(0);
        if (!user.isOnline())
        {
            context.sendTranslated(NEGATIVE, "{user} is not online!");
            return;
        }
        user.sendTranslated(POSITIVE, "{sender} wants to teleport to you!", sender);
        user.sendTranslated(NEUTRAL, "Use {text:/tpaccept} to accept or {text:/tpdeny} to deny the request!");
        user.get(BasicsAttachment.class).setPendingTpToRequest(sender.getUniqueId());
        user.get(BasicsAttachment.class).removePendingTpFromRequest();
        context.sendTranslated(POSITIVE, "Teleport request sent to {user}!", user);
        int waitTime = this.module.getConfiguration().commands.teleportRequestWait * 20;
        if (waitTime > 0)
        {
            final User sendingUser = sender;
            final int taskID = module.getCore().getTaskManager().runTaskDelayed(this.module, new Runnable()
            {
                public void run()
                {
                    user.get(BasicsAttachment.class).removeTpRequestCancelTask();
                    user.get(BasicsAttachment.class).removePendingTpToRequest();
                    sendingUser.sendTranslated(NEGATIVE, "{user} did not accept your teleport request.", user);
                    user.sendTranslated(NEGATIVE, "Teleport request of {sender} timed out.", sendingUser);
                }
            }, waitTime); // wait x - seconds
            Integer oldtaskID = user.get(BasicsAttachment.class).getTpRequestCancelTask();
            if (oldtaskID != null)
            {
                module.getCore().getTaskManager().cancelTask(this.module, oldtaskID);
            }
            user.get(BasicsAttachment.class).setTpRequestCancelTask(taskID);
        }
    }

    @Command(desc = "Requests to teleport a player to you.")
    @Params(positional = @Param(label = "player", type = User.class))
    public void tpahere(CommandContext context)
    {
        if (context.getSource() instanceof User)
        {
            User sender = (User)context.getSource();
            sender.get(BasicsAttachment.class).removeTpRequestCancelTask();
            final User user = context.get(0);
            if (!user.isOnline())
            {
                context.sendTranslated(NEGATIVE, "{user} is not online!");
                return;
            }
            user.sendTranslated(POSITIVE, "{sender} wants to teleport you to them!", sender);
            user.sendTranslated(NEUTRAL, "Use {text:/tpaccept} to accept or {text:/tpdeny} to deny the request!");
            user.get(BasicsAttachment.class).setPendingTpFromRequest(sender.getUniqueId());
            user.get(BasicsAttachment.class).removePendingTpToRequest();
            context.sendTranslated(POSITIVE, "Teleport request send to {user}!", user);
            int waitTime = this.module.getConfiguration().commands.teleportRequestWait * 20;
            if (waitTime > 0)
            {
                final User sendingUser = sender;
                final int taskID = module.getCore().getTaskManager().runTaskDelayed(this.module, new Runnable()
                {
                    public void run()
                    {
                        user.get(BasicsAttachment.class).removeTpRequestCancelTask();
                        user.get(BasicsAttachment.class).removePendingTpFromRequest();
                        sendingUser.sendTranslated(NEGATIVE, "{user} did not accept your teleport request.", user);
                        user.sendTranslated(NEGATIVE, "Teleport request of {sender} timed out.", sendingUser);
                    }
                }, waitTime); // wait x - seconds
                Integer oldtaskID = user.get(BasicsAttachment.class).getTpRequestCancelTask();
                if (oldtaskID != null)
                {
                    module.getCore().getTaskManager().cancelTask(this.module, oldtaskID);
                }
                user.get(BasicsAttachment.class).setTpRequestCancelTask(taskID);
            }
            return;
        }
        context.sendTranslated(NEGATIVE, "{text:Pro Tip}: Teleport does not work IRL!");
    }

    @Command(alias = "tpac", desc = "Accepts any pending teleport request.")
    public void tpaccept(CommandContext context)
    {
        if (context.getSource() instanceof User)
        {
            User sender = (User)context.getSource();
            UUID uuid = sender.get(BasicsAttachment.class).getPendingTpToRequest();
            if (uuid == null)
            {
                uuid = sender.get(BasicsAttachment.class).getPendingTpFromRequest();
                if (uuid == null)
                {
                    context.sendTranslated(NEGATIVE, "You don't have any pending requests!");
                    return;
                }
                sender.get(BasicsAttachment.class).removePendingTpFromRequest();
                User user = this.module.getCore().getUserManager().getExactUser(uuid);
                if (user == null || !user.isOnline())
                {
                    context.sendTranslated(NEGATIVE, "{user} seems to have disappeared.", user);
                    return;
                }
                if (!TeleportCommands.teleport(sender, user.getLocation(), true, false, true))
                    return;
                user.sendTranslated(POSITIVE, "{user} accepted your teleport request!", sender);
                context.sendTranslated(POSITIVE, "You accepted a teleport to {user}!", user);
            }
            else
            {
                sender.get(BasicsAttachment.class).removePendingTpToRequest();
                User user = this.module.getCore().getUserManager().getExactUser(uuid);
                if (user == null || !user.isOnline())
                {
                    context.sendTranslated(NEGATIVE, "{user} seems to have disappeared.", Bukkit.getPlayer(uuid).getName());
                    return;
                }
                if (!TeleportCommands.teleport(user, sender.getLocation(), true, false, true))
                    return;
                user.sendTranslated(POSITIVE, "{user} accepted your teleport request!", sender);
                context.sendTranslated(POSITIVE, "You accepted a teleport to {user}!", user);
            }
            Integer taskID = sender.get(BasicsAttachment.class).getTpRequestCancelTask();
            if (taskID != null)
            {
                sender.get(BasicsAttachment.class).removeTpRequestCancelTask();
                module.getCore().getTaskManager().cancelTask(this.module, taskID);
            }
            return;
        }
        context.sendTranslated(NEGATIVE, "No one wants to teleport to you!");
    }

    @Command(desc = "Denies any pending teleport request.")
    public void tpdeny(CommandContext context)
    {
        User sender = null;
        if (context.getSource() instanceof User)
        {
            sender = (User)context.getSource();
        }
        if (sender == null)
        {
            context.sendTranslated(NEGATIVE, "No one wants to teleport to you!");
            return;
        }
        UUID tpa =  sender.get(BasicsAttachment.class).getPendingTpToRequest();
        UUID tpahere = sender.get(BasicsAttachment.class).getPendingTpFromRequest();
        if (tpa != null)
        {
            sender.get(BasicsAttachment.class).removePendingTpToRequest();
            User user = this.module.getCore().getUserManager().getExactUser(tpa);
            if (user == null)
            {
                throw new IllegalStateException("Player saved in \"pendingTpToRequest\" was not found!");
            }
            user.sendTranslated(NEGATIVE, "{user} denied your teleport request!", sender);
            context.sendTranslated(NEGATIVE, "You denied {user}'s teleport request!", user);
        }
        else if (tpahere != null)
        {
            sender.get(BasicsAttachment.class).removePendingTpFromRequest();
            User user = this.module.getCore().getUserManager().getExactUser(tpahere);
            if (user == null)
            {
                throw new IllegalStateException("User saved in \"pendingTpFromRequest\" was not found!");
            }
            user.sendTranslated(NEGATIVE, "{user} denied your request!", sender);
            context.sendTranslated(NEGATIVE, "You denied {user}'s teleport request", user);
        }
        else
        {
            context.sendTranslated(NEGATIVE, "You don't have any pending requests!");
            return;
        }
        Integer taskID = sender.get(BasicsAttachment.class).getTpRequestCancelTask();
        if (taskID != null)
        {
            sender.get(BasicsAttachment.class).removeTpRequestCancelTask();
            module.getCore().getTaskManager().cancelTask(this.module, taskID);
        }
    }
}
