package de.cubeisland.cubeengine.log.action.logaction.block;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockSpreadEvent;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.Log;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionType.Category.BLOCK;
import static de.cubeisland.cubeengine.log.action.ActionType.Category.ENVIRONEMENT;

/**
 * Blocks except fire spreading
 * <p>Events: {@link BlockSpreadEvent}</p>
 */
public class BlockSpread extends BlockActionType
{
    public BlockSpread(Log module)
    {
        super(module, "block-spread", BLOCK, ENVIRONEMENT);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event)
    {
        if (!event.getNewState().getType().equals(Material.FIRE))
        {
            if (this.isActive(event.getBlock().getWorld()))
            {
                this.logBlockChange(null,
                                    event.getBlock().getState(),
                                    event.getNewState(),null);
            }
        }
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        user.sendTranslated("%s&6%s&a spreaded%s&a!",
                            logEntry.getNewBlock(),time,loc);
    }
}
