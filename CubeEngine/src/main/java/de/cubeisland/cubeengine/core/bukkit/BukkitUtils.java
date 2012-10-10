package de.cubeisland.cubeengine.core.bukkit;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.LocaleLanguage;
import net.minecraft.server.NetServerHandler;
import net.minecraft.server.ServerConnection;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;

/**
 * This class contains various methods to access bukkit-related stuff.
 */
public class BukkitUtils
{
    private static final Field localeStringField;
    private static Field nshListField = null;

    static
    {
        try
        {
            localeStringField = LocaleLanguage.class.getDeclaredField("d");
            localeStringField.setAccessible(true);

        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to initialize the Bukkit-Language-Hack!");
        }
        try
        {
            nshListField = ServerConnection.class.getDeclaredField("d");
            nshListField.setAccessible(true);
        }
        catch (Exception e)
        {}
    }

    private BukkitUtils()
    {}

    /**
     * Returns the locale string of a player.
     *
     * @param player the Player instance
     * @return the locale string of the player
     */
    public static String getLanguage(Player player)
    {
        if (player.getClass() == CraftPlayer.class)
        {
            try
            {
                return (String)localeStringField.get(((CraftPlayer)player).getHandle().getLocale());
            }
            catch (Exception e)
            {}
        }
        return null;
    }

    /**
     * Gets the CommandMap from the PluginManager
     *
     * @param pluginManager the PluginManager
     * @return the CommandMap instance
     */
    public static CommandMap getCommandMap(PluginManager pluginManager)
    {
        if (pluginManager.getClass() == SimplePluginManager.class)
        {
            try
            {
                for (Field field : pluginManager.getClass().getDeclaredFields())
                {
                    if (CommandMap.class.isAssignableFrom(field.getType()))
                    {
                        field.setAccessible(true);
                        return (CommandMap)field.get(pluginManager);
                    }
                }
            }
            catch (Exception e)
            {}
        }
        return null;
    }

    /**
     * Gets the known command map from a CommandMap instance
     *
     * @param commandMap the CommandMap instance
     * @return the known command map
     */
    public static Map<String, Command> getKnownCommandMap(CommandMap commandMap)
    {
        if (commandMap.getClass() == SimpleCommandMap.class)
        {
            try
            {
                for (Field field : commandMap.getClass().getDeclaredFields())
                {
                    if (Map.class.isAssignableFrom(field.getType()))
                    {
                        field.setAccessible(true);
                        return (Map<String, Command>)field.get(commandMap);
                    }
                }
            }
            catch (Exception e)
            {}
        }
        return null;
    }

    /**
     * Registers the packet hook injector
     *
     * @param plugin        a Plugin to register the injector with
     * @param pluginManager a PluginManager
     */
    public static void registerPacketHookInjector(Plugin plugin, PluginManager pluginManager)
    {
        if (!PacketHookInjector.injected)
        {
            pluginManager.registerEvents(PacketHookInjector.INSTANCE, plugin);
        }
    }

    private static class PacketHookInjector implements Listener
    {
        public static final PacketHookInjector INSTANCE = new PacketHookInjector();
        public static boolean injected = false;

        private PacketHookInjector()
        {}

        /**
         * The event listener swaps the joining player's NetServerHandler
         * instance with a custom one including all the magic to make the new
         * NetServerHandler work.
         *
         * @param event
         */
        @EventHandler(priority = EventPriority.LOW)
        public void onPlayerJoin(PlayerJoinEvent event)
        {
            if (nshListField == null)
            {
                return;
            }
            final Player player = event.getPlayer();
            EntityPlayer playerEntity = ((CraftPlayer)player).getHandle();
            NetServerHandler oldHandler = playerEntity.netServerHandler;
            try
            {

                if (oldHandler.getClass() != CubeEngineNetServerHandler.class)
                {
                    CubeEngineNetServerHandler handler = new CubeEngineNetServerHandler(playerEntity);

                    Location loc = player.getLocation();
                    handler.a(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

                    ServerConnection sc = playerEntity.server.ac();
                    ((List<NetServerHandler>)nshListField.get(sc)).remove(oldHandler);
                    sc.a(handler);
                    System.out.print("Replaced the NetServerHandler of player '" + player.getName() + "'"); // TODO log this as debug or smt like this
                    oldHandler.disconnected = true;
                }
            }
            catch (Exception e)
            {
                playerEntity.netServerHandler = oldHandler;
                e.printStackTrace(System.err);
            }
        }
    }
}