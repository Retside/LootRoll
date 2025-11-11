package me.newtale.lootroll.api.event;

import me.newtale.lootroll.model.RollSession;
import org.bukkit.event.HandlerList;

public class RollSessionStartEvent extends RollSessionEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public RollSessionStartEvent(RollSession session) {
        super(session);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

