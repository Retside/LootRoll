package me.newtale.lootroll.api.event;

import me.newtale.lootroll.model.RollSession;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.Map;

public class RollSessionFinishEvent extends RollSessionEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player winner;
    private final Map<Player, Integer> finalRolls;

    public RollSessionFinishEvent(RollSession session, Player winner, Map<Player, Integer> finalRolls) {
        super(session);
        this.winner = winner;
        this.finalRolls = Collections.unmodifiableMap(finalRolls);
    }

    public Player getWinner() {
        return winner;
    }

    public Map<Player, Integer> getFinalRolls() {
        return finalRolls;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

