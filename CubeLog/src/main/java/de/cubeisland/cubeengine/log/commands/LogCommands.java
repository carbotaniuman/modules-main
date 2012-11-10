package de.cubeisland.cubeengine.log.commands;

import de.cubeisland.cubeengine.core.command.CommandContext;
import de.cubeisland.cubeengine.core.command.ContainerCommand;
import de.cubeisland.cubeengine.core.command.annotation.Command;
import de.cubeisland.cubeengine.core.command.annotation.Flag;
import de.cubeisland.cubeengine.core.command.annotation.Param;
import de.cubeisland.cubeengine.core.command.args.IntArg;
import de.cubeisland.cubeengine.core.module.Module;
import de.cubeisland.cubeengine.core.user.User;
import java.util.Date;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

public class LogCommands extends ContainerCommand
{
    public LogCommands(Module module)
    {
        super(module, "log", "Searches in the database for needed informations.", "cl");
    }

    @Command(
    desc = "Displays all possible parameters.")
    public void params(CommandContext context)
    {
        context.sendMessage("You used the /log params command!");
    }

    @Command(
    names =
    {
        "lookup"
    },
    desc = "Lookups",
    usage = "<params>",
    flags =
    {
        @Flag(longName = "selection", name = "sel"), // only search in Selection
        @Flag(longName = "created", name = "c"), // only search for placed blocks (on by default)
        @Flag(longName = "destroyed", name = "d"), // only search for breaked blocks (on by default)
        @Flag(longName = "chat", name = "ch"), //only search for chatlogs (off by default)
        @Flag(longName = "chestaccess", name = "chest"), //only search for chestaccess (off by default)
        @Flag(longName = "coordinates", name = "coords"),//display position (off by default)
        @Flag(longName = "descending", name = "desc"), //sort in descending order (default ascending)
    },
    params = {
        //@Param(names ={"player", "p"}, type = User[].class), TODO
        @Param(names = "area", type = IntArg.class),
        //@Param(names = "block", type = ItemStack[].class), TODO
        //@Param(names = {"since", "time"}, type = Date.class), TODO
        //@Param(names = "before", type = Date.class),
        //@Param(names = "limit", type = Date.class),
        //@Param(names = "world", type = World.class),
    })
    public void lookup(CommandContext context)
    {
        ItemStack[] blocktypes;
        if (context.hasNamed("block"))
        {
            blocktypes = context.getNamed("block", ItemStack[].class);
        }
    }
}
