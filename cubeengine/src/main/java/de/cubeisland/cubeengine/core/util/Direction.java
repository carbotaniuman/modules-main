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
package de.cubeisland.cubeengine.core.util;

public enum Direction
{
    North(23),
    NorthEast(68),
    East(113),
    SouthEast(158),
    South(203),
    SouthWest(248),
    West(293),
    NorthWest(338);

    private final int dir;

    private Direction(int dir)
    {
        this.dir = dir;
    }

    public static Direction matchDirection(int dir)
    {
        for (Direction direction : values())
        {
            if (dir < direction.dir)
            {
                return direction;
            }
        }
        return Direction.North;
    }
}
