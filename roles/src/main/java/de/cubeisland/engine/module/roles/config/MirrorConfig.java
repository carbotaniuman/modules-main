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
package de.cubeisland.engine.module.roles.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.module.core.util.Triplet;
import de.cubeisland.engine.module.service.world.ConfigWorld;
import de.cubeisland.engine.module.service.world.WorldManager;
import org.spongepowered.api.world.World;

public class MirrorConfig
{
    public final ConfigWorld mainWorld;
    private Log logger;
    //mirror roles / assigned / users
    protected final Map<ConfigWorld, Triplet<Boolean, Boolean, Boolean>> mirrors = new HashMap<>();
    private WorldManager wm;

    public MirrorConfig(WorldManager wm, World world, Log logger)
    {
        this(wm, new ConfigWorld(wm, world), logger);
    }

    protected MirrorConfig(WorldManager wm, ConfigWorld mainWorld, Log logger)
    {
        this.wm = wm;
        this.mainWorld = mainWorld;
        this.logger = logger;
        this.mirrors.put(mainWorld, new Triplet<>(true, true, true));
    }

    /**
     * Returns a map of the mirrored worlds.
     * The mirrors are: roles | assigned roles | assigned permissions and metadata
     *
     * @return
     */
    public Map<World, Triplet<Boolean, Boolean, Boolean>> getWorldMirrors()
    {
        HashMap<World, Triplet<Boolean, Boolean, Boolean>> result = new HashMap<>();
        for (Entry<ConfigWorld, Triplet<Boolean, Boolean, Boolean>> entry : this.mirrors.entrySet())
        {
            World world = entry.getKey().getWorld();
            if (world == null)
            {
                logger.warn("Configured world for mirror of {} does not exist! {}", mainWorld.getName(),
                            entry.getKey().getName());
                continue;
            }
            result.put(world, entry.getValue());
        }
        return result;
    }

    public void setWorld(World world, boolean roles, boolean assigned, boolean users)
    {
        this.setWorld(new ConfigWorld(wm, world), new Triplet<>(roles, assigned, users));
    }

    protected void setWorld(ConfigWorld world, Triplet<Boolean, Boolean, Boolean> t)
    {
        this.mirrors.put(world, t);
    }

    public World getMainWorld()
    {
        if (this.mainWorld.getWorld() == null)
        {
            logger.warn("Configured main world for mirror does not exist! {}", this.mainWorld.getName());
            return null;
        }
        return this.mainWorld.getWorld();
    }
}
