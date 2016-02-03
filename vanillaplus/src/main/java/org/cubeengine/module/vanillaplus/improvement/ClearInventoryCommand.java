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
package org.cubeengine.module.vanillaplus.improvement;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.service.command.annotation.ParameterPermission;
import org.cubeengine.service.command.exception.PermissionDeniedException;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.permission.PermissionContainer;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.permission.PermissionDescription;

import static org.cubeengine.service.i18n.formatter.MessageType.*;

public class ClearInventoryCommand extends PermissionContainer<VanillaPlus>
{
    private final PermissionDescription COMMAND_CLEARINVENTORY = register("command.clearinventory", "", null);
    public final PermissionDescription COMMAND_CLEARINVENTORY_OTHER = register("notify",
                                                                               "Allows clearing the inventory of other players",
                                                                               COMMAND_CLEARINVENTORY);
    public final PermissionDescription COMMAND_CLEARINVENTORY_NOTIFY = register("other",
                                                                                "Notifies you if your inventory got cleared by someone else",
                                                                                COMMAND_CLEARINVENTORY);
    public final PermissionDescription COMMAND_CLEARINVENTORY_PREVENT = register("prevent",
                                                                                 "Prevents your inventory from being cleared unless forced",
                                                                                 COMMAND_CLEARINVENTORY);
    public final PermissionDescription COMMAND_CLEARINVENTORY_FORCE = register("force",
                                                                               "Clears an inventory even if the player has the prevent PermissionDescription",
                                                                               COMMAND_CLEARINVENTORY);

    private I18n i18n;


    public ClearInventoryCommand(VanillaPlus module, I18n i18n)
    {
        super(module);
        this.i18n = i18n;
    }


    @Command(alias = {"ci", "clear"}, desc = "Clears the inventory")
    public void clearinventory(CommandSource context, @Default User player,
                               @Flag(longName = "removeArmor", name = "ra") boolean removeArmor,
                               @ParameterPermission(value = "quiet", desc = "Prevents the other player being notified when his inventory got cleared")
                                    @Flag boolean quiet,
                               @Flag boolean force)
    {
        //sender.sendTranslated(NEGATIVE, "That awkward moment when you realize you do not have an inventory!");
        boolean self = context.getIdentifier().equals(player.getIdentifier());
        if (!self)
        {
            if (!context.hasPermission(COMMAND_CLEARINVENTORY_OTHER.getId()))
            {
                throw new PermissionDeniedException(COMMAND_CLEARINVENTORY_OTHER);
            }
            if (player.hasPermission(COMMAND_CLEARINVENTORY_PREVENT.getId())
                && !(force && context.hasPermission(COMMAND_CLEARINVENTORY_FORCE.getId())))
            {
                i18n.sendTranslated(context, NEGATIVE, "You are not allowed to clear the inventory of {user}", player);
                return;
            }
        }
        player.getInventory().clear(); // TODO will this clear armor too
        if (removeArmor)
        {
            // TODO is this needed?
            player.setBoots(null);
            player.setLeggings(null);
            player.setChestplate(null);
            player.setHelmet(null);
        }
        if (self)
        {
            i18n.sendTranslated(context, POSITIVE, "Your inventory has been cleared!");
            return;
        }
        if (player.isOnline() && player.hasPermission(COMMAND_CLEARINVENTORY_NOTIFY.getId()) && !quiet)
        {
            i18n.sendTranslated(player.getPlayer().get(), NEUTRAL, "Your inventory has been cleared by {sender}!", context);
        }
        i18n.sendTranslated(context, POSITIVE, "The inventory of {user} has been cleared!", player);
    }
}