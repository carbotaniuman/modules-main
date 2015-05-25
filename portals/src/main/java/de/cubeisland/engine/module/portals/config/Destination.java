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
package de.cubeisland.engine.module.portals.config;

import java.util.Random;
import com.flowpowered.math.vector.Vector3d;
import de.cubeisland.engine.butler.CommandInvocation;
import de.cubeisland.engine.butler.parameter.reader.ArgumentReader;
import de.cubeisland.engine.butler.parameter.reader.ReaderException;
import de.cubeisland.engine.module.core.CubeEngine;
import de.cubeisland.engine.module.core.sponge.BukkitUtils;
import de.cubeisland.engine.module.core.util.formatter.MessageType;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.core.util.WorldLocation;
import de.cubeisland.engine.module.service.world.ConfigWorld;
import de.cubeisland.engine.module.portals.Portal;
import de.cubeisland.engine.module.portals.PortalManager;
import de.cubeisland.engine.module.portals.Portals;
import de.cubeisland.engine.module.service.world.WorldManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import de.cubeisland.engine.module.core.util.formatter.MessageType.NEGATIVE;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEGATIVE;

public class Destination
{
    public Type type;
    public ConfigWorld world;
    public WorldLocation location;
    public String portal;

    public Destination(WorldManager wm, Location location, Vector3d direction)
    {
        this.location = new WorldLocation(location, direction);
        this.world = new ConfigWorld(wm, (World)location.getExtent());
        this.type = Type.LOCATION;
    }

    public Destination(WorldManager wm, World world)
    {
        this.world = new ConfigWorld(wm, world);
        this.type = Type.WORLD;
    }

    public Destination(Portal portal)
    {
        this.portal = portal.getName();
        this.type = Type.PORTAL;
    }

    protected Destination()
    {}

    public void teleport(final Entity entity, PortalManager manager, boolean safe)
    {
        Location loc = null;
        switch (type)
        {
        case PORTAL:
            Portal destPortal = manager.getPortal(portal);
            if (destPortal == null)
            {
                if (entity instanceof User)
                {
                    ((User)entity).sendTranslated(NEGATIVE, "Destination portal {input} does not exist!", portal);
                }
                return;
            }
            loc = destPortal.getPortalPos();
            break;
        case WORLD:
            loc = world.getWorld().getSpawnLocation();
            loc.setX(loc.getBlockX() + 0.5);
            loc.setZ(loc.getBlockZ() + 0.5);
            break;
        case LOCATION:
            loc = location.getLocationIn(world.getWorld()); // TODO rotation
            break;
        }
        if (entity.isInsideVehicle())
        {
            // instead tp vehicle + passenger (triggered by vehicle)
            return;
        }
        if (safe && entity instanceof User)
        {
            ((User)entity).safeTeleport(loc, TeleportCause.PLUGIN, false);
        }
        else
        {
            if (entity.getLocation().getWorld() == loc.getWorld() || entity instanceof User)
            {
                // Same world: No Problem
                entity.teleport(loc, TeleportCause.PLUGIN);
            }
            else if (entity instanceof CraftEntity)
            {
                // Different world: use NMS stuff
                BukkitUtils.teleport(manager.module, ((CraftEntity)entity).getHandle(), loc);
            }
            else
            {
                manager.module.getLog().warn("Could not teleport entity: {}", entity);
            }
        }
    }

    public enum Type
    {
        PORTAL, WORLD, LOCATION, RANDOM
    }

    public static class DestinationReader implements ArgumentReader<Destination>
    {
        private final Portals module;
        private final Random random = new Random();

        public DestinationReader(Portals module)
        {
            this.module = module;
        }

        @Override
        public Destination read(Class type, CommandInvocation invocation) throws ReaderException
        {
            String token = invocation.consume(1);
            if ("here".equalsIgnoreCase(token))
            {
                if ((invocation.getCommandSource() instanceof User))
                {
                    return new Destination(((User)invocation.getCommandSource()).getLocation());
                }
                throw new ReaderException("The Portal Agency will bring you your portal for just {text:$ 1337} within {input#amount} weeks", String.valueOf(random.nextInt(51)+1));
            }
            else if (token.startsWith("p:")) // portal dest
            {
                Portal destPortal = module.getPortalManager().getPortal(token.substring(2));
                if (destPortal == null)
                {
                    throw new ReaderException("Portal {input} not found!", token.substring(2));
                }
                return new Destination(destPortal);
            }
            else // world
            {
                World world = this.module.getCore().getWorldManager().getWorld(token);
                if (world == null)
                {
                    throw new ReaderException("World {input} not found!", token);
                }
                return new Destination(world);
            }
        }
    }

    // TODO completer for destination
}
