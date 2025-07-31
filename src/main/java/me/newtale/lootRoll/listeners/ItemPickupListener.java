package me.newtale.lootRoll.listeners;

import me.newtale.lootRoll.managers.RollManager;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

public class ItemPickupListener implements Listener {

    private final RollManager rollManager;

    public ItemPickupListener(RollManager rollManager) {
        this.rollManager = rollManager;
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        Item item = event.getItem();

        if (!rollManager.canPlayerPickupItem(player, item.getEntityId())) {
            event.setCancelled(true);
            return;
        }

        rollManager.removeItemOwnership(item.getEntityId());
    }
}