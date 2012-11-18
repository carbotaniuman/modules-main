package de.cubeisland.cubeengine.basics.teleport;

import de.cubeisland.cubeengine.basics.Basics;
import de.cubeisland.cubeengine.basics.BasicsPerm;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.core.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportListener implements Listener
{
    private Basics basics;

    public TeleportListener(Basics basics)
    {
        this.basics = basics;
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event)
    {
        User user = basics.getUserManager().getExactUser(event.getPlayer());
        switch (event.getCause()) {
            case COMMAND:
            case PLUGIN:
            case UNKNOWN:
                user.setAttribute(basics, "lastLocation", event.getFrom());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event)
    {
        User user = this.basics.getUserManager().getExactUser(event.getEntity());
        if (BasicsPerm.COMMAND_BACK_ONDEATH.isAuthorized(user))
        {
            user.setAttribute(basics, "lastLocation", user.getLocation());
        }
    }
    
    @EventHandler
    public void onClick(PlayerInteractEvent event)
    {
        if (event.getPlayer().getItemInHand().getType().equals(Material.COMPASS))
        {
            switch (event.getAction())
            {
                case LEFT_CLICK_AIR:
                case LEFT_CLICK_BLOCK:
                    if (BasicsPerm.COMPASS_JUMPTO_LEFT.isAuthorized(event.getPlayer()))
                    {
                        Block block = event.getPlayer().getTargetBlock(null, 300);
                        if (block.getTypeId() != 0)
                        {
                            User user = this.basics.getUserManager().getExactUser(event.getPlayer());
                            user.safeTeleport(block.getLocation().add(0.5, 1, 0.5));
                            user.sendMessage("basics", "&ePoof!");
                        }
                    }
                    return;
                    //TODO tp onto block
                case RIGHT_CLICK_AIR:
                case RIGHT_CLICK_BLOCK:
                    if (BasicsPerm.COMPASS_JUMPTO_RIGHT.isAuthorized(event.getPlayer()))
                    {
                        User user = this.basics.getUserManager().getExactUser(event.getPlayer());
                        Location loc = LocationUtil.getBlockBehindWall(user);
                        if (loc == null)
                        {
                            //TODO msg
                            return;
                        }
                        user.safeTeleport(loc);
                        user.sendMessage("basics", "&ePassed the wall");
                    }
            }
        }
    }
}
