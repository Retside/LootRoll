package me.newtale.lootroll.api.event;

import me.newtale.lootroll.model.RollSession;
import org.bukkit.event.Event;

/**
 * Base class for all LootRoll session related events.
 */
public abstract class RollSessionEvent extends Event {
    private final RollSession session;

    protected RollSessionEvent(RollSession session) {
        super(false);
        this.session = session;
    }

    public RollSession getSession() {
        return session;
    }
}

