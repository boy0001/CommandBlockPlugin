package com.boydti.cbp;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.boydti.cbs.CommandProcessor;
import com.intellectualcrafters.configuration.file.YamlConfiguration;
import com.intellectualcrafters.plot.util.StringMan;

public class Main extends JavaPlugin implements Listener {
    
    private static Main plugin;
    
    public static int RATE_LIMIT = 16384;

    public static Main get() {
        return plugin;
    }
    
    public static void debug(String d) {
        System.out.println(d);
    }
    
    @Override
    public void onEnable() {
        Main.plugin = this;
        debug("--- Command Block ---");
        debug("Loading configuration");
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            saveDefaultConfig();
        }
        
        HashMap<String, Object> options = new HashMap<String, Object>();
        YamlConfiguration settings = YamlConfiguration.loadConfiguration(file);
        
        options.put("rate-limit", RATE_LIMIT);

        boolean changed = false;
        for (Entry<String, Object> entry : options.entrySet()) {
            if (!settings.contains(entry.getKey())) {
                settings.set(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        if (changed) {
            try {
                settings.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        RATE_LIMIT = settings.getInt("rate-limit");
        
        HashSet<String> enabled = new HashSet<>(settings.getStringList("allowed"));
        debug("Injecting CommandProcessor for PlotSquared");
        CommandProcessor.manager = new PSProcessor(enabled);
        debug("Registering events");
        Bukkit.getPluginManager().registerEvents(this, this);
    }
    
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage();
        Player player = event.getPlayer();
        if (!cmd.toLowerCase().startsWith("/setcommand") || !player.hasPermission("commandblock.use") && !player.isOp()) {
            return;
        }
        event.setCancelled(true);
        HashSet<Material> transparent = new HashSet<Material>(Arrays.asList(Material.values()));
        transparent.remove(Material.COMMAND);
        transparent.remove(Material.COMMAND_CHAIN);
        transparent.remove(Material.COMMAND_MINECART);
        transparent.remove(Material.COMMAND_REPEATING);
        Block target = player.getTargetBlock(transparent, 50);
        if (target == null || target.getType() != Material.COMMAND || !hasPermission(player, target.getLocation())) {
            player.sendMessage("Invalid target!");
            return;
        }
        CommandBlock cb = (CommandBlock) target.getState();
        cmd = cmd.substring(12);
        String[] split = cmd.split(" ");
        if (split.length > 0) {
            if (!split[0].contains(":")) {
                split[0] = "minecraft:" + (split[0].startsWith("/") ? split[0].substring(1) : split[0]);
            }
            cmd = StringMan.join(split, " ");
        }
        System.out.println("SET COMMAND: ");
        cb.setCommand(cmd);
        cb.update(true);
        player.sendMessage("Successfully set command to " + cmd + " at " + cb.getX() + "," + cb.getY() + "," + cb.getZ());
        return;
    }
    
    public static boolean hasPermission(Player player, Location loc) {
        Block block = loc.getBlock();
        BlockBreakEvent call = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(call);
        return !call.isCancelled();
    }
}
