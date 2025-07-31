package me.newtale.lootRoll.models;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RollSession {
    private final String id;
    private final ItemStack item;
    private final List<Player> participants;
    private final Location location;
    private final Item droppedItem;
    private final Map<Player, Integer> rolls; // Regular rolls
    private final Map<Player, Integer> fallbackRolls; // Fallback rolls (/rollf)
    private final Set<Player> cancelledPlayers; // Players who cancelled their roll
    private Player winner;

    public RollSession(String id, ItemStack item, List<Player> participants, Location location, Item droppedItem) {
        this.id = id;
        this.item = item;
        this.participants = participants;
        this.location = location;
        this.droppedItem = droppedItem;
        this.rolls = new HashMap<>();
        this.fallbackRolls = new HashMap<>();
        this.cancelledPlayers = new HashSet<>();
        this.winner = null;
    }

    public String getId() {
        return id;
    }

    public ItemStack getItem() {
        return item;
    }

    public List<Player> getParticipants() {
        return participants;
    }

    public Location getLocation() {
        return location;
    }

    public Item getDroppedItem() {
        return droppedItem;
    }

    public Map<Player, Integer> getRolls() {
        return rolls;
    }

    public Map<Player, Integer> getFallbackRolls() {
        return fallbackRolls;
    }

    public Set<Player> getCancelledPlayers() {
        return cancelledPlayers;
    }

    public Player getWinner() {
        return winner;
    }

    public void addRoll(Player player, int roll) {
        rolls.put(player, roll);
        fallbackRolls.remove(player);
        cancelledPlayers.remove(player);
    }

    public void addFallbackRoll(Player player, int roll) {
        if (!rolls.containsKey(player)) {
            fallbackRolls.put(player, roll);
            cancelledPlayers.remove(player);
        }
    }

    public void cancelRoll(Player player) {
        rolls.remove(player);
        fallbackRolls.remove(player);
        cancelledPlayers.add(player);
    }

    public boolean hasPlayerRolled(Player player) {
        return rolls.containsKey(player);
    }

    public boolean hasPlayerFallbackRolled(Player player) {
        return fallbackRolls.containsKey(player);
    }

    public boolean hasPlayerRolledAny(Player player) {
        return hasPlayerRolled(player) || hasPlayerFallbackRolled(player);
    }

    public boolean hasPlayerCancelled(Player player) {
        return cancelledPlayers.contains(player);
    }

    public boolean hasPlayerParticipated(Player player) {
        return hasPlayerRolledAny(player) || hasPlayerCancelled(player);
    }

    public boolean haveAllPlayersParticipated() {
        return participants.stream().allMatch(this::hasPlayerParticipated);
    }

    public boolean haveAllPlayersCancelled() {
        return participants.stream().allMatch(this::hasPlayerCancelled);
    }

    public void setWinner(Player winner) {
        this.winner = winner;
    }

    public boolean isWinner(Player player) {
        return winner != null && winner.equals(player);
    }

    public Map<Player, Integer> getEffectiveRolls() {
        if (!rolls.isEmpty()) {
            return rolls;
        }
        return fallbackRolls;
    }

    public Map<Player, Integer> getAllRolls() {
        Map<Player, Integer> allRolls = new HashMap<>();
        allRolls.putAll(fallbackRolls);
        allRolls.putAll(rolls); // Regular rolls override fallback rolls
        return allRolls;
    }
}