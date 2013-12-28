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
package de.cubeisland.engine.core.module.service.selector;

import org.bukkit.Location;
import org.bukkit.event.Listener;

import de.cubeisland.engine.core.CubeEngine;
import de.cubeisland.engine.core.module.service.Selector;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.math.shape.Shape;
// TODO make a module out of it (integrate WE if found)
public class CuboidSelector implements Selector, Listener
{
    @Override
    public Shape getSelection(User user)
    {
        SelectorAttachment attachment = user.attachOrGet(SelectorAttachment.class, CubeEngine.getCore().getModuleManager().getCoreModule());
        return attachment.getSelection();
    }

    @Override
    public Shape get2DProjection(User user)
    {

       return null; // TODO Shape.projectOnto(Plane)
    }

    @Override
    public <T extends Shape> T getSelection(User user, Class<T> shape)
    {
        return null; // TODO
    }

    @Override
    public Location getFirstPoint(User user)
    {
        return this.getPoint(user, 1);
    }

    @Override
    public Location getSecondPoint(User user)
    {
        return this.getPoint(user, 2);
    }

    @Override
    public Location getPoint(User user, int index)
    {
        SelectorAttachment attachment = user.attachOrGet(SelectorAttachment.class, CubeEngine.getCore().getModuleManager().getCoreModule());
        return attachment.getPoint(index);
    }
}
