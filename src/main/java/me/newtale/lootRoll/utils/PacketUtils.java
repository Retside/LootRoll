package me.newtale.lootRoll.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;


public class PacketUtils {

    public static void hideItemFromAllExcept(Item item, Player allowedPlayer, ProtocolManager protocolManager, JavaPlugin plugin) {
        try {
            PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);

            try {
                destroyPacket.getIntLists().write(0, List.of(item.getEntityId()));
            } catch (Exception e1) {
                try {
                    destroyPacket.getIntegerArrays().write(0, new int[]{item.getEntityId()});
                } catch (Exception e2) {
                    try {
                        destroyPacket.getIntegers().write(0, item.getEntityId());
                    } catch (Exception e3) {
                        plugin.getLogger().warning("Failed to create destroy packet for entity " + item.getEntityId() + ": " + e3.getMessage());
                        return;
                    }
                }
            }

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(allowedPlayer)) {
                    try {
                        protocolManager.sendServerPacket(onlinePlayer, destroyPacket);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to send destroy packet to player " + onlinePlayer.getName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hide item from other players: " + e.getMessage());
        }
    }

    public static void hideItemFromPlayers(Item item, List<Player> players, ProtocolManager protocolManager, JavaPlugin plugin) {
        if (item == null || !item.isValid()) return;

        try {
            PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);

            try {
                destroyPacket.getIntLists().write(0, Arrays.asList(item.getEntityId()));
            } catch (Exception e1) {
                try {
                    destroyPacket.getIntegerArrays().write(0, new int[]{item.getEntityId()});
                } catch (Exception e2) {
                    try {
                        destroyPacket.getIntegers().write(0, item.getEntityId());
                    } catch (Exception e3) {
                        plugin.getLogger().warning("Failed to create destroy packet for entity " + item.getEntityId() + ": " + e3.getMessage());
                        return;
                    }
                }
            }

            for (Player player : players) {
                if (player != null && player.isOnline()) {
                    try {
                        protocolManager.sendServerPacket(player, destroyPacket);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to send destroy packet to player " + player.getName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hide item from players: " + e.getMessage());
        }
    }

    public static void hideItemFromNonParticipants(Item item, List<Player> participants, ProtocolManager protocolManager, JavaPlugin plugin) {
        try {
            PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);

            try {
                destroyPacket.getIntLists().write(0, Arrays.asList(item.getEntityId()));
            } catch (Exception e1) {
                try {
                    destroyPacket.getIntegerArrays().write(0, new int[]{item.getEntityId()});
                } catch (Exception e2) {
                    try {
                        destroyPacket.getIntegers().write(0, item.getEntityId());
                    } catch (Exception e3) {
                        plugin.getLogger().warning("Failed to create destroy packet for entity " + item.getEntityId() + ": " + e3.getMessage());
                        return;
                    }
                }
            }

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!participants.contains(onlinePlayer)) {
                    try {
                        protocolManager.sendServerPacket(onlinePlayer, destroyPacket);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to send destroy packet to player " + onlinePlayer.getName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hide item from non-participants: " + e.getMessage());
        }
    }
}