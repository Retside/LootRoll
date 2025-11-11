package me.newtale.lootroll.listener;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import me.newtale.lootroll.manager.LootManager;
import me.newtale.lootroll.manager.PartyManager;
import me.newtale.lootroll.manager.RollManager;
import me.newtale.lootroll.model.LootItem;
import me.newtale.lootroll.model.MobLootConfig;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class MobDeathListener implements Listener {

    private final LootManager lootManager;
    private final PartyManager partyManager;
    private final RollManager rollManager;
    private final Set<UUID> mythicHandledDeaths = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private Economy economy;

    public MobDeathListener(LootManager lootManager, PartyManager partyManager, RollManager rollManager) {
        this.lootManager = lootManager;
        this.partyManager = partyManager;
        this.rollManager = rollManager;
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
    }

    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        if (!(event.getKiller() instanceof Player killer)) return;

        String mobId = event.getMobType().getInternalName();
        if (!lootManager.hasMobLoot(mobId)) return;

        boolean handled = processMobDeath(killer, mobId, event.getEntity().getLocation());
        if (handled) {
            mythicHandledDeaths.add(event.getEntity().getUniqueId());
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player killer)) return;

        if (mythicHandledDeaths.remove(event.getEntity().getUniqueId())) {
            return;
        }

        String mobId = event.getEntityType().name();
        if (!lootManager.hasMobLoot(mobId)) {
            return;
        }

        MobLootConfig config = lootManager.getMobLootConfig(mobId);
        if (config == null) return;

        boolean handled = processMobDeath(killer, mobId, event.getEntity().getLocation(), event.getDrops(), config);
        
        if (config.shouldOverrideVanillaDrops() && handled) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        } else if (config.shouldProcessVanillaDrops() && handled) {
            event.getDrops().clear();
        }
    }

    private boolean processMobDeath(Player killer, String mobId, Location location) {
        MobLootConfig config = lootManager.getMobLootConfig(mobId);
        return processMobDeath(killer, mobId, location, null, config);
    }

    private boolean processMobDeath(Player killer, String mobId, Location location, 
                                    List<ItemStack> vanillaDrops, MobLootConfig config) {
        if (config == null) return false;
        
        // Process exp and money loot
        List<LootItem> expMoneyLoot = lootManager.getExpAndMoneyLoot(config);
        if (!expMoneyLoot.isEmpty()) {
            processExpAndMoneyLoot(killer, expMoneyLoot);
        }
        
        List<ItemStack> droppedItems = new java.util.ArrayList<>();
        
        if (config.shouldProcessVanillaDrops() && vanillaDrops != null) {
            droppedItems.addAll(vanillaDrops);
        }
        
        List<ItemStack> customLoot = lootManager.generateLoot(mobId);
        droppedItems.addAll(customLoot);
        
        if (droppedItems.isEmpty() && expMoneyLoot.isEmpty()) return false;

        if (!partyManager.isPlayerInParty(killer) || !partyManager.hasMultiplePartyMembers(killer)) {
            for (ItemStack item : droppedItems) {
                rollManager.dropItemForSoloPlayer(item, killer, location);
            }
            return true;
        }

        List<Player> partyMembers = partyManager.getPartyMembers(killer);
        if (partyMembers.size() <= 1) {
            for (ItemStack item : droppedItems) {
                rollManager.dropItemForSoloPlayer(item, killer, location);
            }
            return true;
        }

        for (ItemStack item : droppedItems) {
            rollManager.startRollSession(item, partyMembers, location);
        }
        return true;
    }

    private void processExpAndMoneyLoot(Player killer, List<LootItem> expMoneyLoot) {
        List<Player> partyMembers = null;
        boolean inParty = partyManager.isPlayerInParty(killer) && partyManager.hasMultiplePartyMembers(killer);
        if (inParty) {
            partyMembers = partyManager.getPartyMembers(killer);
        }

        for (LootItem lootItem : expMoneyLoot) {
            String type = lootItem.getType().toUpperCase();
            int[] amountRange = parseAmountRange(lootItem);
            int amount = ThreadLocalRandom.current().nextInt(amountRange[0], amountRange[1] + 1);

            if ("EXP".equals(type)) {
                if (lootItem.isSplit() && inParty && partyMembers != null && partyMembers.size() > 1) {
                    int splitAmount = amount / partyMembers.size();
                    for (Player member : partyMembers) {
                        member.giveExp(splitAmount);
                    }
                } else {
                    killer.giveExp(amount);
                }
            } else if ("MONEY".equals(type)) {
                if (economy == null) {
                    Bukkit.getLogger().warning("Vault not found! Money loot cannot be given.");
                    continue;
                }
                if (lootItem.isSplit() && inParty && partyMembers != null && partyMembers.size() > 1) {
                    double splitAmount = (double) amount / partyMembers.size();
                    for (Player member : partyMembers) {
                        economy.depositPlayer(member, splitAmount);
                    }
                } else {
                    economy.depositPlayer(killer, amount);
                }
            }
        }
    }

    private int[] parseAmountRange(LootItem lootItem) {
        int min = lootItem.getMinDrops();
        int max = lootItem.getMaxDrops();
        return new int[]{min, max};
    }
}
