package de.cubeisland.cubeengine.log.action.logaction.spawn;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.Log;
import de.cubeisland.cubeengine.log.action.logaction.SimpleLogActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionType.Category.ENTITY;

/**
 * spawner spawning
 * <p>Events: {@link EntitySpawnActionType}</p>
 */
public class SpawnerSpawn extends SimpleLogActionType

{
    public SpawnerSpawn(Log module)
    {
        super(module, "spawner-spawn", true, ENTITY);
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        user.sendTranslated("%s&6%s &aspawned from a spawner%s!",
                            time,logEntry.getCauserEntity(),loc);
    }
    @Override
    public boolean isSimilar(LogEntry logEntry, LogEntry other)
    {
        return logEntry.causer == other.causer
            && logEntry.world == other.world
            && logEntry.location.equals(other.location);
    }
}
