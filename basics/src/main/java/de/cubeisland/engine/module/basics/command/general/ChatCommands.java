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
package de.cubeisland.engine.module.basics.command.general;

import java.sql.Timestamp;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command.sender.ConsoleCommandSender;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.user.UserManager;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.core.util.TimeUtil;
import de.cubeisland.engine.core.util.converter.DurationConverter;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;
import de.cubeisland.engine.module.basics.storage.BasicsUserEntity;
import de.cubeisland.engine.reflect.exception.ConversionException;
import de.cubeisland.engine.reflect.node.StringNode;
import org.joda.time.Duration;

import static de.cubeisland.engine.command.parameter.property.Greed.INFINITE_GREED;
import static de.cubeisland.engine.core.command.CommandSender.NON_PLAYER_UUID;
import static de.cubeisland.engine.core.util.ChatFormat.YELLOW;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;
import static de.cubeisland.engine.module.basics.storage.TableBasicsUser.TABLE_BASIC_USER;

public class ChatCommands
{
    private final DurationConverter converter = new DurationConverter();
    private final UserManager um;
    private final Basics module;

    private UUID lastWhisperOfConsole = null;

    public ChatCommands(Basics basics)
    {
        this.module = basics;
        this.um = basics.getCore().getUserManager();
    }



    @Command(desc = "Sends a private message to someone", alias = {"tell", "message", "pm", "m", "t", "whisper", "w"})
    @Params(positional = {@Param(label = "player", type = User.class), // TODO staticValues = "console",
                         @Param(label = "message", greed = INFINITE_GREED)})
    public void msg(CommandContext context)
    {
        if ("console".equalsIgnoreCase(context.getString(0)))
        {
            sendWhisperTo(NON_PLAYER_UUID, context.getStrings(1), context);
            return;
        }
        User user = context.get(0);
        if (!this.sendWhisperTo(user.getUniqueId(), context.getStrings(1), context))
        {
            context.sendTranslated(NEGATIVE, "Could not find the player {user} to send the message to. Is the player offline?", user);
        }
    }

    @Command(alias = "r", desc = "Replies to the last person that whispered to you.")
    @Params(positional = @Param(label = "message", greed = INFINITE_GREED))
    public void reply(CommandContext context)
    {
        UUID lastWhisper;
        if (context.getSource() instanceof User)
        {
            lastWhisper = ((User)context.getSource()).get(BasicsAttachment.class).getLastWhisper();
        }
        else
        {
            lastWhisper = lastWhisperOfConsole;
        }
        if (lastWhisper == null)
        {
            context.sendTranslated(NEUTRAL, "No one has sent you a message that you could reply to!");
            return;
        }
        if (!this.sendWhisperTo(lastWhisper, context.getStrings(0), context))
        {
            context.sendTranslated(NEGATIVE, "Could not find the player to reply to. Is the player offline?");
        }
    }

    private boolean sendWhisperTo(UUID whisperTarget, String message, CommandContext context)
    {
        if (NON_PLAYER_UUID.equals(whisperTarget))
        {
            if (context.getSource() instanceof ConsoleCommandSender)
            {
                context.sendTranslated(NEUTRAL, "Talking to yourself?");
                return true;
            }
            if (context.getSource() instanceof User)
            {
                ConsoleCommandSender console = context.getCore().getCommandManager().getConsoleSender();
                console.sendTranslated(NEUTRAL, "{sender} -> {text:You}: {message:color=WHITE}", context.getSource(), message);
                context.sendTranslated(NEUTRAL, "{text:You} -> {user}: {message:color=WHITE}", console.getDisplayName(), message);
                this.lastWhisperOfConsole = context.getSource().getUniqueId();
                ((User)context.getSource()).get(BasicsAttachment.class).setLastWhisper(NON_PLAYER_UUID);
                return true;
            }
            context.sendTranslated(NONE, "Who are you!?");
            return true;
        }
        User user = um.getExactUser(whisperTarget);
        if (!user.isOnline())
        {
            return false;
        }
        if (context.getSource().equals(user))
        {
            context.sendTranslated(NEUTRAL, "Talking to yourself?");
            return true;
        }
        user.sendTranslated(NONE, "{sender} -> {text:You}: {message:color=WHITE}", context.getSource().getName(), message);
        if (user.get(BasicsAttachment.class).isAfk())
        {
            context.sendTranslated(NEUTRAL, "{user} is afk!", user);
        }
        context.sendTranslated(NEUTRAL, "{text:You} -> {user}: {message:color=WHITE}", user, message);
        if (context.getSource() instanceof User)
        {
            ((User)context.getSource()).get(BasicsAttachment.class).setLastWhisper(user.getUniqueId());
        }
        else
        {
            this.lastWhisperOfConsole = user.getUniqueId();
        }
        user.get(BasicsAttachment.class).setLastWhisper(context.getSource().getUniqueId());
        return true;
    }

