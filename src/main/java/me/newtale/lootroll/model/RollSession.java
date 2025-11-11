package me.newtale.lootroll.model;

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
    private final Item droppedItem; // null for exp/money loot
    private final Map<Player, Integer> rolls; // Regular rolls
    private final Map<Player, Integer> greedRolls; // Greed rolls (/greed)
    private final Set<Player> cancelledPlayers; // Players who cancelled their roll
    private Player winner;

    public RollSession(String id, ItemStack item, List<Player> participants, Location location, Item droppedItem) {
        this.id = id;
        this.item = item;
        this.participants = participants;
        this.location = location;
        this.droppedItem = droppedItem;
        this.rolls = new HashMap<>();
        this.greedRolls = new HashMap<>();
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

    public Set<Player> getCancelledPlayers() {
        return cancelledPlayers;
    }

    public Player getWinner() {
        return winner;
    }

    public void addRoll(Player player, int roll) {
        rolls.put(player, roll);
        greedRolls.remove(player);
        cancelledPlayers.remove(player);
    }

    public void addGreedRoll(Player player, int roll) {
        if (!rolls.containsKey(player)) {
            greedRolls.put(player, roll);
            cancelledPlayers.remove(player);
        }
    }

    public void cancelRoll(Player player) {
        rolls.remove(player);
        greedRolls.remove(player);
        cancelledPlayers.add(player);
    }

    public boolean hasPlayerRolled(Player player) {
        return rolls.containsKey(player);
    }

    public boolean hasPlayerGreedRolled(Player player) {
        return greedRolls.containsKey(player);
    }

    public boolean hasPlayerRolledAny(Player player) {
        return hasPlayerRolled(player) || hasPlayerGreedRolled(player);
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
        return greedRolls;
    }

    public Map<Player, Integer> getAllRolls() {
        Map<Player, Integer> allRolls = new HashMap<>();
        allRolls.putAll(greedRolls);
        allRolls.putAll(rolls); // Need rolls override greed rolls
        return allRolls;
    }

    public Map<Player, Integer> getGreedRolls() {
        return greedRolls;
    }
}