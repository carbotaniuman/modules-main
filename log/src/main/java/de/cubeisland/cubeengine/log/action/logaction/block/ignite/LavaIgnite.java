package de.cubeisland.cubeengine.log.action.logaction.block.ignite;

import java.util.EnumSet;

import org.bukkit.World;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.action.logaction.block.BlockActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionType.Category.BLOCK;
import static de.cubeisland.cubeengine.log.action.ActionType.Category.ENVIRONEMENT;

/**
 * lava-ignite
 * <p>Events: {@link IgniteActionType}</p>
 */
public class LavaIgnite extends BlockActionType
{
    @Override
    protected EnumSet<Category> getCategories()
    {
        return EnumSet.of(BLOCK, ENVIRONEMENT);
    }

    @Override
    public String getName()
    {
        return "lava-ignite";
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        user.sendTranslated("%s&aFire got set by lava%s&a!",time,loc);
    }


    @Override
    public boolean isActive(World world)
    {
        return this.lm.getConfig(world).LAVA_IGNITE_enable;
    }
}