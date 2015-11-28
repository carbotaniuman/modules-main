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
package de.cubeisland.engine.module.mail;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.mail.storage.Mail;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.task.TaskManager;
import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserManager;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Record1;
import org.jooq.types.UInteger;
import org.spongepowered.api.text.Texts;

import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.module.mail.storage.TableMail.TABLE_MAIL;
import static org.cubeengine.service.user.TableUser.TABLE_USER;
import static org.spongepowered.api.text.format.TextColors.WHITE;

@Command(name = "mail", desc = "Manages your server mail.")
public class MailCommand extends ContainerCommand
{
    private final MailModule module;
    private final UserManager um;
    private final TaskManager taskManager;
    private final Database db;

    public MailCommand(MailModule module, UserManager um, TaskManager taskManager, Database db)
    {
        super(module);
        this.module = module;
        this.um = um;
        this.taskManager = taskManager;
        this.db = db;
    }

    @Alias(value = "readmail")
    @Command(desc = "Reads your mail.")
    public void read(CommandSender context, @Optional CommandSender player)
    {
        User sender = null;
        if (context instanceof User)
        {
            sender = (User)context;
        }
        if (sender == null)
        {
            if (player == null)
            {
                context.sendTranslated(NEUTRAL, "Log into the game to check your mailbox!");

                return;
            }
            context.sendTranslated(NEUTRAL, "If you wanted to look into other players mail use: {text:/mail spy} {input#player}.", player);
            context.sendTranslated(NEGATIVE, "Otherwise be quiet!");
            return;
        }
        MailAttachment attachment = sender.attachOrGet(MailAttachment.class, module);
        if (attachment.countMail() == 0)
        {
            context.sendTranslated(NEUTRAL, "You do not have any mail!");
            return;
        }
        List<Mail> mails;
        if (player == null) //get mails
        {
            mails = attachment.getMails();
        }
        else //Search for mail of that user
        {
            mails = attachment.getMailsFrom(player);
        }
        if (mails.isEmpty()) // Mailbox is not empty but no message from that player
        {
            context.sendTranslated(NEUTRAL, "You do not have any mail from {user}.", player);
            return;
        }
        context.sendTranslated(POSITIVE, "Your mail:");
        for (int i = 0; i < mails.size(); i++)
        {
            Mail mail = mails.get(i);
            context.sendMessage(Texts.of("\n", WHITE, i+1, ": ", mail.readMail(um)));
        }

    }

    @Alias(value = "spymail")
    @Command(desc = "Shows the mail of other players.")
    public void spy(CommandSender context, User player)
    {
        List<Mail> mails = player.attachOrGet(MailAttachment.class, module).getMails();
        if (mails.isEmpty()) // Mailbox is not empty but no message from that player
        {
            context.sendTranslated(NEUTRAL, "{user} does not have any mail!", player);
            return;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Mail mail : mails)
        {
            i++;
            sb.append("\n").append(ChatFormat.WHITE).append(i).append(": ").append(mail.getValue(TABLE_MAIL.MESSAGE));
        }
        context.sendTranslated(NEUTRAL, "{user}'s mail: {input#mails}", player, ChatFormat.parseFormats(sb.toString()));
    }

    @Alias(value = "sendmail")
    @Command(desc = "Sends mails to other players.")
    public void send(CommandSender context, User player, @Greed(INFINITE) String message)
    {
        this.mail(message, context, player);
        context.sendTranslated(POSITIVE, "Mail send to {user}!", player);
    }

    @Alias(value = "sendallmail")
    @Command(desc = "Sends mails to all players.")
    public void sendAll(CommandSender context, final @Greed(INFINITE) String message)
    {
        Set<User> users = um.getOnlineUsers();
        final Set<UInteger> alreadySend = new HashSet<>();
        User sender = null;
        if (context instanceof User)
        {
            sender = (User)context;
        }
        for (User user : users)
        {
            user.attachOrGet(MailAttachment.class, module).addMail(sender, message);
            alreadySend.add(user.getEntity().getId());
        }
        final UInteger senderId = sender == null ? null : sender.getEntity().getId();
        taskManager.runAsynchronousTaskDelayed(this.module, () -> {
            DSLContext dsl = db.getDSL();

            Collection<Query> queries = dsl.select(TABLE_USER.KEY).from(TABLE_USER).where(TABLE_USER.KEY.notIn(
                alreadySend)).fetch()
                       .map(Record1::value1).stream()
                       .map(userKey -> dsl.insertInto(TABLE_MAIL, TABLE_MAIL.MESSAGE, TABLE_MAIL.USERID, TABLE_MAIL.SENDERID)
                                          .values(message, userKey, senderId))
                       .collect(Collectors.toList());
            dsl.batch(queries).execute();
        }, 0);
        context.sendTranslated(POSITIVE, "Sent mail to everyone!");
    }

    @Command(desc = "Removes a single mail")
    @Restricted(value = User.class, msg = "The console has no mails!")
    public void remove(User context, Integer mailId)
    {
        MailAttachment attachment = context.attachOrGet(MailAttachment.class, module);
        if (attachment.countMail() == 0)
        {
            context.sendTranslated(NEUTRAL, "You do not have any mail!");
            return;
        }
        try
        {
            Mail mail = attachment.getMails().get(mailId);
            db.getDSL().delete(TABLE_MAIL).where(TABLE_MAIL.KEY.eq(mail.getValue(
                TABLE_MAIL.KEY))).execute();
            context.sendTranslated(POSITIVE, "Deleted Mail #{integer#mailid}", mailId);
        }
        catch (IndexOutOfBoundsException e)
        {
            context.sendTranslated(NEGATIVE, "Invalid Mail Id!");
        }
    }

    @Command(desc = "Clears your mail.")
    @Restricted(value = User.class, msg = "You will never have mail here!")
    public void clear(User context, @Optional CommandSender player)
    {
        if (player == null)
        {
            context.attachOrGet(MailAttachment.class, module).clearMail();
            context.sendTranslated(NEUTRAL, "Cleared all mails!");
            return;
        }
        context.attachOrGet(MailAttachment.class, module).clearMailFrom(player);
        context.sendTranslated(NEUTRAL, "Cleared all mail from {user}!", player instanceof User ? player : "console");
    }

    private void mail(String message, CommandSender from, User... users)
    {
        for (User user : users)
        {
            user.attachOrGet(MailAttachment.class, module).addMail(from, message);
            if (user.asPlayer().isOnline())
            {
                user.sendTranslated(NEUTRAL, "You just got a mail from {user}!", from.getName());
            }
        }
    }
}