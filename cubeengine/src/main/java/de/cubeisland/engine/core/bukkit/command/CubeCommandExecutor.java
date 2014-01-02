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
package de.cubeisland.engine.core.bukkit.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import de.cubeisland.engine.core.command.AliasCommand;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.command.exception.CommandException;
import de.cubeisland.engine.core.command.exception.IncorrectUsageException;
import de.cubeisland.engine.core.command.exception.MissingParameterException;
import de.cubeisland.engine.core.command.exception.PermissionDeniedException;
import de.cubeisland.engine.core.command.sender.BlockCommandSender;
import de.cubeisland.engine.core.command.sender.WrappedCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import de.cubeisland.engine.core.Core;
import de.cubeisland.engine.core.CubeEngine;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command.CommandResult;
import de.cubeisland.engine.core.command.CubeCommand;
import de.cubeisland.engine.core.command.HelpContext;
import de.cubeisland.engine.core.util.StringUtils;

import static de.cubeisland.engine.core.util.StringUtils.startsWithIgnoreCase;

public class CubeCommandExecutor implements CommandExecutor, TabCompleter
{
    private static final int TAB_LIMIT_THRESHOLD = 50;
    
    private final CubeCommand command;

    public CubeCommandExecutor(CubeCommand command)
    {
        assert command != null: "The command may not be null!";
        
        this.command = command;
    }

    public CubeCommand getCommand()
    {
        return command;
    }
    
    private static CommandContext toCommandContext(CubeCommand command, CommandSender sender, String label, String[] args)
    {
        Stack<String> labels = new Stack<>();
        labels.push(label);
        
        if (args.length > 0 && !args[0].isEmpty())
        {
            while (args.length > 0)
            {
                if ("?".equals(args[0]))
                {
                    return new HelpContext(command, sender, labels, Arrays.copyOfRange(args, 1, args.length));
                }
                CubeCommand child = command.getChild(args[0]);
                if (child == null)
                {
                    break;
                }
                if (!child.isAuthorized(sender))
                {
                    throw new PermissionDeniedException(child.getPermission());
                }
                command = child;
                labels.push(args[0]);
                args = Arrays.copyOfRange(args, 1, args.length);
            }
        }
        
        if (command instanceof AliasCommand)
        {
            AliasCommand aliasCommand = (AliasCommand)command;
            String[] prefix = aliasCommand.getPrefix();
            String[] suffix = aliasCommand.getSuffix();

            String[] newArgs = new String[prefix.length + args.length + suffix.length];
            System.arraycopy(prefix, 0, newArgs, 0, prefix.length);
            System.arraycopy(args, 0, newArgs, prefix.length, args.length);
            System.arraycopy(suffix, 0, newArgs, prefix.length + args.length, suffix.length);

            args = newArgs;
        }
        
        return command.getContextFactory().parse(command, sender, labels, args);
    }

