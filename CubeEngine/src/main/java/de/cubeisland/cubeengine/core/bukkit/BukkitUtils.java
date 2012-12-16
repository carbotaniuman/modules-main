package de.cubeisland.cubeengine.core.bukkit;

import de.cubeisland.cubeengine.core.CubeEngine;
import de.cubeisland.cubeengine.core.command.CubeCommand;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.core.util.ChatFormat;
import de.cubeisland.cubeengine.core.util.worker.AsyncTaskQueue;
import de.cubeisland.cubeengine.core.util.worker.TaskQueue;
import net.minecraft.server.v1_4_5.EntityPlayer;
import net.minecraft.server.v1_4_5.LocaleLanguage;
import net.minecraft.server.v1_4_5.NBTTagCompound;
import net.minecraft.server.v1_4_5.NBTTagList;
import net.minecraft.server.v1_4_5.NBTTagString;
import net.minecraft.server.v1_4_5.NetServerHandler;
import net.minecraft.server.v1_4_5.ServerConnection;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.craftbukkit.v1_4_5.CraftServer;
import org.bukkit.craftbukkit.v1_4_5.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_4_5.help.SimpleHelpMap;
import org.bukkit.craftbukkit.v1_4_5.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Filter;
import java.util.logging.Logger;

import static de.cubeisland.cubeengine.core.util.log.LogLevel.DEBUG;

/**
 * This class contains various methods to access bukkit-related stuff.
 */
public class BukkitUtils
{
    private static boolean hackSucceeded = false;
    private static final Field LOCALE_STRING_FIELD = findFirstField(String.class, LocaleLanguage.class);
    private static final Field NSH_LIST_FIELD = findFirstField(List.class, ServerConnection.class);

    static
    {
        if (LOCALE_STRING_FIELD != null && NSH_LIST_FIELD != null)
        {
            hackSucceeded = true;
        }
    }

    private BukkitUtils()
    {
    }

