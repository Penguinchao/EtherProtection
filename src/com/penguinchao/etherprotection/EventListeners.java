package com.penguinchao.etherprotection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
//import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
//import org.bukkit.inventory.Inventory;

public class EventListeners implements Listener {
	private List<UUID> placingPrivate;
	private List<UUID> placingPublic;
	public EventListeners(EtherProtection etherProtection) {
		etherProtection.getServer().getPluginManager().registerEvents(this, etherProtection);
		placingPrivate = new ArrayList<UUID>();
		placingPublic = new ArrayList<UUID>();
	}
	protected void setPlacingPrivate(UUID player){
		setPlacingNoProtection(player);
		placingPrivate.add(player);
	}
	protected void setPlacingPublic(UUID player){
		setPlacingNoProtection(player);
		placingPublic.add(player);
	}
	protected void setPlacingNoProtection(UUID player){
		placingPrivate.remove(player);
		placingPublic.remove(player);
	}
	protected boolean isPlacingPublic(UUID player){
		return placingPublic.contains(player);
	}
	protected boolean isPlacingPrivate(UUID player){
		return placingPrivate.contains(player);
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onBlockPlace(BlockPlaceEvent event){
		EtherProtection.debugTrace("[onBlockPlace] Begin");
		//Check cancelled
		if(event.isCancelled()){
			EtherProtection.debugTrace("[onBlockPlace] Event was cancelled - No protection needed");
			EtherProtection.debugTrace("[onBlockPlace] Done");
			return;
		}
		//Null Check
		if(event.getBlock() == null){
			EtherProtection.debugTrace("[onBlockPlace] Block is null");
			EtherProtection.debugTrace("[onBlockPlace] Done");
			return;
		}else{
			EtherProtection.debugTrace("[onBlockPlace] Block is not null");
		}
		//Check Block Type
		if(ProtectionManager.isProtectedType(event.getBlock())){
			EtherProtection.debugTrace("[onBlockPlace] Block is a protected type");
		}else{
			EtherProtection.debugTrace("[onBlockPlace] Block is not a protected type");
			EtherProtection.debugTrace("[onBlockPlace] Done");
			return;
		}
		//Remove any leftover protections
		EtherProtection.debugTrace("[onBlockPlace] Removing existing ownership to prevent bugs");
		ProtectionManager.deleteOwnership(event.getBlock());
		//Set ownership
		if(isPlacingPrivate(event.getPlayer().getUniqueId())){
			EtherProtection.debugTrace("[onBlockPlace] Player is set to place private blocks");
			ProtectionManager.setOwnership(event.getBlock(), event.getPlayer().getUniqueId(), false);
			event.getPlayer().sendMessage(ChatColor.GREEN+"You have placed a private block: "+event.getBlock().getType().toString());
		}else if(isPlacingPublic(event.getPlayer().getUniqueId())){
			EtherProtection.debugTrace("[onBlockPlace] Player is set to place public blocks");
			ProtectionManager.setOwnership(event.getBlock(), event.getPlayer().getUniqueId(), true);
			event.getPlayer().sendMessage(ChatColor.GREEN+"You have placed a public block: "+event.getBlock().getType().toString());
		}else{
			EtherProtection.debugTrace("[onBlockPlace] Player is not set to place public or private blocks");
			//ProtectionManager.setOwnership(event.getBlock(), event.getPlayer().getUniqueId(), false);
		}
		EtherProtection.debugTrace("[onBlockPlace] Done");
	}
	@EventHandler (priority = EventPriority.HIGH)
	public void onBlockBreak(BlockBreakEvent event){
		EtherProtection.debugTrace("[onBlockBreak] Begin");
		//Check cancelled
		if(event.isCancelled()){
			EtherProtection.debugTrace("[onBlockBreak] Event is cancelled");
			EtherProtection.debugTrace("[onBlockBreak] Done");
			return;
		}else{
			EtherProtection.debugTrace("[onBlockBreak] Event is not cancelled");
		}
		//Check if valid block
		if(ProtectionManager.isProtectedType(event.getBlock())){
			EtherProtection.debugTrace("[onBlockBreak] Block is a protected type");
		}else{
			EtherProtection.debugTrace("[onBlockBreak] Block is not a protected type");
			EtherProtection.debugTrace("[onBlockBreak] Done");
			return;
		}
		//Check ownership
		UUID owner = ProtectionManager.getBlockOwner(event.getBlock());
		if(owner == null){
			EtherProtection.debugTrace("[onBlockBreak] Block does not have an owner");
			EtherProtection.debugTrace("[onBlockBreak] Done");
			return;
		}else{
			EtherProtection.debugTrace("[onBlockBreak] Block has an owner");
		}
		//Check if block was broken by a player
		if(event.getPlayer() == null){
			EtherProtection.debugTrace("[onBlockBreak] Player is null - Cancelling Event");
			EtherProtection.debugTrace("[onBlockBreak] Done");
			event.setCancelled(true);
			return;
		}else{
			EtherProtection.debugTrace("[onBlockBreak] Player is not null");
		}
		//Check if player is owner
		if(event.getPlayer().getUniqueId().equals(owner)){
			EtherProtection.debugTrace("[onBlockBreak] Player is the owner - Allowing Event");
			//Notify Owner
			event.getPlayer().sendMessage(ChatColor.GREEN+"You have broken your "+event.getBlock().getType().toString());
			EtherProtection.debugTrace("[onBlockBreak] Done");
			return;
		}else if(event.getPlayer().hasPermission("etherprotection.admin.bypass")){
			EtherProtection.debugTrace("[onBlockBreak] Player is not the owner, but they have bypass permissions");
			event.getPlayer().sendMessage(ChatColor.GRAY+"You have broken the "+event.getBlock().getType().toString()+" owned by "+Bukkit.getOfflinePlayer(owner).getName());
			EtherProtection.debugTrace("[onBlockBreak] Done");
			return;
		}else{
			EtherProtection.debugTrace("[onBlockBreak] Player is not the owner - Cancelling Event");
			event.getPlayer().sendMessage(ChatColor.RED+"That "+event.getBlock().getType().toString()+" is owned by "+Bukkit.getOfflinePlayer(owner).getName());
			event.setCancelled(true);
			EtherProtection.debugTrace("[onBlockBreak] Done");
			return;
		}
		//NOTE: Protection will be removed upon the monitor priority check
	}
	@EventHandler (priority = EventPriority.MONITOR)
	public void onBlockBreakCheck(BlockBreakEvent event){
		EtherProtection.debugTrace("[onBlockBreakCheck] Begin");
		if(event.isCancelled()){
			EtherProtection.debugTrace("[onBlockBreakCheck] Event is cancelled");
			EtherProtection.debugTrace("[onBlockBreakCheck] Done");
			return;
		}else{
			EtherProtection.debugTrace("[onBlockBreakCheck] Event is not cancelled");
		}
		if(event.getBlock() == null){
			EtherProtection.debugTrace("[onBlockBreakCheck] Block is Null");
			EtherProtection.debugTrace("[onBlockBreakCheck] Done");
			return;
		}else{
			EtherProtection.debugTrace("[onBlockBreakCheck] Block is not Null");
		}
		EtherProtection.debugTrace("[onBlockBreakCheck] Deleting ownership to prevent bugs");
		ProtectionManager.deleteOwnership(event.getBlock());
		EtherProtection.debugTrace("[onBlockBreakCheck] Done");
	}
	@EventHandler
	public void onBlockInteract(PlayerInteractEvent event){
		EtherProtection.debugTrace("[onBlockInteract] Begin");
		//Check Block existance
		if(event.hasBlock()){
			EtherProtection.debugTrace("[onBlockInteract] Event has Block");
		}else{
			EtherProtection.debugTrace("[onBlockInteract] Event does not have a Block");
			EtherProtection.debugTrace("[onBlockInteract] Done");
			return;
		}
		//Null Check
		if(event.getClickedBlock() == null){
			EtherProtection.debugTrace("[onBlockInteract] Block is null");
			EtherProtection.debugTrace("[onBlockInteract] Done");
			return;
		}else{
			EtherProtection.debugTrace("[onBlockInteract] Block is not null");
		}
		//Check Type
		if(ProtectionManager.isProtectedType(event.getClickedBlock())){
			EtherProtection.debugTrace("[onBlockInteract] Block is a protected type");
		}else{
			EtherProtection.debugTrace("[onBlockInteract] Block is not a protected type");
			EtherProtection.debugTrace("[onBlockInteract] Done");
			return;
		}
		//Check Ownership
		UUID owner = ProtectionManager.getBlockOwner(event.getClickedBlock());
		if(owner == null){
			//No Owner
			EtherProtection.debugTrace("[onBlockInteract] Block is not owned");
			EtherProtection.debugTrace("[onBlockInteract] Done");
			return;
		}else if(event.getPlayer().getUniqueId().equals(owner)){
			//Player is owner
			EtherProtection.debugTrace("[onBlockInteract] Player is owner");
			EtherProtection.debugTrace("[onBlockInteract] Done");
			return;
		}else if(ProtectionManager.isPublic(event.getClickedBlock())){
			//Block is public
			EtherProtection.debugTrace("[onBlockInteract] Block is public");
			EtherProtection.debugTrace("[onBlockInteract] Done");
			return;
		}else if(ProtectionManager.isVisitor(event.getClickedBlock(), event.getPlayer().getUniqueId())){
			//Player is a visitor
			EtherProtection.debugTrace("[onBlockInteract] Player is visitor");
			EtherProtection.debugTrace("[onBlockInteract] Done");
			return;
		}else{
			//Player is not authorized
			EtherProtection.debugTrace("[onBlockInteract] Player is not authorized to use block");
			event.getPlayer().sendMessage(ChatColor.RED + "That block is protected by "+ Bukkit.getOfflinePlayer(owner).getName());
			if(event.getPlayer().hasPermission("etherprotection.admin.bypass")){
				EtherProtection.debugTrace("[onBlockInteract] Player has Admin bypass perms");
				event.getPlayer().sendMessage(ChatColor.GRAY+"Allowing use anyway, because you have permission to bypass protections");
				EtherProtection.debugTrace("[onBlockInteract] Done");
				return;
			}else{
				EtherProtection.debugTrace("[onBlockInteract] Player does not have Admin bypass perms");
				event.setCancelled(true);
				EtherProtection.debugTrace("[onBlockInteract] Done");
				return;
			}
			
		}
	}
	@EventHandler (priority = EventPriority.HIGH)
	public void onPhysicsUpdate(BlockPhysicsEvent event){
		EtherProtection.debugTrace("[onPysicsUpdate] Begin");
		//Check if event is cancelled
		if(event.isCancelled()){
			EtherProtection.debugTrace("[onPhysicsUpdate] Event is already cancelled");
			EtherProtection.debugTrace("[onPhysicsUpdate] Done");
			return;
		}else{
			EtherProtection.debugTrace("[onPhysicsUpdate] Event is not cancelled");
		}
		//Check if block is null
		if(event.getBlock() == null){
			EtherProtection.debugTrace("[onPhysicsUpdate] Block is null");
			EtherProtection.debugTrace("[onPhysicsUpdate] Done");
			return;
		}else{
			EtherProtection.debugTrace("[onPhysicsUpdate] Block is not null");
		}
		//Check if is Protected type
		if(ProtectionManager.isProtectedType(event.getBlock())){
			EtherProtection.debugTrace("[onPhysicsUpdate] Block is a protected type");
		}else{
			EtherProtection.debugTrace("[onPhysicsUpdate] Block is not a protected type");
			EtherProtection.debugTrace("[onPhysicsUpdate] Done");
			return;
		}
		//Check owner
		UUID owner = ProtectionManager.getBlockOwner(event.getBlock());
		if(owner == null){
			EtherProtection.debugTrace("[onPhysicsUpdate] Block is not owned");
			EtherProtection.debugTrace("[onPhysicsUpdate] Done");
			return;
		}else{
			EtherProtection.debugTrace("[onPhysicsUpdate] Block is owned - Cancelling");
			event.setCancelled(true);
			EtherProtection.debugTrace("[onPhysicsUpdate] Done");
			return;
		}
	}
	@EventHandler (priority = EventPriority.HIGH)
	public void onPistonPush(BlockPistonExtendEvent event){
		EtherProtection.debugTrace("[onPistonPush] Begin");
		//Check cancelled
		if(event.isCancelled()){
			EtherProtection.debugTrace("[onPistonPush] Event is cancelled");
			EtherProtection.debugTrace("[onPistonPush] Done");
			return;
		}
		//Null Check
		List<Block> blocks = event.getBlocks();
		if(blocks == null){
			EtherProtection.debugTrace("[onPistonPush] Blocks are null");
			EtherProtection.debugTrace("[onPistonPush] Done");
			return;
		}
		//Check each block
		for(Block entry : blocks){
			if(ProtectionManager.isProtectedType(entry)){
				if(ProtectionManager.getBlockOwner(entry) == null){
					continue;
				}else{
					EtherProtection.debugTrace("[onPistonPush] Block has an owner - Cancelling event");
					EtherProtection.debugTrace("[onPistonPush] Done");
					event.setCancelled(true);
				}
			}else{
				//EtherProtection.debugTrace("[onPistonPush] Not a protected type");
			}
		}
	}
	@EventHandler (priority = EventPriority.MONITOR)
	public void onPistonPushCheck(BlockPistonExtendEvent event){
		EtherProtection.debugTrace("[onPistonPushCheck] Begin");
		if(event.isCancelled()){
			EtherProtection.debugTrace("[onPistonPushCheck] Event is cancelled");
		}else{
			EtherProtection.debugTrace("[onPistonPushCheck] Event is not cancelled - Deleting ownership of all blocks as a failsafe");
			for(Block entry : event.getBlocks()){
				ProtectionManager.deleteOwnership(entry);
			}
		}
		EtherProtection.debugTrace("[onPistonPushCheck] Done");
	}
	@EventHandler (priority = EventPriority.HIGH)
	public void onPistonPull(BlockPistonRetractEvent event){
		EtherProtection.debugTrace("[onPistonPull] Begin");
		if(event.isCancelled()){
			EtherProtection.debugTrace("[onPistonPull] Event is already cancelled");
			EtherProtection.debugTrace("[onPistonPull] Done");
			return;
		}
		//List<UUID> owners = new ArrayList<UUID>();
		Block pulledBlock = event.getRetractLocation().getBlock();
		//BlockFace dir = event.getDirection().getOppositeFace();
		//Block modifiedBlock = destinationBlock.getRelative(dir);
		//owners.add(ProtectionManager.getBlockOwner(modifiedBlock));
		/*
		if(ProtectionManager.getBlockOwner(modifiedBlock) == null){
			EtherProtection.debugTrace("[onPistonPull] modifiedBlock is not owned");
			if(ProtectionManager.getBlockOwner(destinationBlock) == null){
				EtherProtection.debugTrace("[onPistonPull] destinationBlock is not owned");
			}else{
				EtherProtection.debugTrace("[onPistonPull] destinationBlock IS owned");
				event.setCancelled(true);
			}
		}else{
			EtherProtection.debugTrace("[onPistonPull] modifiedBlock IS owned");
			event.setCancelled(true);
		}
		*/
		//Check Block Type
		if(ProtectionManager.isProtectedType(pulledBlock)){
			EtherProtection.debugTrace("[onPistonPull] Is a protected type");
		}else{
			EtherProtection.debugTrace("[onPistonPull] Is not a protected type");
			EtherProtection.debugTrace("[onPistonPull] Done");
			return;
		}
		//Check Ownership
		if(ProtectionManager.getBlockOwner(pulledBlock) == null){
			EtherProtection.debugTrace("[onPistonPull] No protections found");
		}else{
			EtherProtection.debugTrace("[onPistonPull] Protections found - cancelling");
			event.setCancelled(true);
		}
		EtherProtection.debugTrace("[onPistonPull] Done");
	}
	@EventHandler (priority = EventPriority.MONITOR)
	public void onPistonPullCheck(BlockPistonRetractEvent event){
		EtherProtection.debugTrace("[onPistonPullCheck] Begin");
		if(event.isCancelled()){
			EtherProtection.debugTrace("[onPistonPullCheck] Event is cancelled");
		}else{
			if(event.isSticky()){
				EtherProtection.debugTrace("[onPistonPullCheck] Block is sticky; removing any protection from pulled block");
				Block destinationBlock = event.getRetractLocation().getBlock();
				//BlockFace dir = event.getDirection().getOppositeFace();
				//Block modifiedBlock = destinationBlock.getRelative(dir);
				//ProtectionManager.deleteOwnership(modifiedBlock);
				ProtectionManager.deleteOwnership(destinationBlock);
			}else{
				EtherProtection.debugTrace("[onPistonPullCheck] Block is not sticky");
			}
		}
		EtherProtection.debugTrace("[onPistonPullCheck] Done");
	}
	/*
	//Hopper protection removed, because container block type classes are ambiguous, and because Deadbolt/Lockette should take care of protection
	@EventHandler
	public void onHopperMove(InventoryMoveItemEvent event){
		EtherProtection.debugTrace("[onHopperMove] Begin");
		Inventory sourceInv = event.getSource();
		if(sourceInv == null){
			EtherProtection.debugTrace("[onHopperMove] Source Inventory is null");
			EtherProtection.debugTrace("[onHopperMove] Done");
			return;
		}else{
			EtherProtection.debugTrace("[onHopperMove] Source Inventory is not null");
		}
	}
	@EventHandler (priority = EventPriority.MONITOR)
	public void onPhysicsUpdateCheck(){
		//Removed, because a successful physics check does not mean a block was destroyed
	}

	*/
}