    @Command(desc = "Broadcasts a message")
    @Params(positional = @Param(label = "message", greed = INFINITE_GREED))
    public void broadcast(CommandContext context)
    {
        this.um.broadcastMessage(NEUTRAL, "[{text:Broadcast}] {input}", context.getStrings(0));
    }

    @Command(desc = "Mutes a player")
    @Params(positional = {@Param(label = "player", type = User.class),
                        @Param(label = "duration", req = false)})
    public void mute(CommandContext context)
    {
        User user = context.get(0);
        BasicsUserEntity bUser = user.attachOrGet(BasicsAttachment.class, module).getBasicsUser().getbUEntity();
        Timestamp muted = bUser.getValue(TABLE_BASIC_USER.MUTED);
        if (muted != null && muted.getTime() < System.currentTimeMillis())
        {
            context.sendTranslated(NEUTRAL, "{user} was already muted!", user);
        }
        Duration dura = module.getConfiguration().commands.defaultMuteTime;
        if (context.hasPositional(1))
        {
            try
            {
                dura = converter.fromNode(StringNode.of(context.getString(1)), null);
            }
            catch (ConversionException e)
            {
                context.sendTranslated(NEGATIVE, "Invalid duration format!");
                return;
            }
        }
        bUser.setValue(TABLE_BASIC_USER.MUTED, new Timestamp(System.currentTimeMillis() +
            (dura.getMillis() == 0 ? TimeUnit.DAYS.toMillis(9001) : dura.getMillis())));
        bUser.asyncUpdate();
        String timeString = dura.getMillis() == 0 ? user.getTranslation(NONE, "ever") : TimeUtil.format(
            user.getLocale(), dura.getMillis());
        user.sendTranslated(NEGATIVE, "You are now muted for {input#amount}!", timeString);
        context.sendTranslated(NEUTRAL, "You muted {user} globally for {input#amount}!", user, timeString);
    }

    @Command(desc = "Unmutes a player")
    @Params(positional = @Param(label = "player", type = User.class))
    public void unmute(CommandContext context)
    {
        User user = context.get(0);
        BasicsUserEntity basicsUserEntity = user.attachOrGet(BasicsAttachment.class, module).getBasicsUser().getbUEntity();
        basicsUserEntity.setValue(TABLE_BASIC_USER.MUTED, null);
        basicsUserEntity.asyncUpdate();
        context.sendTranslated(POSITIVE, "{user} is no longer muted!", user);
    }

    @Command(alias = "roll", desc = "Shows a random number from 0 to 100")
    public void rand(CommandContext context)
    {
        this.um.broadcastTranslatedStatus(YELLOW, "rolled a {integer}!", context.getSource(), new Random().nextInt(100));
    }

    @Command(desc = "Displays the colors")
    public void chatcolors(CommandContext context)
    {
        context.sendTranslated(POSITIVE, "The following chat codes are available:");
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (ChatFormat chatFormat : ChatFormat.values())
        {
            if (i++ % 3 == 0)
            {
                builder.append("\n");
            }
            builder.append(" ").append(chatFormat.getChar()).append(" ").append(chatFormat.toString()).append(chatFormat.name()).append(ChatFormat.RESET);
        }
        context.sendMessage(builder.toString());
        context.sendTranslated(POSITIVE, "To use these type {text:&} followed by the code above");
    }
}
