package de.cubeisland.cubeengine.writer;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;

import de.cubeisland.cubeengine.core.Core;
import de.cubeisland.cubeengine.core.command.CommandContext;
import de.cubeisland.cubeengine.core.command.annotation.Command;
import de.cubeisland.cubeengine.core.command.annotation.Param;
import de.cubeisland.cubeengine.core.user.User;

public class EditCommand 
{
	Core core = null;
	
	public EditCommand(Core core){
		this.core = core;
	}
	
	@Command(
			names = {"edit", "rewrite"}, 
			desc = "Edit a sign or unsign a book",
			usage = "[Line1 \"text\"] [Line2 \"text\"] [Line3 \"text\"] [Line4 \"text\"] ",
			params = {
					@Param(
							names = {"1", "Line1"}, 
							types = {String.class}
						),
					@Param(
							names = {"2", "Line2"}, 
							types = {String.class}
						),
					@Param(
							names = {"3", "Line3"}, 
							types = {String.class}
						),
					@Param(
							names = {"4", "Line4"}, 
							types = {String.class}
						)
					}
		)
	public void edit(CommandContext context)
	{
		
		User user = context.getSenderAsUser("writer", "This command can only be used from ingame");
		
		if (user.getItemInHand().getType() == Material.WRITTEN_BOOK)
		{
			ItemStack item = user.getItemInHand();
			BookItem unsigned = new BookItem(item);
			
			unsigned.setAuthor("");
			unsigned.setTitle("");
			
			item = unsigned.getItemStack();
			item.setType(Material.BOOK_AND_QUILL);
			
			user.sendMessage("Your book is now unsigned and ready to be edited");
			return;
		}
		else
		{
			Block target = user.getTargetBlock(null, 10);
			
			if (target.getType() == Material.WALL_SIGN || target.getType() == Material.SIGN_POST)
			{
				if (context.namedCount() < 1){
					user.sendMessage(ChatColor.RED + "You need to speccify at least one parameter");
					return;
				}
				
				Sign sign = (Sign)target.getState();
				
				Map<String, Object[]> params = context.getNamed();	
				for (String key : params.keySet())
				{
					sign.setLine(Integer.parseInt(key) - 1, context.getNamed(key, String.class));
				}
				
				sign.update();
				
				user.sendMessage("The sign has been changed");	
				return;
			}
			else
			{
				user.sendMessage(ChatColor.RED + "You need to have a signed book in hand or be looking at a sign less than 10 blocks away");
				if (core.isDebug())
				{
					user.sendMessage("You where looking at: " + target.getType().name());
				}
			}
		}
	}
}
