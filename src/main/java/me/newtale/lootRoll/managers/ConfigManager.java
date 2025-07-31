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
    private Map<String, FileConfiguration> dropConfigs;
    private File dropsFolder;

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

    public String getMessage(String path, String defaultMessage) {
        return config.getString("messages." + path, defaultMessage);
    }

    public double getRollDistance() {
        return config.getDouble("roll-distance", 100.0);
    }

    public int getRollTime() {
        return config.getInt("roll-time", 30);
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
        File exampleFile = new File(dropsFolder, "new-example.yml");
        if (!exampleFile.exists()) {
            try {
                exampleFile.createNewFile();
                FileConfiguration newConfig = YamlConfiguration.loadConfiguration(exampleFile);

                // Example with new format
                newConfig.set("dark_forest_spider.min-drops", 1);
                newConfig.set("dark_forest_spider.max-drops", 2);

                List<String> spiderLoot = Arrays.asList(
                        "mmoitems{type=ARMOR;item=MAGE_CROWN;unidentified=true;min-drops=1;max-drops=2} 1 .04",
                        "mmoitems{type=MATERIAL;item=SPIDER_SILK;min-drops=2;max-drops=5} 3 .8",
                        "vanilla{item=IRON_SWORD} 1 .5",
                        "mythicmobs{item=DARK_BLADE;min-drops=1;max-drops=1} 1 .1"
                );
                newConfig.set("dark_forest_spider.loot", spiderLoot);

                // Another example with different mob
                newConfig.set("fire_elemental.min-drops", 2);
                newConfig.set("fire_elemental.max-drops", 4);

                List<String> elementalLoot = Arrays.asList(
                        "mmoitems{type=MATERIAL;item=FIRE_CRYSTAL;min-drops=1;max-drops=3} 2 .6",
                        "mmoitems{type=WEAPON;item=FLAME_SWORD;unidentified=true} 1 .15",
                        "vanilla{item=BLAZE_POWDER} 5 .9",
                        "mythicmobs{item=ELEMENTAL_CORE} 1 .25"
                );
                newConfig.set("fire_elemental.loot", elementalLoot);

                // Boss example
                newConfig.set("ancient_dragon.min-drops", 3);
                newConfig.set("ancient_dragon.max-drops", 6);

                List<String> dragonLoot = Arrays.asList(
                        "mmoitems{type=MATERIAL;item=DRAGON_SCALE;min-drops=5;max-drops=10} 1 .95",
                        "mmoitems{type=WEAPON;item=LEGENDARY_SWORD;unidentified=true} 1 .2",
                        "mmoitems{type=ARMOR;item=DRAGON_HELMET;unidentified=true} 1 .15",
                        "mmoitems{type=CONSUMABLE;item=DRAGON_HEART} 1 .1",
                        "vanilla{item=DIAMOND} 10 .8",
                        "mythicmobs{item=ANCIENT_RUNE} 1 .3"
                );
                newConfig.set("ancient_dragon.loot", dragonLoot);

                newConfig.save(exampleFile);
                plugin.getLogger().info("Created new format example configuration: new-format-example.yml");
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create new format example configuration: " + e.getMessage());
            }
        }
    }
}