    public static boolean isCompatible()
    {
        return (hackSucceeded && CraftServer.class == Bukkit.getServer().getClass() && SimplePluginManager.class == Bukkit.getPluginManager().getClass() && SimpleHelpMap.class == Bukkit.getHelpMap().getClass());
    }

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
                return (String) LOCALE_STRING_FIELD.get(((CraftPlayer) player).getHandle().getLocale());
            }
            catch (Exception e)
            {
            }
        }
        return null;
    }

    private static Field findFirstField(Class type, Object o)
    {
        return findFirstField(type, o.getClass());
    }

    private static Field findFirstField(Class type, Class clazz)
    {
        for (Field field : clazz.getDeclaredFields())
        {
            if (type.isAssignableFrom(field.getType()))
            {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    public static SimpleCommandMap swapCommandMap(SimpleCommandMap commandMap)
    {
        Validate.notNull(commandMap, "The command map must not be null!");

        final Server server = Bukkit.getServer();
        final PluginManager pm = Bukkit.getPluginManager();

        Field serverField = findFirstField(CommandMap.class, server);
        Field pmField = findFirstField(CommandMap.class, pm);

        SimpleCommandMap oldMap = ((CraftServer) server).getCommandMap();
        if (serverField != null && pmField != null)
        {
            try
            {
                serverField.set(server, commandMap);
                pmField.set(pm, commandMap);
            }
            catch (Exception e)
            {
                CubeEngine.getLogger().log(DEBUG, e.getLocalizedMessage(), e);
            }
        }
        return oldMap;
    }

    public static void resetCommandMap()
    {
        SimpleCommandMap current = ((CraftServer) Bukkit.getServer()).getCommandMap();
        if (current instanceof CubeCommandMap)
        {
            CubeCommandMap cubeMap = (CubeCommandMap) current;
            swapCommandMap(current = new SimpleCommandMap(Bukkit.getServer()));

            Collection<Command> commands = cubeMap.getKnownCommands().values();

            for (Command command : commands)
            {
                command.unregister(cubeMap);
                if (command instanceof CubeCommand)
                {
                    continue;
                }
                String prefix = "";
                if (command instanceof PluginCommand)
                {
                    prefix = ((PluginCommand) command).getPlugin().getName();
                }
                else if (command instanceof BukkitCommand)
                {
                    prefix = "bukkit";
                }
                current.register(command.getLabel(), prefix, command);
            }

            reloadHelpMap();
        }
    }

    /**
     * Registers the packet hook injector
     *
     * @param plugin a Plugin to register the injector with
     */
    public static void registerPacketHookInjector(Plugin plugin)
    {
        if (!PacketHookInjector.injected)
        {
            Bukkit.getPluginManager().registerEvents(PacketHookInjector.INSTANCE, plugin);

            for (Player player : Bukkit.getOnlinePlayers())
            {
                PacketHookInjector.INSTANCE.swap(player);
            }
        }
    }

    private static Logger logger = null;
    private static Filter filter = null;
    private static CommandLogFilter commandFilter = null;
    public static void disableCommandLogging()
    {
        if (logger == null)
        {
            logger = Logger.getLogger("Minecraft");
        }
        if (commandFilter == null)
        {
            commandFilter = new CommandLogFilter();
        }
        filter = logger.getFilter();
        logger.setFilter(commandFilter);
    }

    public static void resetCommandLogging()
    {
        if (logger != null)
        {
            if (logger.getFilter() == commandFilter)
            {
                logger.setFilter(filter);
            }
            filter = null;
            logger = null;
        }
    }

    private static class PacketHookInjector implements Listener
    {
        public static final PacketHookInjector INSTANCE = new PacketHookInjector();
        public static boolean injected = false;
        private final ExecutorService executorService;
        private final TaskQueue taskQueue;

        private PacketHookInjector()
        {
            this.executorService = Executors.newSingleThreadExecutor();
            this.taskQueue = new AsyncTaskQueue(this.executorService);
        }

        public void shutdown()
        {
            HandlerList.unregisterAll(this);
            this.taskQueue.shutdown();
            this.executorService.shutdown();

            for (Player player : Bukkit.getOnlinePlayers())
            {
                resetPlayerNetServerHandler(player);
            }
        }

        /**
         * The event listener swaps the joining player's NetServerHandler
         * instance with a custom one including all the magic to make the new
         * NetServerHandler work.
         *
         * @param event the join event object
         */
        @EventHandler(priority = EventPriority.LOW)
        public void onPlayerJoin(PlayerJoinEvent event)
        {
            this.swap(event.getPlayer());
        }

        public void swap(final Player player)
        {
            final EntityPlayer entity = ((CraftPlayer) player).getHandle();

            swapPlayerNetServerHandler(entity, new CubeEngineNetServerHandler(entity, this.taskQueue));
        }
    }

    private static final Location helperLocation = new Location(null, 0, 0, 0);
    public static void swapPlayerNetServerHandler(EntityPlayer player, NetServerHandler newHandler)
    {
        if (NSH_LIST_FIELD == null)
        {
            return;
        }
        NetServerHandler oldHandler = player.netServerHandler;
        try
        {
            if (oldHandler.getClass() != newHandler.getClass())
            {
                Location loc = player.getBukkitEntity().getLocation(helperLocation);
                newHandler.a(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

                ServerConnection sc = player.server.ae();
                ((List<NetServerHandler>)NSH_LIST_FIELD.get(sc)).remove(oldHandler);
                sc.a(newHandler);
                CubeEngine.getLogger().log(DEBUG, "Replaced the NetServerHandler of player ''{0}''", player.getName());
                oldHandler.disconnected = true;
            }
        }
        catch (Exception e)
        {
            player.netServerHandler = oldHandler;
            CubeEngine.getLogger().log(DEBUG, e.getLocalizedMessage(), e);
        }
    }

    public static void resetPlayerNetServerHandler(Player player)
    {
        final EntityPlayer entity = ((CraftPlayer)player).getHandle();

        swapPlayerNetServerHandler(entity, new NetServerHandler(entity.server, entity.netServerHandler.networkManager, entity));
    }

    public static void reloadHelpMap()
    {
        SimpleHelpMap helpMap = (SimpleHelpMap)Bukkit.getHelpMap();

        helpMap.clear();
        helpMap.initializeGeneralTopics();
        helpMap.initializeCommands();
    }

    public static boolean renameItemStack(ItemStack itemStack, boolean asLore, String... string)
    {
        if (!(itemStack instanceof CraftItemStack))
        {
            itemStack = new CraftItemStack(itemStack);
        }
        if (string != null)
        {
            for (int i = 0; i < string.length; ++i)
            {
                string[i] = ChatFormat.parseFormats(string[i]);
            }
        }
        if (itemStack == null || itemStack.getType().equals(Material.AIR))
        {
            return false;
        }
        CraftItemStack cis = ((CraftItemStack)itemStack);
        NBTTagCompound tag = cis.getHandle().getTag();
        if (tag == null)
        {
            cis.getHandle().setTag(tag = new NBTTagCompound());
        }
        if (!tag.hasKey("display"))
        {
            tag.setCompound("display", new NBTTagCompound());
        }
        NBTTagCompound display = tag.getCompound("display");
        if (string == null || string[0] == null || string[0].equals(""))
        {
            if (asLore)
            {
                display.remove("Lore");
            }
            else
            {
                display.remove("Name");
            }
            return true;
        }
        if (asLore)
        {
            NBTTagList list = new NBTTagList();
            for (int i = 0; i < string.length; ++i)
            {
                list.add(new NBTTagString("", string[i]));
            }
            display.set("Lore", list);
        }
        else
        {
            display.setString("Name", string[0]);
        }
        return true;
    }

    public static List<String> getItemStackLore(ItemStack itemStack)
    {
        if (itemStack == null)
        {
            return null;
        }
        CraftItemStack cis = ((CraftItemStack)itemStack);
        if (cis.getHandle() == null)
        {
            return null;
        }
        NBTTagCompound tag = cis.getHandle().getTag();
        if (tag == null)
        {
            cis.getHandle().setTag(tag = new NBTTagCompound());
        }
        if (!tag.hasKey("display"))
        {
            return null;
        }
        NBTTagList loreList = tag.getCompound("display").getList("Lore");
        if (loreList == null || loreList.size() == 0)
        {
            return null;
        }
        List<String> lore = new ArrayList<String>();
        for (int i = 0; i < loreList.size(); ++i)
        {
            lore.add(((NBTTagString)loreList.get(0)).data);
        }
        return lore;
    }

    public static String getItemStackName(ItemStack itemStack)
    {
        if (itemStack == null)
        {
            return null;
        }
        if (!(itemStack instanceof CraftItemStack))
        {
            return null;
        }
        CraftItemStack cis = ((CraftItemStack)itemStack);
        if (cis.getHandle() == null)
        {
            return null;
        }
        NBTTagCompound tag = cis.getHandle().getTag();
        if (tag == null)
        {
            cis.getHandle().setTag(tag = new NBTTagCompound());
        }
        if (!tag.hasKey("display"))
        {
            return null;
        }
        String name = tag.getCompound("display").getString("Name");
        if (name.equals(""))
        {
            return null;
        }
        return name;
    }

    public static ItemStack changeHead(ItemStack head, String name)
    {
        if (head.getType().equals(Material.SKULL_ITEM))
        {
            head.setDurability((short)3);
            CraftItemStack newHead = new CraftItemStack(head);
            NBTTagCompound newHeadData = new NBTTagCompound();
            newHeadData.setString("SkullOwner", name);
            newHead.getHandle().tag = newHeadData;
            return newHead;
        }
        else
        {
            return null;
        }
    }

    public static boolean isInvulnerable(Player player)
    {
        if (player != null)
        {
            if (player instanceof User)
            {
                player = ((User)player).getOfflinePlayer().getPlayer();
            }
            if (player != null && player instanceof CraftPlayer)
            {
                return ((CraftPlayer)player).getHandle().abilities.isInvulnerable;
            }
        }
        return false;
    }

    public static void setInvulnerable(Player player, boolean state)
    {
        if (player != null && player instanceof User)
        {
            player = ((User)player).getOfflinePlayer().getPlayer();
        }
        if (player != null && player instanceof CraftPlayer)
        {
            ((CraftPlayer)player).getHandle().abilities.isInvulnerable = state;
            ((CraftPlayer)player).getHandle().updateAbilities();
        }
    }

    public static boolean compareItemStacks(ItemStack a, ItemStack b)
    {
        if (a == null ^ b == null)
        {
            return false;
        }
        else if (a == null && b == null)
        {
            return true;
        }
        if (a.getTypeId() == b.getTypeId() && a.getDurability() == b.getDurability())
        {
            if (a instanceof CraftItemStack && b instanceof CraftItemStack)
            {
                if (!((CraftItemStack)a).getHandle().getTag().equals(((CraftItemStack)b).getHandle().getTag()))
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static void cleanup()
    {
        PacketHookInjector.INSTANCE.shutdown();

        resetCommandMap();
        resetCommandLogging();
    }
}
