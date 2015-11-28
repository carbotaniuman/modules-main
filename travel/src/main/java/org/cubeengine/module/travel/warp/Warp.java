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
package org.cubeengine.module.travel.warp;

import org.cubeengine.module.travel.TeleportPoint;
import org.cubeengine.module.travel.Travel;
import org.cubeengine.module.travel.storage.TableTeleportPoint;
import org.cubeengine.module.travel.storage.TeleportPointModel;
import org.cubeengine.module.travel.storage.TeleportPointModel.Visibility;
import org.cubeengine.service.permission.PermissionManager;
import org.cubeengine.service.user.UserManager;
import org.cubeengine.service.world.WorldManager;
import org.spongepowered.api.service.permission.PermissionDescription;

public class Warp extends TeleportPoint
{
    private PermissionManager pm;

    public Warp(TeleportPointModel teleportPoint, Travel module, PermissionManager pm, WorldManager wm, UserManager um)
    {
        super(teleportPoint, module, wm, um);
        this.pm = pm;
        if (teleportPoint.getValue(TableTeleportPoint.TABLE_TP_POINT.VISIBILITY) == Visibility.PUBLIC.value)
        {
            this.permission = generatePublicPerm();
            return;
        }
        this.permission = null;
    }

    public void setVisibility(Visibility visibility)
    {
        super.setVisibility(visibility);
        model.updateAsync();
        if (visibility == Visibility.PUBLIC)
        {
            this.permission = generatePublicPerm();
            this.iManager.removeInvites(this);
            return;
        }
        this.permission = null;
    }

    @Override
    protected PermissionDescription generatePublicPerm()
    {
        return pm.register(module, "warps.access." + getName(), "", null);
    }
}