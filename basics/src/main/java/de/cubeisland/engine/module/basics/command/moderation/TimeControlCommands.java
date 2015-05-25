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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.module.core.util.formatter.MessageType;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.task.TaskManager;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.core.util.matcher.Match;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.service.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.spongepowered.api.world.World;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEUTRAL;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;

/**
 * Commands changing time. /time /ptime
 */
public class TimeControlCommands
{
    private final Basics module;
    private final TaskManager taskmgr;
    private WorldManager wm;
    private final LockTask lockTask;

    public TimeControlCommands(Basics module, TaskManager taskmgr, WorldManager wm)
    {
        this.module = module;
        this.taskmgr = taskmgr;
        this.wm = wm;
        this.lockTask = new LockTask();
    }

    @Command(desc = "Changes the time of a world")
    public void time(CommandSender context, @Optional String time,
                     @Named({ "w", "worlds", "in"}) String worlds, // TODO worldlist reader // TODO NParams static label reader
                     @Flag boolean lock)
    {
        Collection<World> worldList;
        if (worlds != null)
        {
            if ("*".equals(worlds))
            {
                worldList = wm.getWorlds();
            }
            else
            {
                worldList = worldMatcher.matchWorlds(worlds);
                for (World world : worldList)
                {
                    if (world == null)
                    {
                        context.sendTranslated(NEGATIVE, "Could not match all worlds! {input#worlds}", worlds);
                        return;
                    }
                }
            }
        }
        else
        {
            if (!(context instanceof User))
            {
                context.sendTranslated(NEGATIVE, "You have to specify a world when using this command from the console!");
                return;
            }
            worldList = Collections.singletonList(((User)context).getWorld());
        }
        if (time != null)
        {
            final Long lTime = timeMatcher.matchTimeValue(time);
            if (lTime == null)
            {
                context.sendTranslated(NEGATIVE, "The time you entered is not valid!");
                return;
            }
            if (worldList.size() == 1)
            {
                context.sendTranslated(POSITIVE, "The time of {world} have been set to {input#time} ({input#neartime})!", worldList.get(0), timeMatcher.format(
                    lTime), timeMatcher.getNearTimeName(lTime));
            }
            else if ("*".equals(worlds))
            {
                context.sendTranslated(POSITIVE, "The time of all worlds have been set to {input#time} ({input#neartime})!", timeMatcher.format(lTime), timeMatcher.getNearTimeName(
                    lTime));            }
            else
            {
                context.sendTranslated(POSITIVE, "The time of {amount} worlds have been set to {input#time} ({input#neartime})!", worldList.size(), timeMatcher.format(
                    lTime), timeMatcher.getNearTimeName(lTime));
            }
            for (World world : worldList)
            {
                this.setTime(world, lTime);
                if (lock)
                {
                    if (this.lockTask.worlds.containsKey(world.getName()))
                    {
                        this.lockTask.remove(world);
                        context.sendTranslated(POSITIVE, "Time unlocked for {world}!", world);
                    }
                    else
                    {
                        this.lockTask.add(world);
                        context.sendTranslated(POSITIVE, "Time locked for {world}!", world);
                    }
                }
            }
            return;
        }
        if (lock)
        {
            for (World world : worldList)
            {
                if (this.lockTask.worlds.containsKey(world.getName()))
                {
                    this.lockTask.remove(world);
                    context.sendTranslated(POSITIVE, "Time unlocked for {world}!", world);
                }
                else
                {
                    this.lockTask.add(world);
                    context.sendTranslated(POSITIVE, "Time locked for {world}!", world);
                }
            }
            return;
        }
        context.sendTranslated(POSITIVE, "The current time is:");
        for (World world : worldList)
        {
            context.sendTranslated(NEUTRAL, "{input#time} ({input#neartime}) in {world}.", timeMatcher.format(
                world.getTime()), timeMatcher.getNearTimeName(world.getTime()), world);
        }
    }

    @Command(desc = "Changes the time for a player")
    public void ptime(CommandSender context, String time, @Default User player, @Flag boolean lock) // TODO staticValues = "reset"
    {
        Long lTime = 0L;
        boolean reset = false;
        if ("reset".equalsIgnoreCase(time))
        {
            reset = true;
        }
        else
        {
            lTime = timeMatcher.matchTimeValue(time);
            if (lTime == null)
            {
                context.sendTranslated(NEGATIVE, "Invalid time format!");
                return;
            }
        }

        if (!context.equals(player) && !module.perms().COMMAND_PTIME_OTHER.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to change the time of other players!");
            return;
        }
        if (reset)
        {
            player.resetPlayerTime();
            context.sendTranslated(POSITIVE, "Reseted the time for {user}!", player);
            if (context.equals(player))
            {
                player.sendTranslated(NEUTRAL, "Your time was reset!");
            }
            return;
        }
        String format = timeMatcher.format(lTime);
        String nearTime = timeMatcher.getNearTimeName(lTime);
        if (lock)
        {
            player.resetPlayerTime();
            player.setPlayerTime(lTime, false);
            context.sendTranslated(POSITIVE, "Time locked to {input#time} ({input#neartime}) for {user}!", format, nearTime, player);
        }
        else
        {
            player.resetPlayerTime();
            player.setPlayerTime(lTime - player.getWorld().getTime(), true);
            context.sendTranslated(POSITIVE, "Time set to {input#time} ({input#neartime}) for {user}!", format, nearTime, player);
        }
        if (context.equals(player))
        {
            context.sendTranslated(POSITIVE, "Your time was set to {input#time} ({input#neartime})!", format, nearTime);
        }
    }

    private void setTime(World world, long time)
    {
        world.setTime(time);
    }

    private final class LockTask implements Runnable
    {

        private final Map<String, Long> worlds = new HashMap<>();
        private UUID taskid = null;

        public void add(World world)
        {
            this.worlds.put(world.getName(), world.getTime());
            if (this.taskid == null)
            {
                this.taskid = taskmgr.runTimer(module, this, 10, 10).get();
            }
        }

        public void remove(World world)
        {
            this.worlds.remove(world.getName());
            if (this.taskid != null && this.worlds.isEmpty())
            {
                taskmgr.cancelTask(module, this.taskid);
                this.taskid = null;
            }
        }

        @Override
        public void run()
        {
            Iterator<Map.Entry<String, Long>> iter = this.worlds.entrySet().iterator();

            Map.Entry<String, Long> entry;
            World world;
            while (iter.hasNext())
            {
                entry = iter.next();
                world = Bukkit.getWorld(entry.getKey());
                if (world != null)
                {
                    world.setTime(entry.getValue());
                }
                else
                {
                    iter.remove();
                }
            }
            if (this.taskid != null && this.worlds.isEmpty())
            {
                taskmgr.cancelTask(module, this.taskid);
                this.taskid = null;
            }
        }
    }
}
