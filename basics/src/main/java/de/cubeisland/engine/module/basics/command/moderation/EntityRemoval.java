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
package de.cubeisland.engine.module.basics.command.moderation;

import de.cubeisland.engine.module.service.permission.Permission;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.service.permission.Subject;

public class EntityRemoval
{
    public final Class<?>[] interfaces;
    private final Permission perm;

    EntityRemoval(Permission perm, Class<?>... interfaces)
    {
        this.perm = perm;
        this.interfaces = interfaces;
    }

    public boolean doesMatch(Entity entity)
    {
        if (interfaces.length == 0)
        {
            return this.extra(entity);
        }
        for (Class<?> anInterface : interfaces)
        {
            if (anInterface.isAssignableFrom(entity.getClass()))
            {
                return this.extra(entity);
            }
        }
        return false;
    }

    public boolean isAllowed(Subject permissible)
    {
        return this.perm.isAuthorized(permissible);
    }

    /**
     * Override this to check extra information
     */
    public boolean extra(Entity entity)
    {
        return true;
    }
}
