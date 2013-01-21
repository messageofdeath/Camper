package com.cheesium.Camper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;

public class Camper extends JavaPlugin implements Listener {
	
	public final Logger logger = Logger.getLogger("Minecraft");
	public File file;
	public static FileConfiguration db;
	
	@Override
	public void onEnable() {
		PluginDescriptionFile pdfFile = getDescription();
		this.logger.info("[" + pdfFile.getName() + "] v" + pdfFile.getVersion() + " has been enabled.");
		this.file = new File(getDataFolder(), "database.yml");
		if (!this.file.exists()) {
			this.file.getParentFile().mkdirs();
			try {
				this.file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			copy(getResource("database.yml"), this.file);
		}
		getServer().getPluginManager().registerEvents(this, this);
		guard = getWorldGuard();
		db = new YamlConfiguration();
		loadYamls();
	}
	
	private WorldGuardPlugin getWorldGuard() {
	    Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
	 
	    // WorldGuard may not be loaded
	    if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
	        Bukkit.getServer().shutdown();
	    }
	 
	    return (WorldGuardPlugin) plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player)sender;
			if(cmd.getName().equalsIgnoreCase("camper")) {
				if(player.hasPermission("camper.op")) {
					if(args.length == 1) {
						String tplayer = args[0];
						runCamper(player, tplayer);
					}else{
						player.sendMessage(ChatColor.DARK_RED + "Use /camper <name>");
					}
				}else{
					player.sendMessage(ChatColor.DARK_RED + "You do not have permission for this command");
				}
			}
		}else{
			sender.sendMessage(ChatColor.DARK_RED + "This command can only be used in-game");
		}
		return false;
	}
	
	public void runCamper(CommandSender player, String tplayer) {
		if(db.getString(tplayer) != null) {
			final int times = db.getInt(tplayer);
			if(times == 1) {
				db.set(tplayer, 2);
				saveYamls();
				if(Bukkit.getPlayer(tplayer) != null) {
					Bukkit.getPlayer(tplayer).getWorld().strikeLightning(Bukkit.getPlayer(tplayer).getLocation());
					Bukkit.getPlayer(tplayer).getWorld().createExplosion(Bukkit.getPlayer(tplayer).getLocation(), 0);
				}
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tempban " + tplayer + " 1d");
				Bukkit.broadcastMessage(ChatColor.GOLD + "[Cheesium] " + ChatColor.DARK_GREEN + tplayer + ChatColor.DARK_RED + " was tempbanned for camping. 1 day");
				player.sendMessage(ChatColor.DARK_RED + "[Camper] " + ChatColor.GOLD + tplayer + " was tempbanned for a day!");
			}
			if(times == 2) {
				if(Bukkit.getPlayer(tplayer) != null) {
					Bukkit.getPlayer(tplayer).getWorld().strikeLightning(Bukkit.getPlayer(tplayer).getLocation());
					Bukkit.getPlayer(tplayer).getWorld().createExplosion(Bukkit.getPlayer(tplayer).getLocation(), 0);
				}
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + tplayer + " camping");
				Bukkit.broadcastMessage(ChatColor.GOLD + "[Cheesium] " + ChatColor.DARK_GREEN + tplayer + ChatColor.DARK_RED + " was banned for camping.");
				player.sendMessage(ChatColor.DARK_RED + "[Camper] " + ChatColor.GOLD + tplayer + " was banned!");
			}
		}else{
			db.set(tplayer, 1);
			saveYamls();
			if(Bukkit.getPlayer(tplayer) != null) {
				Bukkit.getPlayer(tplayer).getWorld().strikeLightning(Bukkit.getPlayer(tplayer).getLocation());
				Bukkit.getPlayer(tplayer).getWorld().createExplosion(Bukkit.getPlayer(tplayer).getLocation(), 0);
			}
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tempban " + tplayer + " 1h");
			Bukkit.broadcastMessage(ChatColor.GOLD + "[Cheesium] " + ChatColor.DARK_GREEN + tplayer + ChatColor.DARK_RED + " was tempbanned for camping. 1 hour");
			player.sendMessage(ChatColor.DARK_RED + "[Camper] " + ChatColor.GOLD + tplayer + " was tempbanned for an hour!");
		}
	}
	
	public ArrayList<String> campers1 = new ArrayList<String>(), gothit = new ArrayList<String>(), banned = new ArrayList<String>();
	public WorldGuardPlugin guard;
	
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		if(event.isCancelled())return;
		if(gothit.contains(player.getName())) {
			return;
		}
		if(campers1.contains(player.getName())) {
			RegionManager rg = guard.getRegionManager(Bukkit.getWorld("world"));
			if(getRegion(rg, "etowerupper", player.getLocation())) {
				if(!banned.contains(player.getName())) {
					runCamper(Bukkit.getConsoleSender(), player.getName());
					banned.add(player.getName());
					campers1.remove(player.getName());
					gothit.remove(player.getName());
					Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						@Override
						public void run() {
							banned.remove(player.getName());
						}
					}, 20L * 2);
				}
			}
		}
	}
	
	HashMap<String, ArrayList<String>> attacks =  new HashMap<String, ArrayList<String>>();
	public void removeDeadPlayer(String name2) {
		for(ArrayList<String> array : attacks.values()) {
			for(String name3 : array) {
				if(name3.equalsIgnoreCase(name2)) {
					array.remove(name3);
				}
			}
		}
		return;
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		
	}
	
	@EventHandler
	public void onHit(EntityDamageByEntityEvent event) {
		if(event.isCancelled() || guard == null)return;
		if(event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
			WorldGuardPlugin worldguard = getWorldGuard();
			RegionManager region = worldguard.getRegionManager(Bukkit.getWorld("world"));
			if(getRegion(region, "lowere", event.getDamager().getLocation()) == true) {
				final Player damager = (Player)event.getDamager();
				final Player entity = (Player)event.getEntity();
				if(!gothit.contains(entity.getName())) {
					gothit.add(entity.getName());
					Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						@Override
						public void run() {
							gothit.remove(damager.getName());
						}
					}, 20L * 4);
				}
				if(!campers1.contains(damager.getName())) {
					campers1.add(damager.getName());
					Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						@Override
						public void run() {
							campers1.remove(damager.getName());
						}
					}, 20L * 10);
				}
			}
		}
	}
	
	public boolean getRegion(RegionManager region, String regions, Location loc) {
		if(region.getRegion(regions).contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
			return true;
		}
		return false;
	}

	
	//lowere
	
	private void copy(InputStream in, File file) {
		try {
			OutputStream out = new FileOutputStream(file);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void loadYamls() {
		try {
			db.load(this.file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void saveYamls() {
		try {
			db.save(this.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
