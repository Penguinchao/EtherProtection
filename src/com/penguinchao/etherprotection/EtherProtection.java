package com.penguinchao.etherprotection;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EtherProtection extends JavaPlugin {
	protected EventListeners listener;
	private static boolean debugEnabled;
	private static EtherProtection main;
	@Override
	public void onEnable(){
		saveDefaultConfig();
		main = this;
		debugEnabled = getConfig().getBoolean("debug-enabled");
		listener = new EventListeners(this);
		ProtectionManager.EstablishConnection(this);
	}
	protected static void debugTrace(String message){
		if(debugEnabled){
			main.getLogger().info("[DEBUG] "+message);
		}
	}
	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) { 
		if ( cmd.getName().equalsIgnoreCase("cprivate") ){
			if(!playerCheck(sender)){
				return false;
			}
			Player player = (Player) sender;
			if(!player.hasPermission("etherprotection.mode.private")){
				player.sendMessage(ChatColor.RED+"You do not have permission");
				return false;
			}
			listener.setPlacingPrivate(player.getUniqueId());
			player.sendMessage(ChatColor.GREEN+"Blocks that you place will now be completely inaccessible to others");
		}else if ( cmd.getName().equalsIgnoreCase("cpublic") ){
			if(!playerCheck(sender)){
				return false;
			}
			Player player = (Player) sender;
			if(!player.hasPermission("etherprotection.mode.public")){
				player.sendMessage(ChatColor.RED+"You do not have permission");
				return false;
			}
			listener.setPlacingPublic(player.getUniqueId());
			player.sendMessage(ChatColor.GREEN+"Blocks that you place will now be unbreakable to others");
		}else if ( cmd.getName().equalsIgnoreCase("cnone") ){
			if(!playerCheck(sender)){
				return false;
			}
			Player player = (Player) sender;
			listener.setPlacingNoProtection(player.getUniqueId());
			player.sendMessage(ChatColor.GREEN+"Blocks that you place will not have any protections");
		}else if ( cmd.getName().equalsIgnoreCase("cmodify") ){
			if(!playerCheck(sender)){
				return false;
			}
			if(args.length != 1){
				sender.sendMessage(ChatColor.GREEN+"Change the protection type of the block you are looking at:");
				sender.sendMessage(ChatColor.YELLOW+"/cmodify public"+ChatColor.GREEN+" Make the block usable, but unbreakable to others");
				sender.sendMessage(ChatColor.YELLOW+"/cmodify private"+ChatColor.GREEN+" Make the block unusable and unbreakable to others");
				sender.sendMessage(ChatColor.YELLOW+"/cmodify none"+ChatColor.GREEN+" Remove all protection from the block");
				return false;
			}
			Player player = (Player) sender;
			Block block = player.getTargetBlock((Set<Material>) null, 6);
			if(block == null){
				player.sendMessage(ChatColor.RED+"Please look at a block to be protected");
				return false;
			}else if(!ProtectionManager.isProtectedType(block)){
				sender.sendMessage(ChatColor.RED+"That type of block cannot have protection");
				return false;
			}
			UUID owner = ProtectionManager.getBlockOwner(block);
			boolean uuidWasNull = false;
			boolean isBypassing = false;
			if(owner == null){
				//Block is not owned - continue
				owner = player.getUniqueId();
				uuidWasNull = true;
			}else if(player.getUniqueId().equals(owner)){
				//Modifying own block - continue
			}else{
				//Modifying someone else's block
				if(player.hasPermission("etherprotection.admin.modify")){
					//Has admin permission - continue
					isBypassing = true;
				}else{
					player.sendMessage(ChatColor.RED+"This block is protected by "+Bukkit.getOfflinePlayer(owner).getName() );
					return false;
				}
			}
			if(args[0].equals("public")){
				//Setting as public
				if(!player.hasPermission("etherprotection.mode.public")){
					player.sendMessage(ChatColor.RED+"You do not have permission");
					return false;
				}
				ProtectionManager.setOwnership(block, owner, true);
				player.sendMessage(ChatColor.GREEN+"You have set this block to be usable, but unbreakable to others");
				if(isBypassing){
					player.sendMessage(ChatColor.GRAY+"This block belongs to "+Bukkit.getOfflinePlayer(owner).getName());
				}
			}else if(args[0].equals("private")){
				//Setting as private
				if(!player.hasPermission("etherprotection.mode.private")){
					player.sendMessage(ChatColor.RED+"You do not have permission");
					return false;
				}
				ProtectionManager.setOwnership(block, owner, false);
				player.sendMessage(ChatColor.GREEN+"You have set this block to be unusable and unbreakable to others");
				if(isBypassing){
					player.sendMessage(ChatColor.GRAY+"This block belongs to "+Bukkit.getOfflinePlayer(owner).getName());
				}
			}else if(args[0].equals("none")){
				//Setting as no protection
				if(uuidWasNull){
					player.sendMessage(ChatColor.GREEN+"This block has no protection");
					return false;
				}else{
					ProtectionManager.deleteOwnership(block);
					player.sendMessage(ChatColor.GREEN+"You have removed any protection from this block");
				}
				if(isBypassing){
					player.sendMessage(ChatColor.GRAY+"This block belonged to "+Bukkit.getOfflinePlayer(owner).getName());
				}
			}else{
				//Incorrect syntax
				sender.sendMessage(ChatColor.GREEN+"Change the protection type of the block you are looking at:");
				sender.sendMessage(ChatColor.YELLOW+"/cmodify public"+ChatColor.GREEN+" Make the block usable, but unbreakable to others");
				sender.sendMessage(ChatColor.YELLOW+"/cmodify private"+ChatColor.GREEN+" Make the block unusable and unbreakable to others");
				sender.sendMessage(ChatColor.YELLOW+"/cmodify none"+ChatColor.GREEN+" Remove all protection from the block");
				return false;
			}
		}else if ( cmd.getName().equalsIgnoreCase("cvisitor") ){
			if(!playerCheck(sender)){
				return false;
			}
			if(args.length != 2){
				sender.sendMessage(ChatColor.GREEN+"Add or remove visitors who can access the block that you are looking at");
				sender.sendMessage(ChatColor.YELLOW+"/cvisitor add <name>"+ChatColor.GREEN+" allow the player to use the block");
				sender.sendMessage(ChatColor.YELLOW+"/cvisitor remove <name>"+ChatColor.GREEN+" remove the ability for a player to use the block");
			}
			Player player = (Player) sender;
			Block block = player.getTargetBlock((Set<Material>) null, 6);
			if(block == null){
				player.sendMessage(ChatColor.RED+"Please look at a block to be protected");
				return false;
			}else if(!ProtectionManager.isProtectedType(block)){
				sender.sendMessage(ChatColor.RED+"That type of block cannot have protection");
				return false;
			}
			UUID owner = ProtectionManager.getBlockOwner(block);
			boolean isBypassing = false;
			if(owner == null){
				//Block is not owned - cannot add visitor
				sender.sendMessage(ChatColor.RED+"That block is not owned by anyone");
				return false;
			}else if(player.getUniqueId().equals(owner)){
				//Modifying own block - continue
			}else{
				//Modifying someone else's block
				if(player.hasPermission("etherprotection.admin.modify")){
					//Has admin permission - continue
					isBypassing = true;
				}else{
					player.sendMessage(ChatColor.RED+"This block is protected by "+Bukkit.getOfflinePlayer(owner).getName() );
					return false;
				}
			}
			UUID visitorToAdd = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
			if(visitorToAdd == null){
				sender.sendMessage(ChatColor.RED+"That player could not be found");
				return false;
			}else if(args[0].equals("add")){
				ProtectionManager.addVisitor(block, visitorToAdd);
				sender.sendMessage(ChatColor.GREEN+"Added "+args[1]+" to visitors");
			}else if(args[0].equals("remove")){
				ProtectionManager.removeVisitor(block, visitorToAdd);
				sender.sendMessage(ChatColor.GREEN+"Removed "+args[1]+" from visitors");
			}else{
				sender.sendMessage(ChatColor.GREEN+"Add or remove visitors who can access the block that you are looking at");
				sender.sendMessage(ChatColor.YELLOW+"/cvisitor add <name>"+ChatColor.GREEN+" allow the player to use the block");
				sender.sendMessage(ChatColor.YELLOW+"/cvisitor remove <name>"+ChatColor.GREEN+" remove the ability for a player to use the block");
				return false;
			}
			if(isBypassing){
				player.sendMessage(ChatColor.GRAY+"This block belongs to "+Bukkit.getOfflinePlayer(owner).getName());
			}
		}else if ( cmd.getName().equalsIgnoreCase("cinspect") ){
			if(!playerCheck(sender)){
				return false;
			}
			Player player = (Player) sender;
			Block block = player.getTargetBlock((Set<Material>) null, 6);
			if(block == null){
				//player.sendMessage(ChatColor.RED+"Please look at a protected block");
				sender.sendMessage(ChatColor.GREEN+"This block has no owner");
				return false;
			}else if(!ProtectionManager.isProtectedType(block)){
				//player.sendMessage(ChatColor.RED+"Please look at a protected block");
				sender.sendMessage(ChatColor.GREEN+"This block has no owner");
				return false;
			}
			UUID owner = ProtectionManager.getBlockOwner(block);
			if(owner == null){
				sender.sendMessage(ChatColor.GREEN+"This block has no owner");
				return false;
			}else if(owner.equals(player.getUniqueId())){
				//Player owns block -- allow
			}else if(!player.hasPermission("etherprotection.admin.inspect")){
				sender.sendMessage(ChatColor.RED+"You do not own this block");
				return false;
			}
			String playerName = Bukkit.getOfflinePlayer(owner).getName();
			String blockType = block.getType().toString();
			String protectionType;
			if(ProtectionManager.isPublic(block)){
				protectionType = "Public";
			}else{
				protectionType = "Private";
			}
			player.sendMessage(ChatColor.GRAY+"Owner: "+ChatColor.GREEN+playerName);
			player.sendMessage(ChatColor.GRAY+"UUID: "+ChatColor.GREEN+owner.toString());
			player.sendMessage(ChatColor.GRAY+"Block Type: "+ChatColor.GREEN+blockType);
			player.sendMessage(ChatColor.GRAY+"Protection Type: "+ChatColor.GREEN+protectionType);
			List<UUID> visitors = ProtectionManager.getVisitors(block);
			if(visitors == null){
				player.sendMessage(ChatColor.GRAY+"Visitors: "+ChatColor.GREEN+"None");
				return false;
			}
			boolean isFirst = true;
			String visitorList = "";
			for(UUID entry : visitors){
				if(isFirst){
					visitorList = Bukkit.getOfflinePlayer(entry).getName();
				}else{
					visitorList = visitorList + ", " + Bukkit.getOfflinePlayer(entry).getName();
				}
			}
			player.sendMessage(ChatColor.GRAY+"Visitors: "+ChatColor.GREEN+visitorList);
		}else if ( cmd.getName().equalsIgnoreCase("cpurge") ){
			boolean hasPermission = true;
			if(sender instanceof Player){
				Player player = (Player) sender;
				if(!player.hasPermission("etherprotection.admin.purge")){
					hasPermission = false;
				}
			}
			if(!hasPermission){
				sender.sendMessage(ChatColor.RED+"You do not have permission");
				return false;
			}
			if(args.length != 1){
				sender.sendMessage(ChatColor.YELLOW+"/cpurge <name>"+ChatColor.GREEN+" Delete all protections belonging to someone");
				return false;
			}
			OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
			if(player == null){
				sender.sendMessage(ChatColor.RED+"That player could not be found");
				return false;
			}
			UUID purgeMe = player.getUniqueId();
			ProtectionManager.purgePlayer(purgeMe);
			sender.sendMessage(ChatColor.GREEN+"You have deleted all records for "+player.getName());
			return false;
		}else if ( cmd.getName().equalsIgnoreCase("csetowner") ){
			if(!playerCheck(sender)){
				return false;
			}
			Player player = (Player) sender;
			if(!player.hasPermission("etherprotection.admin.setowner")){
				player.sendMessage(ChatColor.RED+"You do not have permission");
				return false;
			}else if(args.length != 1){
				player.sendMessage(ChatColor.YELLOW+"/csetowner <name>"+ChatColor.GREEN+" Remove all ownership of the block you are looking at, then replace it with the specified player");
				return false;
			}
			Block block = player.getTargetBlock((Set<Material>) null, 6);
			if(block == null){
				player.sendMessage(ChatColor.RED+"Please look at a protected block");
				return false;
			}else if(!ProtectionManager.isProtectedType(block)){
				player.sendMessage(ChatColor.RED+"Please look at a protected block");
				return false;
			}
			OfflinePlayer newOwner = Bukkit.getOfflinePlayer(args[0]);
			if(newOwner == null){
				player.sendMessage(ChatColor.RED+"Could not find player");
				return false;
			}
			UUID newOwnerUUID = newOwner.getUniqueId();
			boolean isPublic = false;
			if(ProtectionManager.getBlockOwner(block) != null){
				isPublic = ProtectionManager.isPublic(block);
			}
			if(ProtectionManager.setOwnership(block, newOwnerUUID, isPublic)){
				sender.sendMessage(ChatColor.GREEN+"You have successfully set the ownership of this block");
			}else{
				sender.sendMessage(ChatColor.RED+"Something went wrong. See console");
			}
		}else if ( cmd.getName().equalsIgnoreCase("chelp") ){
			sender.sendMessage(ChatColor.YELLOW+"/chelp"+ChatColor.GREEN+" Show this help page");
			if( !(sender instanceof Player) ){
				sender.sendMessage(ChatColor.YELLOW+"/cpurge <name>"+ChatColor.GREEN+" Delete all protections belonging to someone");
				return false;
			}
			Player player = (Player) sender;
			if(player.hasPermission("etherprotection.admin.purge")){
				sender.sendMessage(ChatColor.YELLOW+"/cpurge <name>"+ChatColor.GREEN+" Delete all protections belonging to someone");
			}
			sender.sendMessage(ChatColor.DARK_GREEN+"Change the protection type of the block you are looking at:");
			if(player.hasPermission("etherprotection.mode.public")){
				sender.sendMessage(ChatColor.DARK_GREEN+"- "+ChatColor.YELLOW+"/cmodify public"+ChatColor.GREEN+" Make the block usable, but unbreakable to others");
			}
			if(player.hasPermission("etherprotection.mode.private")){
				sender.sendMessage(ChatColor.DARK_GREEN+"- "+ChatColor.YELLOW+"/cmodify private"+ChatColor.GREEN+" Make the block unusable and unbreakable to others");
			}
			sender.sendMessage(ChatColor.DARK_GREEN+"- "+ChatColor.YELLOW+"/cmodify none"+ChatColor.GREEN+" Remove all protection from the block");
			if(player.hasPermission("etherprotection.mode.public")){
				sender.sendMessage(ChatColor.YELLOW+"/cpublic"+ChatColor.GREEN+" Future blocks you place will be usable, but unbreakable to others");
			}
			if(player.hasPermission("etherprotection.mode.private")){
				sender.sendMessage(ChatColor.YELLOW+"/cprivate"+ChatColor.GREEN+" Future blocks you place will be unusable and unbreakable to others");
			}
			sender.sendMessage(ChatColor.YELLOW+"/cnone"+ChatColor.GREEN+" Future blocks you place will not be automatically protected");
			sender.sendMessage(ChatColor.DARK_GREEN+"Add or remove visitors who can access the block that you are looking at");
			sender.sendMessage(ChatColor.DARK_GREEN+"- "+ChatColor.YELLOW+"/cvisitor add <name>"+ChatColor.GREEN+" allow the player to use the block");
			sender.sendMessage(ChatColor.DARK_GREEN+"- "+ChatColor.YELLOW+"/cvisitor remove <name>"+ChatColor.GREEN+" remove the ability for a player to use the block");
			//if(player.hasPermission("etherprotection.admin.inspect")){
			sender.sendMessage(ChatColor.YELLOW+"/cinspect <name>"+ChatColor.GREEN+" View the protection information for the block you are looking at");
			//}
			if(player.hasPermission("etherprotection.admin.setowner")){
				sender.sendMessage(ChatColor.YELLOW+"/csetowner <name>"+ChatColor.GREEN+" Set the owner for the block you are looking at");
			}
		}
		return false;
	}
	private boolean playerCheck(CommandSender sender){
		if(sender instanceof Player){
			return true;
		}else{
			sender.sendMessage(ChatColor.RED+"That command can only be sent by a player");
			return false;
		}
	}
}
