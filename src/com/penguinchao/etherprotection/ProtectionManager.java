package com.penguinchao.etherprotection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class ProtectionManager {
	private static EtherProtection main;
	private static Connection connection;
	
	public static void EstablishConnection(EtherProtection passedMain){
		EtherProtection.debugTrace("[EstablishConnection] Begin");
		main = passedMain;
		String mysqlHostName= main.getConfig().getString("mysqlHostName");
		String mysqlPort	= main.getConfig().getString("mysqlPort");
		String mysqlUsername= main.getConfig().getString("mysqlUsername");
		String mysqlPassword= main.getConfig().getString("mysqlPassword");
		String mysqlDatabase= main.getConfig().getString("mysqlDatabase");
		String dburl = "jdbc:mysql://" + mysqlHostName + ":" + mysqlPort + "/" + mysqlDatabase;
		main.getLogger().info("Connecting to Database");
		try{
			connection = DriverManager.getConnection(dburl, mysqlUsername, mysqlPassword);
			checkTables();
		}catch(Exception exception){
			main.getLogger().info("[ERROR] Could not connect to the database -- disabling EtherProtection");
			exception.printStackTrace();
			Bukkit.getPluginManager().disablePlugin(main);
		}
		EtherProtection.debugTrace("[EstablishConnection] Done");
	}
	private static void checkTables() throws SQLException{
		EtherProtection.debugTrace("[checkTables] Begin");
		String query = "CREATE TABLE IF NOT EXISTS `"+main.getConfig().getString("mysqlPrefix")+"savedprotections` ( `player_uuid` VARCHAR(36) NOT NULL , `protectiontype` VARCHAR(50) NOT NULL , `x` INT NOT NULL , `y` INT NOT NULL , `z` INT NOT NULL , `world` VARCHAR(150) NOT NULL ) ENGINE = InnoDB; ";
		PreparedStatement sql = connection.prepareStatement(query);
		sql.executeUpdate();
		EtherProtection.debugTrace("[checkTables] Done");
	}
	public static boolean isEnabled(){
		EtherProtection.debugTrace("[isEnabled] Begin");
		//Used by other plugins to check if plugin is enabled
		if(main == null){
			EtherProtection.debugTrace("[isEnabled] Done");
			return false;
		}else if(connection == null){
			EtherProtection.debugTrace("[isEnabled] Done");
			return false;
		}else{
			EtherProtection.debugTrace("[isEnabled] Done");
			return true;
		}
	}
	public static UUID getBlockOwner(Block block){
		return getBlockOwner(block, true);
	}
	private static UUID getBlockOwner(Block block, boolean isFirst){
		if(isFirst){
			EtherProtection.debugTrace("[getBlockOwner] Begin First");
		}else{
			EtherProtection.debugTrace("[getBlockOwner] Begin Second");
		}
		if(block == null){
			EtherProtection.debugTrace("[getBlockOwner] Done - Null");
			return null;
		}
		String query = "SELECT `player_uuid` FROM `"+main.getConfig().getString("mysqlPrefix")+"savedprotections` WHERE (`protectiontype` = 'public' OR `protectiontype` = 'private') AND `x` = '"+block.getX()+"' AND `y` = '"+block.getY()+"' AND `z` = '"+block.getZ()+"' AND `world` = '"+block.getWorld().getName()+"'; ";
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			ResultSet results = sql.executeQuery();
			if(results.next()){
				EtherProtection.debugTrace("[getBlockOwner] Results found. Returning UUID");
				EtherProtection.debugTrace("[getBlockOwner] Done");
				UUID returnMe = UUID.fromString(results.getString("player_uuid"));
				return returnMe;
			}else{
				if(isFirst){
					EtherProtection.debugTrace("[getBlockOwner] Not found on first pass");
					return getBlockOwner(getOtherDoorBlock(block), false);
				}else{
					EtherProtection.debugTrace("[getBlockOwner] Not Found on second pass");
					EtherProtection.debugTrace("[getBlockOwner] Done - Null");
					return null;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			EtherProtection.debugTrace("[getBlockOwner] SQLException");
			EtherProtection.debugTrace("[getBlockOwner] Done - Null");
			return null;
		}
	}
	public static boolean setOwnership(Block block, UUID player, boolean isPublic){
		EtherProtection.debugTrace("[setOwnership] Begin");
		if(block == null){
			return false;
		}
		if(player == null){
			return false;
		}
		//attempts to set block ownership.
		deleteOwnership(block, true);
		String query = "INSERT INTO `"+main.getConfig().getString("mysqlPrefix")+"savedprotections` (`player_uuid`, `protectiontype`, `x`, `y`, `z`, `world`) VALUES ('"+player.toString()+"', '";
		if(isPublic){
			query = query+"public', '"+block.getX()+"', '"+block.getY()+"', '"+block.getZ()+"', '"+block.getWorld().getName()+"') ";
		}else{
			query = query+"private', '"+block.getX()+"', '"+block.getY()+"', '"+block.getZ()+"', '"+block.getWorld().getName()+"') ";
		}
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			sql.executeUpdate();
			EtherProtection.debugTrace("[setOwnership] Done");
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			EtherProtection.debugTrace("[setOwnership] Done");
			return false;
		}
		
	}
	public static void deleteOwnership(Block block){
		deleteOwnership(block, true);
	}
	private static void deleteOwnership(Block block, boolean isFirst){
		if(isFirst){
			EtherProtection.debugTrace("[deleteOwnership] Begin First");
		}else{
			EtherProtection.debugTrace("[deleteOwnership] Begin Second");
		}
		if(block == null){
			return;
		}
		//Deletes all References to protected block
		String query = "DELETE FROM `"+main.getConfig().getString("mysqlPrefix")+"savedprotections` WHERE `x` = '"+block.getX()+"' AND `y` = '"+block.getY()+"' AND `z` = '"+block.getZ()+"' AND `world` = '"+block.getWorld().getName()+"';";
		PreparedStatement sql;
		try {
			sql = connection.prepareStatement(query);
			sql.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if(isFirst){
			EtherProtection.debugTrace("[deleteOwnership] Getting other block");
			deleteOwnership(getOtherDoorBlock(block), false);
			EtherProtection.debugTrace("[deleteOwnership] Done First");
		}else{
			EtherProtection.debugTrace("[deleteOwnership] Done Second");
		}
	}
	@SuppressWarnings("deprecation")
	private static Block getOtherDoorBlock(Block block){
		//Null Check
		if(block == null){
			EtherProtection.debugTrace("[getOtherDoorBlock] Block is Null");
			EtherProtection.debugTrace("[getOtherDoorBlock] Done - Null");
			return null;
		}
		//Check if Door block
		boolean isDoor = false;
		List<String> doors = main.getConfig().getStringList("doorblocks");
		for(String entry : doors){
			Integer id = Integer.parseInt(entry);
			if(id.intValue() == block.getTypeId()){
				isDoor = true;
				break;
			}
		}
		if(!isDoor){
			EtherProtection.debugTrace("[getOtherDoorBlock] Not a door block");
			EtherProtection.debugTrace("[getOtherDoorBlock] Done - Null");
			return null;
		}else{
			EtherProtection.debugTrace("[getOtherDoorBlock] Is a door block");
		}
		//Check Above
		Location aboveLoc = new Location(block.getWorld(), block.getX(), block.getY() + 1, block.getZ());
		Block aboveBlock = aboveLoc.getBlock();
		if(aboveBlock == null){
			EtherProtection.debugTrace("[getOtherDoorBlock] Above block is null");
		}else{
			EtherProtection.debugTrace("[getOtherDoorBlock] Above block is not null");
			if(aboveBlock.getType() == block.getType()){
				EtherProtection.debugTrace("[getOtherDoorBlock] Above block is matching");
				EtherProtection.debugTrace("[getOtherDoorBlock] Done");
				return aboveBlock;
			}
		}
		//Check Below
		Location belowLoc = new Location(block.getWorld(), block.getX(), block.getY() - 1, block.getZ());
		Block belowBlock = belowLoc.getBlock();
		if(belowBlock == null){
			EtherProtection.debugTrace("[getOtherDoorBlock] Below block is null");
			EtherProtection.debugTrace("[getOtherDoorBlock] Done - Null");
			return null;
		}else{
			EtherProtection.debugTrace("[getOtherDoorBlock] Below block is not null");
			if(belowBlock.getType() == block.getType()){
				EtherProtection.debugTrace("[getOtherDoorBlock] Below block is matching");
				EtherProtection.debugTrace("[getOtherDoorBlock] Done");
				return belowBlock;
			}else{
				EtherProtection.debugTrace("[getOtherDoorBlock] No matching door blocks?");
				EtherProtection.debugTrace("[getOtherDoorBlock] Done - Null");
				return null;
			}
		}
	}
	public static boolean purgePlayer(UUID player){
		EtherProtection.debugTrace("[purgePlayer] Begin");
		if(player == null){
			EtherProtection.debugTrace("[purgePlayer] UUID is null");
			EtherProtection.debugTrace("[purgePlayer] Done");
			return false;
		}
		main.getLogger().info("Attempting to delete all records for "+player.toString());
		List<Block> allOwnedBlocks = getAllOwnedBlocks(player);
		EtherProtection.debugTrace("[purgePlayer] Deleting all owned blocks and any of their visitors");
		if(allOwnedBlocks == null){
			EtherProtection.debugTrace("[purgePlayer] Empty list of owned blocks");
		}else{
			EtherProtection.debugTrace("[purgePlayer] List of owned blocks is not empty");
			for(Block entry : allOwnedBlocks){
				deleteOwnership(entry);
			}
			EtherProtection.debugTrace("[purgePlayer] Finished deleting owned blocks");
		}
		EtherProtection.debugTrace("[purgePlayer] Deleting any visitor privileges");
		String query = "DELETE FROM `"+main.getConfig().getString("mysqlPrefix")+"savedprotections` WHERE `player_uuid` = '"+player.toString()+"';";
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			sql.executeUpdate();
		} catch (SQLException e) {
			EtherProtection.debugTrace("[purgePlayer] SQL Exception");
			e.printStackTrace();
			EtherProtection.debugTrace("[purgePlayer] Done");
			return false;
		}
		return true;
	}
	public static List<Block> getAllOwnedBlocks(UUID player){
		EtherProtection.debugTrace("[getAllOwnedBlocks] Begin");
		if(player == null){
			EtherProtection.debugTrace("[getAllOwnedBlocks] UUID is Null");
			EtherProtection.debugTrace("[getAllOwnedBlocks] Done - Null");
			return null;
		}
		String query = "SELECT * FROM `"+main.getConfig().getString("mysqlPrefix")+"savedprotections` WHERE `player_uuid` = '"+player.toString()+"' AND (`protectiontype` = 'private' OR `protectiontype` = 'public');";
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			ResultSet results = sql.executeQuery();
			List<Block> returnMe = new ArrayList<Block>();
			while(results.next()){
				World world = Bukkit.getWorld(results.getString("world"));
				if(world == null){
					continue;
				}
				Location loc = new Location(world, results.getInt("x"), results.getInt("y"), results.getInt("z"));
				Block addMe = loc.getBlock();
				if(addMe == null){
					continue;
				}else{
					returnMe.add(addMe);
				}
			}
			if(returnMe.size() < 1){
				EtherProtection.debugTrace("[getAllOwnedBlocks] Set is empty");
				EtherProtection.debugTrace("[getAllOwnedBlocks] Done - Null");
				return null;
			}
			return returnMe;
		} catch (SQLException e) {
			EtherProtection.debugTrace("[getAllOwnedBlocks] SQL Exception");
			e.printStackTrace();
			EtherProtection.debugTrace("[getAllOwnedBlocks] Done - Null");
			return null;
		}
	}
	public static boolean isProtectedType(Material material){
		EtherProtection.debugTrace("[isProtectedType] Begin");
		if(material == null){
			EtherProtection.debugTrace("[isProtectedType] Material is null");
			EtherProtection.debugTrace("[isProtectedType] Done");
			return false;
		}else{
			EtherProtection.debugTrace("[isProtectedType] Material is not null");
		}
		@SuppressWarnings("deprecation")
		int id = material.getId();
		//Check Blacklist IDs
		for(Integer entry : main.getConfig().getIntegerList("blacklist-id")){
			if(entry.intValue() == id){
				EtherProtection.debugTrace("[isProtectedType] is Blacklisted by ID");
				EtherProtection.debugTrace("[isProtectedType] Done - False");
				return false;
			}
		}
		EtherProtection.debugTrace("[isProtectedType] is not Blacklisted by ID");
		boolean onlyBlacklist = main.getConfig().getBoolean("only-blacklist");
		//Check Whitelist IDs
		if(!onlyBlacklist){
			for(Integer entry : main.getConfig().getIntegerList("whitelist-id")){
				if(entry.intValue() == id){
					EtherProtection.debugTrace("[isProtectedType] is Whitelisted by ID");
					EtherProtection.debugTrace("[isProtectedType] Done - True");
					return true;
				}
			}
		}
		EtherProtection.debugTrace("[isProtectedType] is not Whitelisted by ID");
		//Check Blacklist Strings
		for(String entry : main.getConfig().getStringList("blacklist-strings")){
			if(StringUtils.containsIgnoreCase(material.toString(), entry)){
				EtherProtection.debugTrace("[isProtectedType] is Blacklisted by String");
				EtherProtection.debugTrace("[isProtectedType] Done - False");
				return false;
			}
		}
		//Check Whitelist Strings
		if(!onlyBlacklist){
			for(String entry : main.getConfig().getStringList("whitelist-strings")){
				if(StringUtils.containsIgnoreCase(material.toString(), entry)){
					EtherProtection.debugTrace("[isProtectedType] is Whitelisted by String");
					EtherProtection.debugTrace("[isProtectedType] Done - True");
					return true;
				}
			}
			EtherProtection.debugTrace("[isProtectedType] is not Whitelisted by ID");
			EtherProtection.debugTrace("[isProtectedType] Done - False");
			return false;
		}
		EtherProtection.debugTrace("[isProtectedType] is not Whitelisted by ID");
		EtherProtection.debugTrace("[isProtectedType] Done - True");
		return true;
	}
	@SuppressWarnings("deprecation")
	public static boolean isProtectedType(int blockID){
		return isProtectedType(Material.getMaterial(blockID));
	}
	@SuppressWarnings("deprecation")
	public static boolean isProtectedType(Block block){
		EtherProtection.debugTrace("[isProtectedType] Begin");
		if(block == null){
			EtherProtection.debugTrace("[isProtectedType] Block is Null");
			EtherProtection.debugTrace("[isProtectedType] Done - False");
			return false;
		}
		Integer id = block.getTypeId();
		//Check Blacklist IDs
		for(Integer entry : main.getConfig().getIntegerList("blacklist-id")){
			if(entry.intValue() == id.intValue()){
				EtherProtection.debugTrace("[isProtectedType] is Blacklisted by ID");
				EtherProtection.debugTrace("[isProtectedType] Done - False");
				return false;
			}
		}
		EtherProtection.debugTrace("[isProtectedType] is not Blacklisted by ID");
		boolean onlyBlacklist = main.getConfig().getBoolean("only-blacklist");
		//Check Whitelist IDs
		if(!onlyBlacklist){
			for(Integer entry : main.getConfig().getIntegerList("whitelist-id")){
				if(entry.intValue() == id.intValue()){
					EtherProtection.debugTrace("[isProtectedType] is Whitelisted by ID");
					EtherProtection.debugTrace("[isProtectedType] Done - True");
					return true;
				}
			}
		}
		EtherProtection.debugTrace("[isProtectedType] is not Whitelisted by ID");
		//Check Blacklist Strings
		for(String entry : main.getConfig().getStringList("blacklist-strings")){
			if(StringUtils.containsIgnoreCase(block.getType().toString(), entry)){
				EtherProtection.debugTrace("[isProtectedType] is Blacklisted by String");
				EtherProtection.debugTrace("[isProtectedType] Done - False");
				return false;
			}
		}
		//Check Whitelist Strings
		if(!onlyBlacklist){
			for(String entry : main.getConfig().getStringList("whitelist-strings")){
				if(StringUtils.containsIgnoreCase(block.getType().toString(), entry)){
					EtherProtection.debugTrace("[isProtectedType] is Whitelisted by String");
					EtherProtection.debugTrace("[isProtectedType] Done - True");
					return true;
				}
			}
			EtherProtection.debugTrace("[isProtectedType] is not Whitelisted by ID");
			EtherProtection.debugTrace("[isProtectedType] Done - False");
			return false;
		}
		EtherProtection.debugTrace("[isProtectedType] is not Whitelisted by ID");
		EtherProtection.debugTrace("[isProtectedType] Done - True");
		return true;
	}
	public static boolean isPublic(Block block){
		EtherProtection.debugTrace("[isPublic] Begin");
		//Returns true if the protected block is allowed to be interacted with
		String query = "SELECT * FROM `"+main.getConfig().getString("mysqlPrefix")+"savedprotections` WHERE `protectiontype` = 'public' AND `x` = '"+block.getX()+"' AND `y` = '"+block.getY()+"' AND `z` = '"+block.getZ()+"' AND `world` = '"+block.getWorld().getName()+"' ";
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			ResultSet results = sql.executeQuery();
			if(results.next()){
				EtherProtection.debugTrace("[isPublic] Has Results");
				EtherProtection.debugTrace("[isPublic] Done - True");
				return true;
			}else{
				EtherProtection.debugTrace("[isPublic] Doesn't have results");
				EtherProtection.debugTrace("[isPublic] Done - False");
				return false;
			}
		} catch (SQLException e) {
			EtherProtection.debugTrace("[isPublic] SQL Exception");
			e.printStackTrace();
			EtherProtection.debugTrace("[isPublic] Done - False");
			return false;
		}
	}
	public static List<UUID> getVisitors(Block block){
		//Returns a list of players who are allowed to interact with private block
		String query = "SELECT `player_uuid` FROM `"+main.getConfig().getString("mysqlPrefix")+"savedprotections` WHERE `protectiontype` = 'visitor' AND `x` = '"+block.getX()+"' AND `y` = '"+block.getY()+"' AND `z` = '"+block.getZ()+"' AND `world` = '"+block.getWorld().getName()+"'; ";
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			ResultSet results = sql.executeQuery();
			List<UUID> returnMe = new ArrayList<UUID>();
			while(results.next()){
				returnMe.add(UUID.fromString(results.getString("player_uuid")));
			}
			if(returnMe.size() > 0){
				return returnMe;
			}else{
				return null;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static void addVisitor(Block block, UUID player){
		EtherProtection.debugTrace("[addVisitor] Begin");
		String query = "INSERT INTO `"+main.getConfig().getString("mysqlPrefix")+"savedprotections` (`player_uuid`, `protectiontype`, `x`, `y`, `z`, `world`) VALUES ('"+player.toString()+"', 'visitor', '"+block.getX()+"', '"+block.getY()+"', '"+block.getZ()+"', '"+block.getWorld().getName()+"');";
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			sql.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		EtherProtection.debugTrace("[addVisitor] Done");
	}
	public static void removeVisitor(Block block, UUID player){
		EtherProtection.debugTrace("[removeVisitor] Begin");
		String query = "DELETE FROM `"+main.getConfig().getString("mysqlPrefix")+"savedprotections` WHERE `player_uuid` = '"+player.toString()+"' AND `protectiontype` = 'visitor' AND `x` = '"+block.getX()+"' AND `y` = '"+block.getY()+"' AND `z` = '"+block.getZ()+"' AND `world` = '"+block.getWorld().getName()+"';";
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			sql.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		EtherProtection.debugTrace("[removeVisitor] Done");
	}
	public static boolean isVisitor(Block block, UUID player){
		EtherProtection.debugTrace("[isVisitor] Begin");
		List<UUID> visitors = getVisitors(block);
		if(visitors == null){
			EtherProtection.debugTrace("[isVisitor] Visitors is null");
			EtherProtection.debugTrace("[isVisitor] Done - False");
			return false;
		}else{
			boolean returnMe = visitors.contains(player);
			if(returnMe){
				EtherProtection.debugTrace("[isVisitor] Player is contained");
				EtherProtection.debugTrace("[isVisitor] Done - True");
			}else{
				EtherProtection.debugTrace("[isVisitor] Player is not contained");
				EtherProtection.debugTrace("[isVisitor] Done - False");
			}
			return returnMe;
		}
	}
}
