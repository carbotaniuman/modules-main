package de.cubeisland.cubeengine.basics.command.moderation;

import de.cubeisland.cubeengine.basics.Basics;
import de.cubeisland.cubeengine.basics.BasicsAttachment;
import de.cubeisland.cubeengine.basics.BasicsPerm;
import de.cubeisland.cubeengine.core.command.CommandContext;
import de.cubeisland.cubeengine.core.command.parameterized.Flag;
import de.cubeisland.cubeengine.core.command.parameterized.ParameterizedContext;
import de.cubeisland.cubeengine.core.command.reflected.Command;
import de.cubeisland.cubeengine.core.command.sender.CommandSender;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.core.util.InventoryGuardFactory;
import org.bukkit.inventory.ItemStack;

/**
 * Contains commands that allow to modify an inventory.
 * <p>/invsee
 * <p>/clearinventory
 * <p>/stash
 */
public class InventoryCommands
{
    private Basics basics;

    public InventoryCommands(Basics basics)
    {
        this.basics = basics;
    }

    @Command(desc = "Allows you to see into the inventory of someone else.",
            flags = @Flag(longName = "force", name = "f"),
            usage = "<player>", min = 1, max = 1)
    public void invsee(ParameterizedContext context)
    {
        if (context.getSender() instanceof User)
        {
            User sender = (User)context.getSender();
            User user = context.getUser(0);
            if (user == null)
            {
                context.sendMessage("basics", "&cUser &2%s &cnot found!", context.getString(0));
                return;
            }
            boolean allowModify = false;
            if (BasicsPerm.COMMAND_INVSEE_MODIFY.isAuthorized(sender))
            {
                allowModify = true;
                if (!(context.hasFlag("f") && BasicsPerm.COMMAND_INVSEE_MODIFY_FORCE.isAuthorized(sender))
                    && BasicsPerm.COMMAND_INVSEE_MODIFY_PREVENT.isAuthorized(user))
                {
                    allowModify = false;
                }
            }
            if (BasicsPerm.COMMAND_INVSEE_NOTIFY.isAuthorized(user))
            {
                user.sendMessage("basics", "&2%s &eis looking into your inventory.", sender.getName());
            }
            InventoryGuardFactory guard = InventoryGuardFactory.prepareInventory(user.getInventory(), sender);
            if (!allowModify)
            {
                guard.blockPutInAll().blockTakeOutAll();
            }
            guard.submitInventory(this.basics, true);
            return;
        }
        context.sendMessage("basics", "&cThis command can only be used by a player!");
    }

    @Command(desc = "Stashes or unstashes your inventory to reuse later")
    public void stash(CommandContext context)
    {
        if (context.getSender() instanceof User)
        {
            User sender = (User)context.getSender();
            ItemStack[] stashedInv = sender.get(BasicsAttachment.class).getStashedInventory();
            ItemStack[] stashedArmor = sender.get(BasicsAttachment.class).getStashedArmor();
            ItemStack[] invToStash = sender.getInventory().getContents().clone();
            ItemStack[] armorToStash = sender.getInventory().getArmorContents().clone();
            if (stashedInv != null)
            {
                sender.getInventory().setContents(stashedInv);
            }
            else
            {
                sender.getInventory().clear();
            }

            sender.get(BasicsAttachment.class).setStashedInventory(invToStash);
            if (stashedArmor != null)
            {
                sender.getInventory().setBoots(stashedArmor[0]);
                sender.getInventory().setLeggings(stashedArmor[1]);
                sender.getInventory().setChestplate(stashedArmor[2]);
                sender.getInventory().setHelmet(stashedArmor[3]);
            }
            else
            {
                sender.getInventory().setBoots(null);
                sender.getInventory().setLeggings(null);
                sender.getInventory().setChestplate(null);
                sender.getInventory().setHelmet(null);
            }
            sender.get(BasicsAttachment.class).setStashedArmor(armorToStash);
            sender.sendMessage("basics", "&aSwapped stashed Inventory!");
            return;
        }
        context.sendMessage("core", "&cYeah you better put it away!");
    }

    @Command(names = {"clearinventory", "ci", "clear"},
            desc = "Clears the inventory", usage = "[player]",
            flags = @Flag(longName = "removeArmor", name = "ra"), max = 1)
    @SuppressWarnings("deprecation")
    public void clearinventory(ParameterizedContext context)
    {
        CommandSender sender = context.getSender();
        final User target;
        if (context.hasArgs())
        {
            target = context.getArg(0, User.class);
            if (target == null)
            {
                sender.sendMessage("basics", "&cThe specified user was not found!");
                return;
            }
        }
        else if (sender instanceof User)
        {
            target = (User)sender;
        }
        else
        {
            sender.sendMessage("basics", "&cThere is no inventory to clear in the console...");
            return;
        }

        if (sender != target && !BasicsPerm.COMMAND_CLEARINVENTORY_OTHER.isAuthorized(sender))
        {
            context.sendMessage("basics", "&cYou are not allowed to clear the inventory of other users!");
            return;
        }

        target.getInventory().clear();
        if (context.hasFlag("ra"))
        {
            target.getInventory().setBoots(null);
            target.getInventory().setLeggings(null);
            target.getInventory().setChestplate(null);
            target.getInventory().setHelmet(null);
        }
        target.updateInventory();

        if (sender == target)
        {
            sender.sendMessage("basics", "&aYour inventory has been cleared! :)");
        }
        else
        {
            target.sendMessage("basics", "&eYour inventory has been cleared by &6%s&e!", sender.getName());
            sender.sendMessage("basics", "&aThe inventory of &6%s&a has been cleared!", target.getName());
        }
    }
}
