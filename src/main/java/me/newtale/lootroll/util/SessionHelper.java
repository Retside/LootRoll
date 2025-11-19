package me.newtale.lootroll.util;

import org.bukkit.entity.Player;

import me.newtale.lootroll.model.RollSession;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SessionHelper {

    public static RollSession findSessionByItemName(List<RollSession> sessions, String itemName) {
        String normalizedInput = itemName.toLowerCase().trim();

        for (RollSession session : sessions) {

            String sessionItemName = ItemUtils.getItemDisplayName(session.getItem()).toLowerCase();
            String sessionItemNameNoColors = ItemUtils.getItemDisplayNameWithoutColors(session.getItem()).toLowerCase();

            if (sessionItemName.equals(normalizedInput) || sessionItemNameNoColors.equals(normalizedInput)) {
                return session;
            }

            if (sessionItemName.contains(normalizedInput) || sessionItemNameNoColors.contains(normalizedInput)) {
                return session;
            }
        }

        return null;
    }

    public static List<RollSession> getAvailableSessionsForPlayer(Player player,
                                                                Map<Player, List<RollSession>> playerActiveSessions,
                                                                Map<String, RollSession> activeRolls) {
        return getAvailableSessionsForPlayer(player, playerActiveSessions, activeRolls, false);
    }


    public static List<RollSession> getAvailableSessionsForPlayer(Player player,
                                                                Map<Player, List<RollSession>> playerActiveSessions,
                                                                Map<String, RollSession> activeRolls,
                                                                boolean isGreedRoll) {
        List<RollSession> sessions = playerActiveSessions.get(player);
        if (sessions == null) {
            return List.of();
        }

        List<RollSession> activeSessions = sessions.stream()
                .filter(session -> activeRolls.containsKey(session.getId()))
                .toList();

        if (isGreedRoll) {
            return activeSessions.stream()
                    .filter(session -> !session.hasPlayerRolledAny(player))
                    .collect(Collectors.toList());
        } else {
            return activeSessions.stream()
                    .filter(session -> !session.hasPlayerRolled(player))
                    .collect(Collectors.toList());
        }
    }

    public static List<RollSession> getAvailableSessionsForSkip(Player player,
                                                                Map<Player, List<RollSession>> playerActiveSessions,
                                                                Map<String, RollSession> activeRolls) {
        List<RollSession> sessions = playerActiveSessions.get(player);
        if (sessions == null) {
            return List.of();
        }

        return sessions.stream()
                .filter(session -> activeRolls.containsKey(session.getId()))
                .filter(session -> !session.hasPlayerParticipated(player)) // Player hasn't participated yet
                .collect(Collectors.toList());
    }

    public static Player findWinner(Map<Player, Integer> rolls) {
        Player winner = null;
        int highestRoll = 0;

        for (Map.Entry<Player, Integer> entry : rolls.entrySet()) {
            if (entry.getValue() > highestRoll) {
                highestRoll = entry.getValue();
                winner = entry.getKey();
            }
        }

        return winner;
    }

    public static List<String> getAvailableItemNamesForTabCompletion(List<RollSession> sessions) {
        return sessions.stream()
                .map(session -> ItemUtils.getItemDisplayNameWithoutColors(session.getItem()))
                .collect(Collectors.toList());
    }

    public static List<String> getAvailableItemNames(List<RollSession> sessions) {
        return sessions.stream()
                .map(session -> ItemUtils.getItemDisplayName(session.getItem()))
                .collect(Collectors.toList());
    }
}