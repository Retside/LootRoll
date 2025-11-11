package me.newtale.lootroll.api.event;

import me.newtale.lootroll.model.RollSession;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class RollGreedEvent extends RollSessionEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private int roll;
    private boolean cancelled;

    public RollGreedEvent(RollSession session, Player player, int roll) {
        super(session);
        this.player = player;
        this.roll = roll;
    }

    public Player getPlayer() {
        return player;
    }

    public int getRoll() {
        return roll;
    }

    public void setRoll(int roll) {
        this.roll = roll;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

