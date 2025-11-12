package me.newtale.lootroll.manager;

import io.lumine.mythic.lib.api.item.NBTItem;
import me.newtale.lootroll.common.config.MobDropConfig;
import me.newtale.lootroll.model.LootItem;
import me.newtale.lootroll.model.MobLootConfig;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.items.MythicItem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LootManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final Map<String, MobLootConfig> mobLootMap;

    private static final Pattern NEW_FORMAT_PATTERN = Pattern.compile(
            "^(\\w+)\\{([^}]+)}\\s+(\\d+(?:-\\d+|to\\d+)?)\\s+([\\d.]+)$",
            Pattern.CASE_INSENSITIVE
    );

    public LootManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.mobLootMap = new HashMap<>();
    }

    public void loadLootConfiguration() {
        mobLootMap.clear();

        Map<String, Map<String, MobDropConfig>> dropConfigs = configManager.getDropConfigs();

        for (Map.Entry<String, Map<String, MobDropConfig>> entry : dropConfigs.entrySet()) {
            String fileName = entry.getKey();
            Map<String, MobDropConfig> mobConfigs = entry.getValue();
            loadMobsFromConfig(mobConfigs, fileName);
        }
    }

    private int loadMobsFromConfig(Map<String, MobDropConfig> mobConfigs, String fileName) {
        int mobCount = 0;

        for (Map.Entry<String, MobDropConfig> entry : mobConfigs.entrySet()) {
            String mobId = entry.getKey();
            MobDropConfig mobConfig = entry.getValue();
            
            if (mobConfig == null) {
                continue;
            }

            int globalMinDrops = mobConfig.getMinDrops();
            int globalMaxDrops = mobConfig.getMaxDrops();
            boolean overrideVanillaDrops = mobConfig.isOverrideVanillaDrops();
            boolean processVanillaDrops = mobConfig.isProcessVanillaDrops();

            List<LootItem> lootItems = new ArrayList<>();

            // Парсимо список лутів
            List<String> lootList = mobConfig.getLoot();
            if (lootList != null) {
                for (String lootLine : lootList) {
                    LootItem item = parseNewFormatLoot(lootLine);
                    if (item != null) {
                        lootItems.add(item);
                    }
                }
            }

            if (!lootItems.isEmpty() || processVanillaDrops) {
                String normalizedId = normalizeMobId(mobId);
                MobLootConfig mobLootConfig = new MobLootConfig(mobId, lootItems, globalMinDrops, globalMaxDrops, 
                        overrideVanillaDrops, processVanillaDrops);
                mobLootMap.put(normalizedId, mobLootConfig);
                mobCount++;
            } else {
                plugin.getLogger().warning("No loot items found for mob: " + mobId);
            }
        }

        return mobCount;
    }

    private LootItem parseNewFormatLoot(String lootLine) {
        try {
            String trimmed = lootLine.trim();
            Matcher matcher = NEW_FORMAT_PATTERN.matcher(trimmed);
            if (!matcher.matches()) {
                plugin.getLogger().warning("Invalid loot format: " + lootLine + " (trimmed: " + trimmed + ")");
                return null;
            }

            String lootType = matcher.group(1).toUpperCase();
            String parameters = matcher.group(2);
            String amountStr = matcher.group(3); // May be "3", "2-5", or "0to3"
            double chanceDecimal = Double.parseDouble(matcher.group(4));
            double chance = chanceDecimal * 100.0;

            // Parse parameters inside curly braces
            Map<String, String> params = parseParameters(parameters);

            // For EXP and MONEY types, itemId is not required
            String itemId = null;
            if (!"EXP".equals(lootType) && !"MONEY".equals(lootType)) {
                String type = params.get("type");
                String item = params.get("item");

                if (type != null && item != null) {
                    itemId = type + ":" + item;
                } else if (item != null) {
                    itemId = item;
                }

                if (itemId == null) {
                    plugin.getLogger().warning("No item identifier found in loot line: " + lootLine);
                    return null;
                }
            } else {
                // For EXP and MONEY, use the type as itemId for consistency
                itemId = lootType.toLowerCase();
            }

            boolean unidentified = Boolean.parseBoolean(params.getOrDefault("unidentified", "false"));
            boolean split = Boolean.parseBoolean(params.getOrDefault("split", "false"));

            // Parse min-drops and max-drops from new or legacy format
            int minDrops, maxDrops;

            // First, check legacy-style parameters in curly braces
            if (params.containsKey("min-drops") || params.containsKey("max-drops")) {
                // Legacy: take from parameters
                minDrops = Integer.parseInt(params.getOrDefault("min-drops", "1"));
                maxDrops = Integer.parseInt(params.getOrDefault("max-drops", String.valueOf(minDrops)));
            } else {
                // New format: parse from amountStr
                int[] dropRange = parseDropRange(amountStr);
                minDrops = dropRange[0];
                maxDrops = dropRange[1];
            }

            // amount for legacy logic (number of items, not drops)
            int amount = 1; // Default 1

            return new LootItem(lootType, itemId, chance, amount, unidentified, minDrops, maxDrops, split);

        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing loot line '" + lootLine + "': " + e.getMessage());
            return null;
        }
    }

    private int[] parseDropRange(String amountStr) {
        try {
            // Check "X-Y" format
            if (amountStr.contains("-")) {
                String[] parts = amountStr.split("-", 2);
                int min = Integer.parseInt(parts[0]);
                int max = Integer.parseInt(parts[1]);
                return new int[]{min, max};
            }

            // Check "XtoY" format
            if (amountStr.contains("to")) {
                String[] parts = amountStr.split("to", 2);
                int min = Integer.parseInt(parts[0]);
                int max = Integer.parseInt(parts[1]);
                return new int[]{min, max};
            }

            // Single number → min and max are equal
            int value = Integer.parseInt(amountStr);
            return new int[]{value, value};

        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing drop range '" + amountStr + "': " + e.getMessage());
            // Fallback: return 1-1
            return new int[]{1, 1};
        }
    }

    private Map<String, String> parseParameters(String parameters) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = parameters.split(";");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }

        return params;
    }

    public List<LootItem> getMobLoot(String mobId) {
        MobLootConfig config = mobLootMap.get(normalizeMobId(mobId));
        return config != null ? config.getLootItems() : null;
    }

    public MobLootConfig getMobLootConfig(String mobId) {
        return mobLootMap.get(normalizeMobId(mobId));
    }

    public boolean hasMobLoot(String mobId) {
        MobLootConfig config = mobLootMap.get(normalizeMobId(mobId));
        return config != null && (!config.getLootItems().isEmpty() || config.shouldProcessVanillaDrops());
    }

    public List<ItemStack> generateLoot(String mobId) {
        MobLootConfig mobLootConfig = mobLootMap.get(normalizeMobId(mobId));
        if (mobLootConfig == null || mobLootConfig.getLootItems().isEmpty()) {
            return new ArrayList<>();
        }

        return generateLoot(mobLootConfig);
    }

    public List<ItemStack> generateLoot(MobLootConfig mobLootConfig) {
        List<ItemStack> items = new ArrayList<>();

        int targetDropCount = ThreadLocalRandom.current().nextInt(
                mobLootConfig.getMinDrops(),
                mobLootConfig.getMaxDrops() + 1
        );

        if (mobLootConfig.getLootItems().isEmpty() || targetDropCount == 0) {
            return items;
        }

        List<LootItem> availableItems = new ArrayList<>(mobLootConfig.getLootItems());

        for (int i = 0; i < targetDropCount; i++) {
            if (availableItems.isEmpty()) {
                break;
            }

            LootItem selectedItem = null;
            int attempts = 0;
            int maxAttempts = availableItems.size() * 3; // Max attempts

            while (selectedItem == null && attempts < maxAttempts) {
                LootItem candidateItem = availableItems.get(
                        ThreadLocalRandom.current().nextInt(availableItems.size())
                );

                if (ThreadLocalRandom.current().nextDouble(100.0) <= candidateItem.getChance()) {
                    selectedItem = candidateItem;
                }

                attempts++;
            }

            if (selectedItem == null && i < mobLootConfig.getMinDrops()) {
                selectedItem = availableItems.get(
                        ThreadLocalRandom.current().nextInt(availableItems.size())
                );
            }

            if (selectedItem == null) {
                continue;
            }

            // Skip exp and money items - they are handled separately
            String itemType = selectedItem.getType().toUpperCase();
            if ("EXP".equals(itemType) || "MONEY".equals(itemType)) {
                availableItems.remove(selectedItem);
                continue;
            }

            List<ItemStack> itemDrops = createItemDrops(selectedItem);
            items.addAll(itemDrops);

            availableItems.remove(selectedItem);
        }

        // Stack identical items together
        return stackIdenticalItems(items);
    }

    private List<ItemStack> stackIdenticalItems(List<ItemStack> items) {
        List<ItemStack> stackedItems = new ArrayList<>();
        
        for (ItemStack item : items) {
            if (item == null) continue;
            
            boolean found = false;
            for (ItemStack stacked : stackedItems) {
                // Use isSimilar to check if items can be stacked
                if (stacked.isSimilar(item)) {
                    int newAmount = stacked.getAmount() + item.getAmount();
                    int maxStackSize = stacked.getMaxStackSize();
                    
                    if (newAmount <= maxStackSize) {
                        stacked.setAmount(newAmount);
                    } else {
                        // If exceeds max stack size, keep the existing and add remainder
                        stacked.setAmount(maxStackSize);
                        ItemStack remainder = item.clone();
                        remainder.setAmount(newAmount - maxStackSize);
                        stackedItems.add(remainder);
                    }
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                stackedItems.add(item.clone());
            }
        }
        
        return stackedItems;
    }

    public List<LootItem> getExpAndMoneyLoot(MobLootConfig mobLootConfig) {
        List<LootItem> expMoneyItems = new ArrayList<>();
        
        for (LootItem lootItem : mobLootConfig.getLootItems()) {
            String itemType = lootItem.getType().toUpperCase();
            if ("EXP".equals(itemType) || "MONEY".equals(itemType)) {
                // Check chance
                if (ThreadLocalRandom.current().nextDouble(100.0) <= lootItem.getChance()) {
                    expMoneyItems.add(lootItem);
                }
            }
        }
        
        return expMoneyItems;
    }

    private List<ItemStack> createItemDrops(LootItem lootItem) {
        List<ItemStack> drops = new ArrayList<>();

        int dropCount = ThreadLocalRandom.current().nextInt(
                lootItem.getMinDrops(),
                lootItem.getMaxDrops() + 1
        );

        if (dropCount <= 0) {
            return drops;
        }

        ItemStack baseItem = createItemStack(lootItem);
        if (baseItem == null) {
            return drops;
        }

        // Stack items together instead of creating separate ItemStacks
        ItemStack stackedItem = baseItem.clone();
        stackedItem.setAmount(dropCount);
        drops.add(stackedItem);

        return drops;
    }

    private ItemStack createItemStack(LootItem lootItem) {
        try {
            ItemStack item = null;

            switch (lootItem.getType().toUpperCase()) {
                case "MMOITEMS":
                    if (Bukkit.getPluginManager().getPlugin("MMOItems") != null) {
                        String[] parts = lootItem.getItemId().split(":");
                        if (parts.length >= 2) {
                            Type type = Type.get(parts[0]);
                            String id = parts[1];
                            if (type != null) {
                                item = MMOItems.plugin.getItem(type, id);

                                if (item != null && lootItem.isUnidentified()) {
                                    item = unidentifyItem(item);
                                }
                            }
                        }
                    }
                    break;

                case "MM":
                case "MYTHICMOBS":
                    if (Bukkit.getPluginManager().getPlugin("MythicMobs") != null) {
                        try {
                            Optional<MythicItem> mythicItem = MythicBukkit.inst().getItemManager().getItem(lootItem.getItemId());
                            if (mythicItem.isPresent()) {
                                item = (ItemStack) mythicItem.get().generateItemStack(lootItem.getAmount());
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to create MythicMobs item: " + lootItem.getItemId() + " - " + e.getMessage());
                        }
                    }
                    break;

                case "VANILLA":
                default:
                    Material material = Material.getMaterial(lootItem.getItemId().toUpperCase());
                    if (material != null) {
                        item = new ItemStack(material, lootItem.getAmount());
                    }
                    break;
            }

            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create item: " + lootItem.getItemId());
            return null;
        }
    }

    private ItemStack unidentifyItem(ItemStack itemToUnidentify) {
        try {
            Type type = Type.get(itemToUnidentify);
            if (type != null && type.getUnidentifiedTemplate() != null) {
                return type.getUnidentifiedTemplate().newBuilder(NBTItem.get(itemToUnidentify)).build();
            }
            return itemToUnidentify;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to unidentify item: " + e.getMessage());
            return itemToUnidentify;
        }
    }

    public Set<String> getAllMobIds() {
        return mobLootMap.keySet();
    }

    public int getTotalMobCount() {
        return mobLootMap.size();
    }

    public Map<String, MobLootConfig> getAllMobLootConfigs() {
        return new HashMap<>(mobLootMap);
    }

    private String normalizeMobId(String mobId) {
        return mobId == null ? null : mobId.toUpperCase(Locale.ROOT);
    }

}