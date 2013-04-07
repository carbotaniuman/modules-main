package de.cubeisland.cubeengine.log.action.logaction;

import java.util.EnumSet;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.storage.ItemData;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionType.Category.ITEM;
import static de.cubeisland.cubeengine.log.action.ActionType.Category.PLAYER;

/**
 * crafting items
 * <p>Events: {@link CraftItemEvent}</p>
 */
public class CraftItem extends SimpleLogActionType
{
    @Override
    protected EnumSet<Category> getCategories()
    {
        return EnumSet.of(PLAYER,ITEM);
    }

    @Override
    public boolean canRollback()
    {
        return false;
    }

    @Override
    public String getName()
    {
        return "craft-item";
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event)
    {
        if (this.isActive(event.getWhoClicked().getWorld()))
        {
            ItemData itemData = new ItemData(event.getRecipe().getResult());
            this.logSimple(event.getWhoClicked(),itemData.serialize(this.om));
        }
    }
    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        user.sendTranslated("%s&2%s&a crafted &6%s%s&a!",
                            time,logEntry.getCauserUser().getDisplayName(),
                            logEntry.getItemData(),loc);
    }

    @Override
    public boolean isSimilar(LogEntry logEntry, LogEntry other)
    {
        return logEntry.causer == other.causer
            && logEntry.world == other.world
            && logEntry.getItemData().equals(other.getItemData()); // ignoring amount
    }

    @Override
    public boolean isActive(World world)
    {
        return this.lm.getConfig(world).CRAFT_ITEM_enable;
    }
}