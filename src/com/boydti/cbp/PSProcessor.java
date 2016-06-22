package com.boydti.cbp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.boydti.cbs.CommandProcessor;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotMessage;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RegionWrapper;
import com.plotsquared.bukkit.util.BukkitUtil;

public class PSProcessor extends CommandProcessor {
    
    private final HashSet<String> enabled;
    
    private final HashMap<Plot, Integer> limiter = new HashMap<>();
    
    private final HashMap<Plot, HashMap<String, Object[]>> messages = new HashMap<>();

    public PSProcessor(HashSet<String> enabled) {
        this.enabled = enabled;
        TaskManager.taskRepeat(new Runnable() {
            @Override
            public void run() {
                limiter.clear();
            }
        }, 1200);
        
        TaskManager.taskRepeat(new Runnable() {
            @Override
            public void run() {
                for (Entry<Plot, HashMap<String, Object[]>> entry : messages.entrySet()) {
                    for (Entry<String, Object[]> entry2 : entry.getValue().entrySet()) {
                        sendMessage(entry.getKey(), entry2.getValue());
                    }
                }
                messages.clear();
            }
        }, 213);
    }
    
    @Override
    public boolean isLocationAllowed(String world, int x, int y, int z) {
        if (lastRegion == null) {
            return false;
        }
        return checkRegion(x, y, z) && checkPlot();
    }
    
    private HashSet<RegionWrapper> lastRegion;
    private Plot lastPlot;
    private Block lastBlock;
    private Location lastLoc;
    private String lastCmd;
    
    @Override
    public boolean setOther(Object listener) {
        PS.debug("Listener: " + listener);
        PS.debug(" -> " + listener.getClass());
        return false;
    }
    

    public boolean checkRegion(int x, int y, int z) {
        for (RegionWrapper region : lastRegion) {
            if (region.isIn(x, y, z)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean setConsole(Class<?> clazz, String command) {
        lastCmd = null;
        return true;
    }

    @Override
    public boolean setLocation(org.bukkit.Location loc, Class<?> clazz, String command) {
        boolean result = enabled.contains(clazz.getSimpleName());
        if (!result) {
            handleError("NoPerm", "No permission");
            return false;
        }
        lastBlock = null;
        lastLoc = BukkitUtil.getLocation(loc);
        if (lastRegion != null && checkRegion(lastLoc.getX(), lastLoc.getY(), lastLoc.getZ())) {
            lastCmd = command;
            return checkPlot();
        }
        Plot plot = lastLoc.getOwnedPlot();
        if (plot == null) {
            lastRegion = null;
            return false;
        }
        lastRegion = plot.getRegions();
        lastPlot = plot;
        lastCmd = command;
        return checkPlot();
    }

    @Override
    public boolean setPlayer(Player arg0, Class<?> arg1, String command) {
        lastCmd = null;
        return true;
    }
    
    public boolean checkPlot() {
        if (lastPlot == null) {
            return false;
        }
        Integer limit = limiter.get(lastPlot);
        if (limit == null) {
            limit = 0;
        }
        limiter.put(lastPlot, limit + 1);
        if (limit <= Main.RATE_LIMIT) {
            return true;
        }
        handleError("RateLimit", "Rate limit reached (" + Main.RATE_LIMIT + "/min)");
        return false;
//        return limit < 16384;
    }

    @Override
    public boolean setCommandBlock(Block block, Class<?> clazz, String command) {
        boolean result = enabled.contains(clazz.getSimpleName());
        if (!result) {
            PS.debug("Not allowed class: " + clazz.getSimpleName() + " | " + enabled);
            handleError("NoPerm", "No permission");
            return false;
        }
        if (!block.equals(lastBlock)) {
            lastBlock = block;
            lastLoc = BukkitUtil.getLocation(lastBlock.getLocation());
            if (lastRegion != null && checkRegion(lastLoc.getX(), lastLoc.getY(), lastLoc.getZ())) {
                lastCmd = command;
                return checkPlot();
            }
            Plot plot = lastLoc.getOwnedPlot();
            if (plot == null) {
                lastRegion = null;
                return false;
            }
            lastRegion = plot.getRegions();
            lastPlot = plot;
        }
        lastCmd = command;
        return checkPlot();
    }
    
    public void sendMessage(Plot plot, Object[] data) {
        String message = (String) data[0];
        Location loc = (Location) data[1];
        String command = (String) data[2];
        
        List<PlotPlayer> players = plot.getPlayersInPlot();
        if (players.size() == 0) {
            return;
        }
        PlotMessage toSend = new PlotMessage();
        toSend
        .text("[")
        .color("&8")
        .text(loc.getX() + "," + loc.getY() + "," + loc.getZ())
        .color("&7")
        .tooltip("This is the command block having issues")
        .suggest("/minecraft:tp " + loc.getX() + " " + loc.getY() + " " + loc.getZ())
        .text("] ")
        .color("&8")
        .text(message)
        .tooltip(command)
        .text(" x" + data[3])
        .color("&c");
        for (PlotPlayer player : players) {
            if (plot.isAdded(player.getUUID())) {
                toSend.send(player);
            }
        }
    }

    private String lastMessage = null;
    
    @Override
    public void handleError(String id, String message) {
        if (message == lastMessage) {
            return;
        }
        if (lastPlot != null && lastLoc != null && lastCmd != null) {
            lastMessage = message;
            HashMap<String, Object[]> current = messages.get(lastPlot);
            if (current == null) {
                current = new HashMap<>();
                messages.put(lastPlot, current);
            }
            Object[] data = current.get(id);
            if (data == null) {
                current.put(id, new Object[] { message, lastLoc, lastCmd, 1 });
            }
            else {
                data[3] = (Integer) data[3] + 1;
            }
        }
    }
}
