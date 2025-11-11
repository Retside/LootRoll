package me.newtale.lootroll.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class PacketUtils {

	public static void hideItemFromAllExcept(Item item, Player allowedPlayer, JavaPlugin plugin) {
		try {
			int entityId = item.getEntityId();
			WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(new int[]{entityId});

			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if (!onlinePlayer.equals(allowedPlayer)) {
					try {
						PacketEvents.getAPI().getPlayerManager().sendPacket(onlinePlayer, destroy);
					} catch (Exception e) {
						plugin.getLogger().warning("Failed to send destroy packet to player " + onlinePlayer.getName() + ": " + e.getMessage());
					}
				}
			}
		} catch (Exception e) {
			plugin.getLogger().warning("Failed to hide item from other players: " + e.getMessage());
		}
	}

	public static void hideItemFromPlayers(Item item, List<Player> players, JavaPlugin plugin) {
		if (item == null || !item.isValid()) return;

		try {
			int entityId = item.getEntityId();
			WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(new int[]{entityId});

			for (Player player : players) {
				if (player != null && player.isOnline()) {
					try {
						PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroy);
					} catch (Exception e) {
						plugin.getLogger().warning("Failed to send destroy packet to player " + player.getName() + ": " + e.getMessage());
					}
				}
			}
		} catch (Exception e) {
			plugin.getLogger().warning("Failed to hide item from players: " + e.getMessage());
		}
	}

	public static void hideItemFromNonParticipants(Item item, List<Player> participants, JavaPlugin plugin) {
		try {
			int entityId = item.getEntityId();
			WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(new int[]{entityId});

			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if (!participants.contains(onlinePlayer)) {
					try {
						PacketEvents.getAPI().getPlayerManager().sendPacket(onlinePlayer, destroy);
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