    @Override
    public boolean onCommand(final org.bukkit.command.CommandSender bukkitSender, final Command bukkitCommand, final String label, final String[] args)
    {
        final CommandSender sender = wrapSender(this.command.getModule().getCore(), bukkitSender);
        
        try
        {
            final CommandContext ctx = toCommandContext(this.command, sender, label, args);
            if (ctx instanceof HelpContext)
            {
                ctx.getCommand().help((HelpContext)ctx);
                return true;
            }
            if (this.command.isAsynchronous())
            {
                ctx.getCore().getTaskManager().getThreadFactory().newThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        run0(ctx);
                    }
                }).start();
            }
            else
            {
                this.run0(ctx);
            }
        }
        catch (CommandException e)
        {
            this.handleCommandException(sender, e);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(final org.bukkit.command.CommandSender bukkitSender, final Command bukkitCommand, final String label, final String[] args)
    {
        final Core core = this.command.getModule().getCore();
        final CommandSender sender = wrapSender(core, bukkitSender);
        
        try
        {
            CommandContext context = toCommandContext(this.command, sender, label, args);

            List<String> result = this.completeChild(context);
            if (result == null)
            {
                result = context.getCommand().tabComplete(context);
                if (result == null)
                {
                    result = bukkitCommand.tabComplete(bukkitSender, label, args);
                }
            }
            
            final int max = core.getConfiguration().commands.maxTabCompleteOffers;
            if (result.size() > max)
            {
                if (StringUtils.implode(", ", result).length() < TAB_LIMIT_THRESHOLD)
                {
                    return result;
                }
                result = result.subList(0, max);
            }
            return result;
        }
        catch (Exception e)
        {
            this.handleCommandException(sender, e);
        }
        
        return null;
    }
    
    protected final List<String> completeChild(CommandContext context)
    {
        CubeCommand command = context.getCommand();
        if (command.hasChildren() && context.getArgCount() == 1)
        {
            List<String> actions = new ArrayList<>();
            String token = context.getString(0).toLowerCase(Locale.ENGLISH);

            CommandSender sender = context.getSender();
            Set<CubeCommand> names = command.getChildren();
            for (CubeCommand child : names)
            {
                if (startsWithIgnoreCase(child.getName(), token) && child.isAuthorized(sender))
                {
                    actions.add(child.getName());
                }
            }
            Collections.sort(actions, String.CASE_INSENSITIVE_ORDER);

            return actions;
        }
        return null;
    }


    private static CommandSender wrapSender(Core core, org.bukkit.command.CommandSender bukkitSender)
    {
        if (bukkitSender instanceof CommandSender)
        {
            return (CommandSender)bukkitSender;
        }
        else if (bukkitSender instanceof Player)
        {
            return core.getUserManager().getExactUser(bukkitSender.getName());
        }
        else if (bukkitSender instanceof org.bukkit.command.ConsoleCommandSender)
        {
            return core.getCommandManager().getConsoleSender();
        }
        else if (bukkitSender instanceof org.bukkit.command.BlockCommandSender)
        {
            return new BlockCommandSender(core, (org.bukkit.command.BlockCommandSender)bukkitSender);
        }
        else
        {
            return new WrappedCommandSender(core, bukkitSender);
        }
    }

    private void run0(CommandContext ctx)
    {
        try
        {
            CommandResult result = ctx.getCommand().run(ctx);
            if (result != null)
            {
                result.show(ctx);
            }
        }
        catch (Exception e)
        {
            handleCommandException(ctx.getSender(), e);
        }
    }

    private void handleCommandException(final CommandSender sender, Throwable t)
    {
        if (!CubeEngine.isMainThread())
        {
            final Throwable tmp = t;
            sender.getCore().getTaskManager().callSync(new Callable<Void>()
            {
                @Override
                public Void call() throws Exception
                {
                    handleCommandException(sender, tmp);
                    return null;
                }
            });
            return;
        }
        if (t instanceof InvocationTargetException || t instanceof ExecutionException)
        {
            t = t.getCause();
        }
        if (t instanceof MissingParameterException)
        {
            sender.sendTranslated("&cThe parameter &6%s&c is missing!", t.getMessage());
        }
        else if (t instanceof IncorrectUsageException)
        {
            IncorrectUsageException e = (IncorrectUsageException)t;
            if (e.getMessage() != null)
            {
                sender.sendMessage(t.getMessage());
            }
            else
            {
                sender.sendTranslated("&cThat seems wrong...");
            }
            if (e.getDisplayUsage())
            {
                sender.sendTranslated("&eProper usage: &f%s", this.command.getUsage(sender));
            }
        }
        else if (t instanceof PermissionDeniedException)
        {
            PermissionDeniedException e = (PermissionDeniedException)t;
            if (e.getMessage() != null)
            {
                sender.sendMessage(e.getMessage());
            }
            else
            {
                sender.sendTranslated("&cYou're not allowed to do this!");
                sender.sendTranslated("&cContact an administrator if you think this is a mistake!");
            }
            sender.sendTranslated("&cMissing permission: &6%s", e.getPermission());
        }
        else
        {
            sender.sendTranslated("&4An unknown error occurred while executing this command!");
            sender.sendTranslated("&4Please report this error to an administrator.");
            this.command.getModule().getLog().debug(t, t.getLocalizedMessage());
        }
    }
}
