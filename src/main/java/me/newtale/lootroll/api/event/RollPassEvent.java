package me.newtale.lootroll.api.event;

import me.newtale.lootroll.model.RollSession;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class RollPassEvent extends RollSessionEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private boolean cancelled;

    public RollPassEvent(RollSession session, Player player) {
        super(session);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
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

