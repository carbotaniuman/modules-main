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
package de.cubeisland.engine.module.mail.storage;

import javax.persistence.Entity;
import javax.persistence.Table;
import org.cubeengine.service.database.AsyncRecord;
import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserManager;
import org.jooq.types.UInteger;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;

import static de.cubeisland.engine.module.mail.storage.TableMail.TABLE_MAIL;
import static org.spongepowered.api.text.format.TextColors.*;

@Entity
@Table(name = "mail")
public class Mail extends AsyncRecord<Mail>
{
    public Mail()
    {
        super(TABLE_MAIL);
    }

    public Mail newMail(User user, UInteger senderId, String message)
    {
        this.setValue(TABLE_MAIL.MESSAGE, message);
        this.setValue(TABLE_MAIL.USERID, user.getEntity().getId());
        this.setValue(TABLE_MAIL.SENDERID, senderId);
        return this;
    }

    public Text readMail(UserManager um)
    {
        UInteger value = this.getValue(TABLE_MAIL.SENDERID);
        if (value == null || value.longValue() == 0)
        {
            return Texts.of(RED, "CONSOLE", WHITE, ": ", getValue(TABLE_MAIL.MESSAGE));
        }
        User user = um.getUser(this.getValue(TABLE_MAIL.SENDERID));
        return Texts.of(DARK_GREEN, user.getDisplayName(), WHITE, ": ", getValue(TABLE_MAIL.MESSAGE));
    }
}