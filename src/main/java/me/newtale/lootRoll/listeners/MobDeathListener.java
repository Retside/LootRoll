package me.newtale.lootRoll.listeners;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import me.newtale.lootRoll.managers.LootManager;
import me.newtale.lootRoll.managers.PartyManager;
import me.newtale.lootRoll.managers.RollManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MobDeathListener implements Listener {

    private final LootManager lootManager;
    private final PartyManager partyManager;
    private final RollManager rollManager;

    public MobDeathListener(LootManager lootManager, PartyManager partyManager, RollManager rollManager) {
        this.lootManager = lootManager;
        this.partyManager = partyManager;
        this.rollManager = rollManager;
    }

    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        if (!(event.getKiller() instanceof Player)) return;

        Player killer = (Player) event.getKiller();
        String mobId = event.getMobType().getInternalName();

        if (!lootManager.hasMobLoot(mobId)) return;

        if (!partyManager.isPlayerInParty(killer) || !partyManager.hasMultiplePartyMembers(killer)) {
            List<ItemStack> droppedItems = lootManager.generateLoot(mobId);
            if (droppedItems.isEmpty()) return;

            for (ItemStack item : droppedItems) {
                rollManager.dropItemForSoloPlayer(item, killer, event.getEntity().getLocation());
            }
            return;
        }

        List<Player> partyMembers = partyManager.getPartyMembers(killer);
        if (partyMembers.size() <= 1) {
            List<ItemStack> droppedItems = lootManager.generateLoot(mobId);
            if (droppedItems.isEmpty()) return;

            for (ItemStack item : droppedItems) {
                rollManager.dropItemForSoloPlayer(item, killer, event.getEntity().getLocation());
            }
            return;
        }

        List<ItemStack> droppedItems = lootManager.generateLoot(mobId);
        if (droppedItems.isEmpty()) return;

        for (ItemStack item : droppedItems) {
            rollManager.startRollSession(item, partyMembers, event.getEntity().getLocation());
        }
    }
}