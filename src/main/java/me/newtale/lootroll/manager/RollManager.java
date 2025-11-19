package me.newtale.lootroll.manager;

import me.newtale.lootroll.api.event.*;
import me.newtale.lootroll.model.RollSession;
import me.newtale.lootroll.util.*;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.experience.EXPSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import me.newtale.lootroll.util.ItemUtils;
import static me.newtale.lootroll.util.MessageUtils.processMessageWithVariables;
import static me.newtale.lootroll.util.PacketUtils.hideItemFromPlayers;

import java.util.*;

public class RollManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final PartyManager partyManager;
    private final ParticleEffectManager particleEffectManager;

    private final Map<String, RollSession> activeRolls;
    private final Map<Integer, String> clientSideItems;
    private final Map<Player, List<RollSession>> playerActiveSessions;
    private final Map<Integer, Player> itemOwnership;
    private final Map<Integer, List<Player>> unlockedItems;
    private final Map<String, BukkitTask> rollTimers;

    public RollManager(JavaPlugin plugin, ConfigManager configManager, PartyManager partyManager, ParticleEffectManager particleEffectManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.partyManager = partyManager;
        this.particleEffectManager = particleEffectManager;
        this.activeRolls = new HashMap<>();
        this.clientSideItems = new HashMap<>();
        this.playerActiveSessions = new HashMap<>();
        this.itemOwnership = new HashMap<>();
        this.unlockedItems = new HashMap<>();
        this.rollTimers = new HashMap<>();
    }

    public void startRollSession(ItemStack itemStack, List<Player> participants, Location location) {
        String sessionId = UUID.randomUUID().toString();

        // Don't create physical item for exp/money loot
        Item droppedItem = null;
        boolean isVirtualLoot = ItemUtils.isExpLoot(itemStack) || ItemUtils.isMoneyLoot(itemStack) || ItemUtils.isMmoExpLoot(itemStack);
        
        if (!isVirtualLoot) {
            droppedItem = RollUtils.createDroppedItem(location, itemStack);
        }

        RollSession session = new RollSession(sessionId, itemStack, participants, location, droppedItem);
        activeRolls.put(sessionId, session);
        
        if (droppedItem != null) {
            clientSideItems.put(droppedItem.getEntityId(), sessionId);
        }

        Bukkit.getPluginManager().callEvent(new RollSessionStartEvent(session));

        for (Player participant : participants) {
            playerActiveSessions.computeIfAbsent(participant, k -> new ArrayList<>()).add(session);
        }

        if (droppedItem != null) {
            PacketUtils.hideItemFromNonParticipants(droppedItem, participants, plugin);
            particleEffectManager.startItemAnimation(droppedItem, itemStack, participants);
        }
        
        sendLootMessages(session);
        startRollTimer(session);
    }

    public void dropItemForSoloPlayer(ItemStack itemStack, Player owner, Location location) {
        Item droppedItem = RollUtils.createDroppedItem(location, itemStack);

        itemOwnership.put(droppedItem.getEntityId(), owner);

        PacketUtils.hideItemFromAllExcept(droppedItem, owner, plugin);

        particleEffectManager.startItemAnimation(droppedItem, itemStack, Collections.singletonList(owner));
    }

    public boolean canPlayerPickupItem(Player player, int entityId) {
        if (clientSideItems.containsKey(entityId)) {
            return false;
        }

        if (unlockedItems.containsKey(entityId)) {
            List<Player> allowedPlayers = unlockedItems.get(entityId);
            return allowedPlayers.contains(player);
        }

        if (itemOwnership.containsKey(entityId)) {
            Player owner = itemOwnership.get(entityId);
            return player.equals(owner);
        }

        return true;
    }

    public void removeItemOwnership(int entityId) {
        itemOwnership.remove(entityId);
        unlockedItems.remove(entityId);
    }

    private void sendLootMessages(RollSession session) {
        String itemName = ItemUtils.getItemDisplayName(session.getItem(), configManager);
        String itemNameNoColors = ItemUtils.getItemDisplayNameWithoutColors(session.getItem(), configManager);
        String messageTemplate = configManager.getMessage("loot-dropped",
                "<gold>[Loot]</gold> <white><item></white> <gray>has dropped! Type <green>/roll</green> or <green>/roll <item_name></green> to roll for it!");

        for (Player player : session.getParticipants()) {
            Component message = MessageUtils.processMessageWithVariables(messageTemplate, itemName, itemNameNoColors, session.getItem(), configManager);
            player.sendMessage(message);
        }
    }

    private void startRollTimer(RollSession session) {
        int rollTime = configManager.getRollTime();

        BukkitTask task = new BukkitRunnable() {
            int timeLeft = rollTime;

            @Override
            public void run() {
                if (!activeRolls.containsKey(session.getId())) {
                    this.cancel();
                    rollTimers.remove(session.getId());
                    return;
                }

                if (session.haveAllPlayersParticipated()) {
                    finishRollSession(session.getId());
                    this.cancel();
                    rollTimers.remove(session.getId());
                    return;
                }

                if (timeLeft <= 0) {
                    finishRollSession(session.getId());
                    this.cancel();
                    rollTimers.remove(session.getId());
                    return;
                }

                if (timeLeft == 10) {
                    String itemName = ItemUtils.getItemDisplayName(session.getItem(), configManager);
                    String itemNameNoColors = ItemUtils.getItemDisplayNameWithoutColors(session.getItem(), configManager);
                    String countdownTemplate = configManager.getMessage("countdown",
                            "<gray>Rolling for <white><item></white> ends in <red><time></red> seconds!");

                    for (Player participant : session.getParticipants()) {
                        if (participant.isOnline()) {
                            Component countdownMessage = MessageUtils.createLootMessage(countdownTemplate, itemName, itemNameNoColors, session.getItem(), configManager,
                                    Placeholder.unparsed("time", String.valueOf(timeLeft)));
                            participant.sendMessage(countdownMessage);
                        }
                    }
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 20L, 20L);

        rollTimers.put(session.getId(), task);
    }

    public void handleRollCommand(Player player) {
        handleRollCommand(player, null, false);
    }

    public void handleRollCommand(Player player, String itemName) {
        handleRollCommand(player, itemName, false);
    }

    public void handleGreedRollCommand(Player player) {
        handleRollCommand(player, null, true);
    }

    public void handleGreedRollCommand(Player player, String itemName) {
        handleRollCommand(player, itemName, true);
    }

    public void handleRollCancelCommand(Player player) {
        handleRollCancelCommand(player, null);
    }

    public void handleRollCancelCommand(Player player, String itemName) {
        if (!partyManager.isPlayerInParty(player)) {
            String messageTemplate = configManager.getMessage("not-in-party",
                    "<red>You must be in a party to use the roll system!");
            Component message = processMessageWithVariables(messageTemplate, null, null, null);
            player.sendMessage(message);
            return;
        }

        List<RollSession> availableSessions = SessionHelper.getAvailableSessionsForSkip(player, playerActiveSessions, activeRolls);


        if (availableSessions.isEmpty()) {
            String messageTemplate = configManager.getMessage("no-sessions-to-skip",
                    "<red>No active rolls to skip!");
            Component message = processMessageWithVariables(messageTemplate, null, null, null);
            player.sendMessage(message);
            return;
        }

        RollSession targetSession = null;

        if (itemName != null && !itemName.trim().isEmpty()) {
            targetSession = SessionHelper.findSessionByItemName(availableSessions, itemName);
            if (targetSession == null) {
                String messageTemplate = configManager.getMessage("item-not-found",
                        "<red>Item '<yellow><item_name></yellow>' not found in your available rolls!");
                Component message = processMessageWithVariables(messageTemplate, null, itemName, null,
                        Placeholder.unparsed("item", itemName));
                player.sendMessage(message);

                showAvailableItemsToSkip(player, availableSessions);
                return;
            }
        } else {
            if (availableSessions.size() == 1) {
                targetSession = availableSessions.getFirst();
            } else {
                showAvailableItemsToSkip(player, availableSessions);
                return;
            }
        }

        RollPassEvent passEvent = new RollPassEvent(targetSession, player);
        Bukkit.getPluginManager().callEvent(passEvent);
        if (passEvent.isCancelled()) {
            return;
        }

        targetSession.cancelRoll(player);
        announceSkipRoll(targetSession, player);

        if (targetSession.haveAllPlayersParticipated()) {
            finishRollSession(targetSession.getId());
        }
    }

    private void showAvailableItemsToSkip(Player player, List<RollSession> sessions) {
        String messageTemplate = configManager.getMessage("available-items-to-skip",
                "<gray>Available items to skip:");
        Component message = processMessageWithVariables(messageTemplate, null, null, null);
        player.sendMessage(message);

        for (RollSession session : sessions) {
            String itemName = ItemUtils.getItemDisplayName(session.getItem(), configManager);
            String itemNameNoColors = ItemUtils.getItemDisplayNameWithoutColors(session.getItem(), configManager);
            String itemTemplate = configManager.getMessage("available-skip-item-format",
                    "<gray>- <white><item></white>");
            Component itemMessage = MessageUtils.createLootMessage(itemTemplate, itemName, itemNameNoColors, session.getItem(), configManager);
            player.sendMessage(itemMessage);
        }
    }

    private void announceSkipRoll(RollSession session, Player player) {
        String itemName = ItemUtils.getItemDisplayName(session.getItem(), configManager);
        String itemNameNoColors = ItemUtils.getItemDisplayNameWithoutColors(session.getItem(), configManager);

        String rollMessageTemplate = configManager.getMessage("player-skipped-roll",
                "<yellow><player></yellow> <gray>skipped rolling for <white><item></white>");

        for (Player participant : session.getParticipants()) {
            if (participant.isOnline()) {
                Component rollMessage = MessageUtils.createLootMessage(rollMessageTemplate, itemName, itemNameNoColors, session.getItem(), configManager,
                        Placeholder.unparsed("player", player.getName()));
                participant.sendMessage(rollMessage);
            }
        }
    }

    // Updated method to fetch sessions available to pass
    public List<RollSession> getSkippableSessionsForPlayer(Player player) {
        return SessionHelper.getAvailableSessionsForSkip(player, playerActiveSessions, activeRolls);
    }

    private void handleRollCommand(Player player, String itemName, boolean isGreedRoll) {
        if (!partyManager.isPlayerInParty(player)) {
            String messageTemplate = configManager.getMessage("not-in-party",
                    "<red>You must be in a party to use the roll system!");
            Component message = processMessageWithVariables(messageTemplate, null, null, null);
            player.sendMessage(message);
            return;
        }

        List<RollSession> availableSessions = SessionHelper.getAvailableSessionsForPlayer(player, playerActiveSessions, activeRolls, isGreedRoll);
        if (availableSessions.isEmpty()) {
            String messageKey = isGreedRoll ? "no-active-greed-roll" : "no-active-roll";
            String messageTemplate = configManager.getMessage(messageKey,
                    "<red>No active roll session found!");
            Component message = processMessageWithVariables(messageTemplate, null, null, null);
            player.sendMessage(message);
            return;
        }

        RollSession targetSession = null;

        if (itemName != null && !itemName.trim().isEmpty()) {
            targetSession = SessionHelper.findSessionByItemName(availableSessions, itemName);
            if (targetSession == null) {
                String messageTemplate = configManager.getMessage("item-not-found",
                        "<red>Item '<yellow><item_name></yellow>' not found in active rolls!");
                Component message = processMessageWithVariables(messageTemplate, null, itemName, null,
                        Placeholder.unparsed("item", itemName));
                player.sendMessage(message);

                showAvailableItems(player, availableSessions);
                return;
            }
        } else {
            if (availableSessions.size() == 1) {
                targetSession = availableSessions.getFirst();
            } else {
                showAvailableItems(player, availableSessions);
                return;
            }
        }

        int roll = RollUtils.generateRoll();

        if (isGreedRoll) {
            RollGreedEvent greedEvent = new RollGreedEvent(targetSession, player, roll);
            Bukkit.getPluginManager().callEvent(greedEvent);
            if (greedEvent.isCancelled()) {
                return;
            }
            roll = greedEvent.getRoll();
            targetSession.addGreedRoll(player, roll);
        } else {
            RollNeedEvent needEvent = new RollNeedEvent(targetSession, player, roll);
            Bukkit.getPluginManager().callEvent(needEvent);
            if (needEvent.isCancelled()) {
                return;
            }
            roll = needEvent.getRoll();
            targetSession.addRoll(player, roll);
        }

        announceRoll(targetSession, player, roll, isGreedRoll);

        if (targetSession.haveAllPlayersParticipated()) {
            finishRollSession(targetSession.getId());
        }
    }

    private void showAvailableItems(Player player, List<RollSession> sessions) {
        String messageTemplate = configManager.getMessage("available-items",
                "<gray>Available items:");
        Component message = processMessageWithVariables(messageTemplate, null, null, null);
        player.sendMessage(message);

        for (RollSession session : sessions) {
            String itemName = ItemUtils.getItemDisplayName(session.getItem(), configManager);
            String itemNameNoColors = ItemUtils.getItemDisplayNameWithoutColors(session.getItem(), configManager);
            String itemTemplate = configManager.getMessage("available-item-format",
                    "<gray>- <white><item></white>");
            Component itemMessage = MessageUtils.createLootMessage(itemTemplate, itemName, itemNameNoColors, session.getItem(), configManager);
            player.sendMessage(itemMessage);
        }
    }

    public List<RollSession> getAvailableSessionsForPlayer(Player player) {
        return SessionHelper.getAvailableSessionsForPlayer(player, playerActiveSessions, activeRolls, false);
    }

    public List<String> getAvailableGreedItemNames(Player player) {
        List<RollSession> sessions = SessionHelper.getAvailableSessionsForPlayer(player, playerActiveSessions, activeRolls, true);
        return SessionHelper.getAvailableItemNames(sessions);
    }

    private void announceRoll(RollSession session, Player player, int roll, boolean isGreedRoll) {
        String itemName = ItemUtils.getItemDisplayName(session.getItem(), configManager);
        String itemNameNoColors = ItemUtils.getItemDisplayNameWithoutColors(session.getItem(), configManager);

        String messageKey = isGreedRoll ? "player-greed-rolled" : "player-rolled";
        String rollMessageTemplate = configManager.getMessage(messageKey,
                isGreedRoll ?
                        "<green><player></green> <gray>greed rolled <aqua><roll></aqua> for <white><item></white>" :
                        "<green><player></green> <gray>rolled <aqua><roll></aqua> for <white><item></white>");

        for (Player participant : session.getParticipants()) {
            if (participant.isOnline()) {
                Component rollMessage = MessageUtils.createLootMessage(rollMessageTemplate, itemName, itemNameNoColors, session.getItem(), configManager,
                        Placeholder.unparsed("player", player.getName()),
                        Placeholder.unparsed("roll", String.valueOf(roll)));
                participant.sendMessage(rollMessage);
            }
        }
    }

    private void finishRollSession(String sessionId) {
        RollSession session = activeRolls.remove(sessionId);
        if (session == null) return;

        BukkitTask timer = rollTimers.remove(sessionId);
        if (timer != null && !timer.isCancelled()) {
            timer.cancel();
        }

        for (Player participant : session.getParticipants()) {
            List<RollSession> playerSessions = playerActiveSessions.get(participant);
            if (playerSessions != null) {
                playerSessions.remove(session);
                if (playerSessions.isEmpty()) {
                    playerActiveSessions.remove(participant);
                }
            }
        }

        Map<Player, Integer> effectiveRolls = session.getEffectiveRolls();

        if (effectiveRolls.isEmpty()) {
            unlockItemForParticipants(session);
            Bukkit.getPluginManager().callEvent(new RollSessionFinishEvent(session, null, session.getAllRolls()));
            return;
        }

        Player winner = SessionHelper.findWinner(effectiveRolls);
        if (winner != null) {
            giveItemToWinner(session, winner);
        } else {
            cleanupClientSideItem(sessionId);
        }

        Bukkit.getPluginManager().callEvent(new RollSessionFinishEvent(session, session.getWinner(), session.getAllRolls()));
    }

    private void unlockItemForParticipants(RollSession session) {
        Item droppedItem = session.getDroppedItem();
        
        if (droppedItem != null) {
            clientSideItems.remove(droppedItem.getEntityId());
            unlockedItems.put(droppedItem.getEntityId(), new ArrayList<>(session.getParticipants()));
        }
        
        session.setWinner(null);

        String itemName = ItemUtils.getItemDisplayName(session.getItem(), configManager);
        String itemNameNoColors = ItemUtils.getItemDisplayNameWithoutColors(session.getItem(), configManager);
        String noRollMessageTemplate = configManager.getMessage("no-rolls-unlocked",
                "<gray>No one rolled for <white><item></white>. Item is now available for pickup by party members!");

        for (Player participant : session.getParticipants()) {
            if (participant.isOnline()) {
                Component noRollMessage = MessageUtils.createLootMessage(noRollMessageTemplate, itemName, itemNameNoColors, session.getItem(), configManager);
                participant.sendMessage(noRollMessage);
            }
        }
    }

    private void giveItemToWinner(RollSession session, Player winner) {
        session.setWinner(winner);
        ItemStack item = session.getItem();

        // Handle exp and money loot
        if (ItemUtils.isExpLoot(item)) {
            int amount = ItemUtils.getLootAmount(item);
            winner.giveExp(amount);
        } else if (ItemUtils.isMmoExpLoot(item)) {
            int amount = ItemUtils.getLootAmount(item);
            if (Bukkit.getPluginManager().getPlugin("MMOCore") != null) {
                try {
                    PlayerData playerData = PlayerData.get(winner.getUniqueId());
                    playerData.giveExperience(amount, EXPSource.OTHER);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to give MMOCore exp: " + e.getMessage());
                }
            }
        } else if (ItemUtils.isMoneyLoot(item)) {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                RegisteredServiceProvider<Economy> rsp = 
                    Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
                if (rsp != null) {
                    Economy economy = rsp.getProvider();
                    int amount = ItemUtils.getLootAmount(item);
                    economy.depositPlayer(winner, amount);
                }
            }
        } else {
            winner.getInventory().addItem(item);
        }

        Item droppedItem = session.getDroppedItem();
        if (droppedItem != null) {
            hideItemFromPlayers(droppedItem, session.getParticipants(), plugin);
        }
        cleanupClientSideItem(session.getId());

        String itemName = ItemUtils.getItemDisplayName(session.getItem(), configManager);
        String itemNameNoColors = ItemUtils.getItemDisplayNameWithoutColors(session.getItem(), configManager);

        boolean wonWithGreed = !session.getRolls().containsKey(winner) && session.getGreedRolls().containsKey(winner);
        String messageKey = wonWithGreed ? "greed-roll-winner" : "roll-winner";
        String winMessageTemplate = configManager.getMessage(messageKey,
                wonWithGreed ?
                        "<gold><player></gold> <gray>won <white><item></white> with a greed roll of <aqua><roll></aqua>!" :
                        "<gold><player></gold> <gray>won <white><item></white> with a roll of <aqua><roll></aqua>!");

        int winningRoll = session.getEffectiveRolls().get(winner);

        for (Player participant : session.getParticipants()) {
            if (participant.isOnline()) {
                Component winMessage = MessageUtils.createLootMessage(winMessageTemplate, itemName, itemNameNoColors, session.getItem(), configManager,
                        Placeholder.unparsed("player", winner.getName()),
                        Placeholder.unparsed("roll", String.valueOf(winningRoll)));
                participant.sendMessage(winMessage);
            }
        }
    }


    private void cleanupClientSideItem(String sessionId) {
        RollSession session = activeRolls.get(sessionId);
        if (session != null) {
            Item droppedItem = session.getDroppedItem();
            if (droppedItem != null) {
                clientSideItems.remove(droppedItem.getEntityId());
                if (droppedItem.isValid()) {
                    droppedItem.remove();
                }
            }
        }
    }

    public void cleanup() {
        for (BukkitTask timer : rollTimers.values()) {
            if (timer != null && !timer.isCancelled()) {
                timer.cancel();
            }
        }
        rollTimers.clear();

        for (String sessionId : activeRolls.keySet()) {
            cleanupClientSideItem(sessionId);
        }
        activeRolls.clear();
        clientSideItems.clear();
        playerActiveSessions.clear();
        itemOwnership.clear();
        unlockedItems.clear();
    }
}