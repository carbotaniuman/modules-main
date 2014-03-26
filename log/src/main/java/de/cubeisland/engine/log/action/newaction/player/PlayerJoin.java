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
package de.cubeisland.engine.log.action.newaction.player;

import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.log.action.newaction.ActionTypeBase;

import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;

/**
 * Represents a Player joining the server
 */
public class PlayerJoin extends PlayerActionType<PlayerActionListener>
{
    // return "player-join";
    // return this.lm.getConfig(world).PLAYER_JOIN_enable;

    public String ip;

    @Override
    public boolean canAttach(ActionTypeBase action)
    {
        return action instanceof PlayerJoin
            && ((PlayerJoin)action).playerUUID.equals(this.playerUUID);
    }

    @Override
    public String translateAction(User user)
    {
        if (this.hasAttached())
        {
            return user.getTranslation(POSITIVE, "{user} joined the server x{amount}",
                                this.playerName, this.getAttached().size() + 1);
        }
        return user.getTranslation(POSITIVE, "{user} joined the server", this.playerName);
        // TODO ip if set
    }

    public void setIp(String ip)
    {
        this.ip = ip;
    }
}
