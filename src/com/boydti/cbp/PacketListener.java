package com.boydti.cbp;


import io.netty.buffer.ByteBuf;

import java.lang.reflect.Method;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.intellectualcrafters.plot.util.StringMan;

public class PacketListener {
    public PacketListener() {
        final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        
        manager.addPacketListener(new PacketAdapter(Main.get(), ListenerPriority.NORMAL, PacketType.Play.Client.CUSTOM_PAYLOAD) {
            @Override
            public void onPacketReceiving(final PacketEvent event) {
                
                try {
                    Object payload = event.getPacket().getHandle();
                    Class<? extends Object> classPayload = payload.getClass();
                    Method methodGetSerializer = classPayload.getMethod("b");
                    ByteBuf serializer = (ByteBuf) methodGetSerializer.invoke(payload);
                    Method methodGetTag = classPayload.getMethod("a");

                    final Player player = event.getPlayer();
                    if (player.isOp() || !StringMan.isEqual((String) methodGetTag.invoke(payload), "MC|AdvCdm")) {
                        return;
                    }
                    // cancel all changes in case of error
                    event.setCancelled(true);
                    if (!player.hasPermission("commandblock.use")) {
                        player.sendMessage("Lacking permission: commandblock.use");
                        return;
                    }
                    // received packet
                    if (player.getGameMode() != GameMode.CREATIVE) {
                        player.sendMessage("You must be in creative mode to set this command block");
                        return;
                    }
                    try {
                        byte b = serializer.readByte();
                        if (b == 0) {
                            World world = player.getWorld();
                            final Location loc = new Location(world, serializer.readInt(), serializer.readInt(), serializer.readInt());
                            Method methodGetCommand = serializer.getClass().getMethod("c", int.class);
                            final String command = (String) methodGetCommand.invoke(serializer, serializer.readableBytes());
                            TaskManager.task(new Runnable() {
                                @Override
                                public void run() {
                                    Block block = loc.getBlock();
                                    if (block != null) {
                                        if (!Main.hasPermission(player, loc)) {
                                            player.sendMessage("You don't have permission for that area.");
                                        }
                                        BlockState state = block.getState();
                                        if (state != null && state instanceof CommandBlock) {
                                            CommandBlock cmdblock = (CommandBlock) state;
                                            String cmd = command;
                                            String[] split = cmd.split(" ");
                                            if (split.length > 0) {
                                                if (!split[0].contains(":")) {
                                                    split[0] = "minecraft:" + (split[0].startsWith("/") ? split[0].substring(1) : split[0]);
                                                }
                                                cmd = StringMan.join(split, " ");
                                            }
                                            cmdblock.setCommand(cmd);
                                            cmdblock.update();
                                            player.sendMessage("Successfully set command to " + cmd + " at " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
                                            return;
                                        }
                                    }
                                    player.sendMessage("Invalid command block position.");
                                }
                            });
                        } else {
                            player.sendMessage("You are not allowed to set command block data for entities.");
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        player.sendMessage("Invalid packet.");
                    } finally {
                        serializer.release();
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        });
    }
}
