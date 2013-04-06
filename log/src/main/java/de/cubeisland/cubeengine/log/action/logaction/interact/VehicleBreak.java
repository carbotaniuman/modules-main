package de.cubeisland.cubeengine.log.action.logaction.interact;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.vehicle.VehicleDestroyEvent;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.Log;
import de.cubeisland.cubeengine.log.action.logaction.SimpleLogActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionType.Category.ENTITY;
import static de.cubeisland.cubeengine.log.action.ActionType.Category.PLAYER;

/**
 * Breaking vehicles
 * <p>Events: {@link VehicleDestroyEvent}
 */
public class VehicleBreak extends SimpleLogActionType
{
    public VehicleBreak(Log module)
    {
        super(module, "vehicle-break", true, PLAYER, ENTITY);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleDestroy(final VehicleDestroyEvent event)
    {
        if (this.isActive(event.getVehicle().getWorld()))
        {
            Entity causer = null;
            if (event.getAttacker() != null)
            {
                if (event.getAttacker() instanceof Player)
                {
                    causer = event.getAttacker();
                }
                else if (event.getAttacker() instanceof Projectile)
                {
                    Projectile projectile = (Projectile) event.getAttacker();
                    if (projectile.getShooter() instanceof Player)
                    {
                        causer = projectile.getShooter();
                    }
                    else if (projectile.getShooter() != null)
                    {
                        causer = projectile.getShooter();
                    }
                }
            }
            else if (event.getVehicle().getPassenger() instanceof Player)
            {
                causer = event.getVehicle().getPassenger();
            }
            this.logSimple(event.getVehicle().getLocation(),causer,event.getVehicle(),null);
        }
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        user.sendTranslated("%s&2%s &aebroke a &6%s%s&a!",
                            time, logEntry.getCauserUser() == null ?
                            logEntry.getCauserEntity() :
                            logEntry.getCauserUser().getDisplayName(),
                            logEntry.getEntityFromData(),loc);
    }

    @Override
    public boolean isSimilar(LogEntry logEntry, LogEntry other)
    {
        return logEntry.world == other.world
            && logEntry.causer == other.causer
            && logEntry.data == other.data;
    }
}
