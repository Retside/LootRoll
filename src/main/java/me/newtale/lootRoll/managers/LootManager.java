package me.newtale.lootRoll.managers;

import io.lumine.mythic.lib.api.item.NBTItem;
import me.newtale.lootRoll.models.LootItem;
import me.newtale.lootRoll.models.MobLootConfig;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.items.MythicItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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

    private static final Pattern NEW_FORMAT_PATTERN = Pattern.compile("^(\\w+)\\{([^}]+)}\\s+(\\d+)\\s+([.\\d]+)$");

    public LootManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.mobLootMap = new HashMap<>();
    }

    public void loadLootConfiguration() {
        mobLootMap.clear();


        Map<String, FileConfiguration> dropConfigs = configManager.getDropConfigs();
        int totalMobs = 0;

        for (Map.Entry<String, FileConfiguration> entry : dropConfigs.entrySet()) {
            String fileName = entry.getKey();
            FileConfiguration dropConfig = entry.getValue();


            int mobsFromFile = loadMobsFromConfig(dropConfig, fileName);
            totalMobs += mobsFromFile;
        }


        ConfigurationSection mobsSection = configManager.getConfig().getConfigurationSection("mobs");
        if (mobsSection != null) {
            int mobsFromMainConfig = loadMobsFromSection(mobsSection, "main config");
            totalMobs += mobsFromMainConfig;
        }
    }

    private int loadMobsFromConfig(FileConfiguration config, String fileName) {
        int mobCount = 0;


        Set<String> rootKeys = config.getKeys(false);

        for (String mobId : rootKeys) {
            ConfigurationSection mobSection = config.getConfigurationSection(mobId);
            if (mobSection == null) continue;


            int globalMinDrops = mobSection.getInt("min-drops", 0);
            int globalMaxDrops = mobSection.getInt("max-drops", globalMinDrops);

            List<LootItem> lootItems = new ArrayList<>();


            if (mobSection.isList("loot")) {
                List<String> lootList = mobSection.getStringList("loot");
                for (String lootLine : lootList) {
                    LootItem item = parseNewFormatLoot(lootLine);
                    if (item != null) {
                        lootItems.add(item);
                    }
                }
            } else {

                ConfigurationSection lootSection = mobSection.getConfigurationSection("loot");
                if (lootSection != null) {
                    for (String lootId : lootSection.getKeys(false)) {
                        ConfigurationSection itemSection = lootSection.getConfigurationSection(lootId);
                        if (itemSection == null) continue;

                        String type = itemSection.getString("type", "VANILLA");
                        String itemId = itemSection.getString("item");
                        double chance = itemSection.getDouble("chance", 100.0);
                        int amount = itemSection.getInt("amount", 1);
                        boolean unidentified = itemSection.getBoolean("unidentified", false);


                        int itemMinDrops = itemSection.getInt("min-drops", 0);
                        int itemMaxDrops = itemSection.getInt("max-drops", itemMinDrops);

                        if (itemId != null) {
                            lootItems.add(new LootItem(type, itemId, chance, amount, unidentified, itemMinDrops, itemMaxDrops));
                        }
                    }
                }
            }

            if (!lootItems.isEmpty()) {
                MobLootConfig mobLootConfig = new MobLootConfig(mobId, lootItems, globalMinDrops, globalMaxDrops);
                mobLootMap.put(mobId, mobLootConfig);
                mobCount++;
            }
        }

        return mobCount;
    }

    private int loadMobsFromSection(ConfigurationSection mobsSection, String source) {
        int mobCount = 0;

        for (String mobId : mobsSection.getKeys(false)) {
            ConfigurationSection mobSection = mobsSection.getConfigurationSection(mobId);
            if (mobSection == null) continue;

            int globalMinDrops = mobSection.getInt("min-drops", 0);
            int globalMaxDrops = mobSection.getInt("max-drops", globalMinDrops);

            List<LootItem> lootItems = new ArrayList<>();

            if (mobSection.isList("loot")) {
                List<String> lootList = mobSection.getStringList("loot");
                for (String lootLine : lootList) {
                    LootItem item = parseNewFormatLoot(lootLine);
                    if (item != null) {
                        lootItems.add(item);
                    }
                }
            } else {

                ConfigurationSection lootSection = mobSection.getConfigurationSection("loot");
                if (lootSection != null) {
                    for (String lootId : lootSection.getKeys(false)) {
                        ConfigurationSection itemSection = lootSection.getConfigurationSection(lootId);
                        if (itemSection == null) continue;

                        String type = itemSection.getString("type", "VANILLA");
                        String itemId = itemSection.getString("item");
                        double chance = itemSection.getDouble("chance", 100.0);
                        int amount = itemSection.getInt("amount", 1);
                        boolean unidentified = itemSection.getBoolean("unidentified", false);


                        int itemMinDrops = itemSection.getInt("min-drops", 0);
                        int itemMaxDrops = itemSection.getInt("max-drops", itemMinDrops);

                        if (itemId != null) {
                            lootItems.add(new LootItem(type, itemId, chance, amount, unidentified, itemMinDrops, itemMaxDrops));
                        }
                    }
                }
            }

            if (!lootItems.isEmpty()) {
                MobLootConfig mobLootConfig = new MobLootConfig(mobId, lootItems, globalMinDrops, globalMaxDrops);
                mobLootMap.put(mobId, mobLootConfig);
                mobCount++;
                plugin.getLogger().info("Loaded " + lootItems.size() + " loot items for mob '" + mobId + "' from " + source +
                        " (min-drops: " + globalMinDrops + ", max-drops: " + globalMaxDrops + ")");
            }
        }

        return mobCount;
    }

    private LootItem parseNewFormatLoot(String lootLine) {
        try {
            Matcher matcher = NEW_FORMAT_PATTERN.matcher(lootLine.trim());
            if (!matcher.matches()) {
                plugin.getLogger().warning("Invalid loot format: " + lootLine);
                return null;
            }

            String lootType = matcher.group(1).toUpperCase();
            String parameters = matcher.group(2);
            String amountStr = matcher.group(3); // Може бути "3", "2-5", або "0to3"
            double chanceDecimal = Double.parseDouble(matcher.group(4));
            double chance = chanceDecimal * 100.0;

            // Парсинг параметрів з фігурних дужок
            Map<String, String> params = parseParameters(parameters);

            String itemId = null;
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

            boolean unidentified = Boolean.parseBoolean(params.getOrDefault("unidentified", "false"));

            // Парсинг min-drops та max-drops з нового формату або старого
            int minDrops, maxDrops;

            // Спочатку перевіряємо чи є параметри в старому форматі (в фігурних дужках)
            if (params.containsKey("min-drops") || params.containsKey("max-drops")) {
                // Старий формат - беремо з параметрів
                minDrops = Integer.parseInt(params.getOrDefault("min-drops", "1"));
                maxDrops = Integer.parseInt(params.getOrDefault("max-drops", String.valueOf(minDrops)));
            } else {
                // Новий формат - парсимо з amountStr
                int[] dropRange = parseDropRange(amountStr);
                minDrops = dropRange[0];
                maxDrops = dropRange[1];
            }

            // amount для старої логіки (кількість предметів, не дропів)
            int amount = 1; // За замовчуванням 1

            return new LootItem(lootType, itemId, chance, amount, unidentified, minDrops, maxDrops);

        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing loot line '" + lootLine + "': " + e.getMessage());
            return null;
        }
    }

    private int[] parseDropRange(String amountStr) {
        try {
            // Перевіряємо формат "X-Y"
            if (amountStr.contains("-")) {
                String[] parts = amountStr.split("-", 2);
                int min = Integer.parseInt(parts[0]);
                int max = Integer.parseInt(parts[1]);
                return new int[]{min, max};
            }

            // Перевіряємо формат "XtoY"
            if (amountStr.contains("to")) {
                String[] parts = amountStr.split("to", 2);
                int min = Integer.parseInt(parts[0]);
                int max = Integer.parseInt(parts[1]);
                return new int[]{min, max};
            }

            // Одне число - і мін, і макс однакові
            int value = Integer.parseInt(amountStr);
            return new int[]{value, value};

        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing drop range '" + amountStr + "': " + e.getMessage());
            // За замовчуванням повертаємо 1-1
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
        MobLootConfig config = mobLootMap.get(mobId);
        return config != null ? config.getLootItems() : null;
    }

    public MobLootConfig getMobLootConfig(String mobId) {
        return mobLootMap.get(mobId);
    }

    public boolean hasMobLoot(String mobId) {
        MobLootConfig config = mobLootMap.get(mobId);
        return config != null && !config.getLootItems().isEmpty();
    }

    public List<ItemStack> generateLoot(String mobId) {
        MobLootConfig mobLootConfig = mobLootMap.get(mobId);
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
            int maxAttempts = availableItems.size() * 3; // Максимум спроб

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

            List<ItemStack> itemDrops = createItemDrops(selectedItem);
            items.addAll(itemDrops);

            availableItems.remove(selectedItem);
        }

        return items;
    }

    private List<ItemStack> createItemDrops(LootItem lootItem) {
        List<ItemStack> drops = new ArrayList<>();

        int dropCount = ThreadLocalRandom.current().nextInt(
                lootItem.getMinDrops(),
                lootItem.getMaxDrops() + 1
        );

        for (int i = 0; i < dropCount; i++) {
            ItemStack item = createItemStack(lootItem);
            if (item != null) {
                drops.add(item);
            }
        }

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

}