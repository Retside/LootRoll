package me.newtale.lootRoll.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private final Map<String, FileConfiguration> dropConfigs;
    private final File dropsFolder;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dropConfigs = new HashMap<>();

        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();

        this.dropsFolder = new File(plugin.getDataFolder(), "drops");
        if (!dropsFolder.exists()) {
            dropsFolder.mkdirs();
            createExampleDropConfigs();
        }

        loadDropConfigs();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        loadDropConfigs();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    // Добавляем getter для plugin
    public JavaPlugin getPlugin() {
        return plugin;
    }

    public String getMessage(String path, String defaultMessage) {
        return config.getString("messages." + path, defaultMessage);
    }

    public double getRollDistance() {
        return config.getDouble("roll-distance", 100.0);
    }

    public int getRollTime() {
        return config.getInt("roll-time", 30);
    }

    // Добавляем метод для получения типа party системы
    public String getPartySystem() {
        return config.getString("party-system", "parties");
    }

    private void loadDropConfigs() {
        dropConfigs.clear();

        if (!dropsFolder.exists()) {
            return;
        }

        File[] files = dropsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                FileConfiguration dropConfig = YamlConfiguration.loadConfiguration(file);
                String fileName = file.getName().replace(".yml", "");
                dropConfigs.put(fileName, dropConfig);
                plugin.getLogger().info("Loaded drop configuration: " + fileName);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load drop configuration: " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    public Map<String, FileConfiguration> getDropConfigs() {
        return dropConfigs;
    }

    public FileConfiguration getDropConfig(String fileName) {
        return dropConfigs.get(fileName);
    }

    private void createExampleDropConfigs() {
        createExample();
    }

    private void createExample() {
        File exampleFile = new File(dropsFolder, "example.yml");
        if (!exampleFile.exists()) {
            try {
                exampleFile.createNewFile();
                FileConfiguration newConfig = YamlConfiguration.loadConfiguration(exampleFile);

                // Example with new format
                newConfig.set("dark_forest_spider.min-drops", 1);
                newConfig.set("dark_forest_spider.max-drops", 2);

                List<String> spiderLoot = Arrays.asList(
                        "mmoitems{type=ARMOR;item=MAGE_CROWN;unidentified=true} 1 .04", // Старий формат
                        "mmoitems{type=MATERIAL;item=SPIDER_SILK} 2-5 .8", // Новий формат з дефісом
                        "mmoitems{type=MATERIAL;item=WEB_ESSENCE} 1to3 .6", // Новий формат з "to"
                        "mmoitems{type=MATERIAL;item=RARE_SILK} 1 .2", // Новий формат - одне число
                        "vanilla{item=IRON_SWORD} 1 .5",
                        "mythicmobs{item=DARK_BLADE} 1 .1"
                );
                newConfig.set("dark_forest_spider.loot", spiderLoot);

                // Another example with different mob
                newConfig.set("fire_elemental.min-drops", 2);
                newConfig.set("fire_elemental.max-drops", 4);

                List<String> elementalLoot = Arrays.asList(
                        "mmoitems{type=MATERIAL;item=FIRE_CRYSTAL} 1-3 .6", // Новий формат
                        "mmoitems{type=WEAPON;item=FLAME_SWORD;unidentified=true} 1 .15",
                        "vanilla{item=BLAZE_POWDER} 5 .9",
                        "mythicmobs{item=ELEMENTAL_CORE} 0to2 .25" // Новий формат з "to"
                );
                newConfig.set("fire_elemental.loot", elementalLoot);

                // Boss example
                newConfig.set("ancient_dragon.min-drops", 3);
                newConfig.set("ancient_dragon.max-drops", 6);

                List<String> dragonLoot = Arrays.asList(
                        "mmoitems{type=MATERIAL;item=DRAGON_SCALE} 5-10 .95", // Новий формат
                        "mmoitems{type=WEAPON;item=LEGENDARY_SWORD;unidentified=true} 1 .2",
                        "mmoitems{type=ARMOR;item=DRAGON_HELMET;unidentified=true} 1 .15",
                        "mmoitems{type=CONSUMABLE;item=DRAGON_HEART} 1 .1",
                        "vanilla{item=DIAMOND} 5to15 .8", // Новий формат з "to"
                        "mythicmobs{item=ANCIENT_RUNE} 1 .3" // Старий формат
                );
                newConfig.set("ancient_dragon.loot", dragonLoot);

                newConfig.save(exampleFile);
                plugin.getLogger().info("Created example configuration: example.yml");
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create example configuration: " + e.getMessage());
            }
        }
    }
}