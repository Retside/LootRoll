package me.newtale.lootroll.listener;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import me.newtale.lootroll.manager.ConfigManager;
import me.newtale.lootroll.manager.LootManager;
import me.newtale.lootroll.manager.PartyManager;
import me.newtale.lootroll.manager.RollManager;
import me.newtale.lootroll.model.LootItem;
import me.newtale.lootroll.model.MobLootConfig;
import me.newtale.lootroll.util.ItemUtils;
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
    private final ConfigManager configManager;
    private final Set<UUID> mythicHandledDeaths = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private Economy economy;

    public MobDeathListener(LootManager lootManager, PartyManager partyManager, RollManager rollManager, ConfigManager configManager) {
        this.lootManager = lootManager;
        this.partyManager = partyManager;
        this.rollManager = rollManager;
        this.configManager = configManager;
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
        
        List<ItemStack> droppedItems = new java.util.ArrayList<>();
        
        if (config.shouldProcessVanillaDrops() && vanillaDrops != null) {
            droppedItems.addAll(vanillaDrops);
        }
        
        List<ItemStack> customLoot = lootManager.generateLoot(mobId);
        droppedItems.addAll(customLoot);
        
        // Process exp and money loot - create ItemStacks for rolling (virtual, no physical drop)
        List<LootItem> expMoneyLoot = lootManager.getExpAndMoneyLoot(config);
        boolean inParty = partyManager.isPlayerInParty(killer) && partyManager.hasMultiplePartyMembers(killer);
        boolean expMoneyProcessed = false;
        
        for (LootItem lootItem : expMoneyLoot) {
            String type = lootItem.getType().toUpperCase();
            int[] amountRange = parseAmountRange(lootItem);
            int amount = ThreadLocalRandom.current().nextInt(amountRange[0], amountRange[1] + 1);
            
            if ("EXP".equals(type)) {
                expMoneyProcessed = true;
                if (inParty) {
                    // Roll for exp if in party (virtual item, no physical drop)
                    ItemStack expItem = ItemUtils.createExpItemStack(amount, configManager);
                    rollManager.startRollSession(expItem, partyManager.getPartyMembers(killer), location);
                } else {
                    // Give directly if not in party
                    killer.giveExp(amount);
                }
            } else if ("MONEY".equals(type)) {
                expMoneyProcessed = true;
                if (economy == null) {
                    Bukkit.getLogger().warning("Vault not found! Money loot cannot be given.");
                    continue;
                }
                if (inParty) {
                    // Roll for money if in party (virtual item, no physical drop)
                    ItemStack moneyItem = ItemUtils.createMoneyItemStack(amount, configManager);
                    rollManager.startRollSession(moneyItem, partyManager.getPartyMembers(killer), location);
                } else {
                    // Give directly if not in party
                    economy.depositPlayer(killer, amount);
                }
            }
        }
        
        // Return true if we have items to drop OR if we processed exp/money (for override-vanilla-drops to work)
        if (droppedItems.isEmpty() && !expMoneyProcessed) {
            return false;
        }

        // Process dropped items if any
        if (!droppedItems.isEmpty()) {
            if (!partyManager.isPlayerInParty(killer) || !partyManager.hasMultiplePartyMembers(killer)) {
                for (ItemStack item : droppedItems) {
                    // Handle exp/money for solo players
                    if (ItemUtils.isExpLoot(item)) {
                        int amount = ItemUtils.getLootAmount(item);
                        killer.giveExp(amount);
                    } else if (ItemUtils.isMoneyLoot(item)) {
                        if (economy != null) {
                            int amount = ItemUtils.getLootAmount(item);
                            economy.depositPlayer(killer, amount);
                        }
                    } else {
                        rollManager.dropItemForSoloPlayer(item, killer, location);
                    }
                }
                return true;
            }

            List<Player> partyMembers = partyManager.getPartyMembers(killer);
            if (partyMembers.size() <= 1) {
                for (ItemStack item : droppedItems) {
                    // Handle exp/money for solo players
                    if (ItemUtils.isExpLoot(item)) {
                        int amount = ItemUtils.getLootAmount(item);
                        killer.giveExp(amount);
                    } else if (ItemUtils.isMoneyLoot(item)) {
                        if (economy != null) {
                            int amount = ItemUtils.getLootAmount(item);
                            economy.depositPlayer(killer, amount);
                        }
                    } else {
                        rollManager.dropItemForSoloPlayer(item, killer, location);
                    }
                }
                return true;
            }

            for (ItemStack item : droppedItems) {
                rollManager.startRollSession(item, partyMembers, location);
            }
        }
        
        // Return true if we processed something (items or exp/money)
        return true;
    }


    private int[] parseAmountRange(LootItem lootItem) {
        int min = lootItem.getMinDrops();
        int max = lootItem.getMaxDrops();
        return new int[]{min, max};
    }
}
