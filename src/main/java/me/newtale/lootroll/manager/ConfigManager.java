package me.newtale.lootroll.manager;

import me.newtale.lootroll.LootRoll;
import org.bukkit.plugin.java.JavaPlugin;

import me.newtale.lootroll.config.ConfigValidator;
import me.newtale.lootroll.config.MessagesConfig;
import me.newtale.lootroll.config.MobDropConfig;
import me.newtale.lootroll.config.PluginConfig;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final LootRoll plugin;
    private final ConfigValidator configValidator;
    private PluginConfig config;
    private final Map<String, Map<String, MobDropConfig>> dropConfigs;
    private final File dropsFolder;

    public ConfigManager(LootRoll plugin) {
        this.plugin = plugin;
        this.configValidator = new ConfigValidator(plugin);
        this.dropConfigs = new HashMap<>();

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try {
            this.config = configValidator.validateAndUpdateConfig(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load config.yml: " + e.getMessage());
            e.printStackTrace();
            this.config = new PluginConfig();
        }

        this.dropsFolder = new File(plugin.getDataFolder(), "drops");
        if (!dropsFolder.exists()) {
            dropsFolder.mkdirs();
            createExampleDropConfigs();
        }

        loadDropConfigs();
    }

    public void reloadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try {
            this.config = configValidator.validateAndUpdateConfig(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to reload config.yml: " + e.getMessage());
            e.printStackTrace();
        }

        loadDropConfigs();
    }

    public PluginConfig getConfig() {
        return config;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public String getMessage(String path, String defaultMessage) {
        String message = getMessageByPath(path);
        return message != null ? message : defaultMessage;
    }

    public String getMessageByPath(String path) {
        if (config == null || config.getMessages() == null) {
            return null;
        }

        MessagesConfig messages = config.getMessages();
        
        String camelCasePath = kebabToCamel(path);
        
        try {
            java.lang.reflect.Field field = messages.getClass().getDeclaredField(camelCasePath);
            field.setAccessible(true);
            Object value = field.get(messages);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String kebabToCamel(String kebab) {
        StringBuilder camel = new StringBuilder();
        boolean nextUpperCase = false;
        for (char c : kebab.toCharArray()) {
            if (c == '-') {
                nextUpperCase = true;
            } else {
                camel.append(nextUpperCase ? Character.toUpperCase(c) : c);
                nextUpperCase = false;
            }
        }
        return camel.toString();
    }

    public double getRollDistance() {
        return config != null ? config.getRollDistance() : 100.0;
    }

    public int getRollTime() {
        return config != null ? config.getRollTime() : 30;
    }

    public String getPartySystem() {
        return config != null ? config.getPartySystem() : "parties";
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
                Map<String, MobDropConfig> mobConfigs = configValidator.validateDropConfig(file);
                String fileName = file.getName().replace(".yml", "");
                dropConfigs.put(fileName, mobConfigs);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load drop configuration: " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    public Map<String, Map<String, MobDropConfig>> getDropConfigs() {
        return dropConfigs;
    }

    public Map<String, MobDropConfig> getDropConfig(String fileName) {
        return dropConfigs.get(fileName);
    }

    private void createExampleDropConfigs() {
        createExample();
    }

    private void createExample() {
        File exampleFile = new File(dropsFolder, "example.yml");
        if (!exampleFile.exists()) {
            try {
                Map<String, MobDropConfig> exampleConfigs = new HashMap<>();

                // Example 1: dark_forest_spider
                MobDropConfig spiderConfig = getMobDropConfig();
                exampleConfigs.put("dark_forest_spider", spiderConfig);

                // Example 2: fire_elemental
                MobDropConfig elementalConfig = new MobDropConfig();
                elementalConfig.setMinDrops(2);
                elementalConfig.setMaxDrops(4);
                elementalConfig.setLoot(java.util.Arrays.asList(
                        "mmoitems{type=MATERIAL;item=FIRE_CRYSTAL} 1-3 .6",
                        "mmoitems{type=WEAPON;item=FLAME_SWORD;unidentified=true} 1 .15",
                        "vanilla{item=blaze_powder} 5 .9",
                        "mythicmobs{item=ElementalCore} 0to2 .25"
                ));
                exampleConfigs.put("fire_elemental", elementalConfig);

                // Example 3: ancient_dragon
                MobDropConfig dragonConfig = new MobDropConfig();
                dragonConfig.setMinDrops(3);
                dragonConfig.setMaxDrops(6);
                dragonConfig.setLoot(java.util.Arrays.asList(
                        "mmoitems{type=MATERIAL;item=DRAGON_SCALE} 5-10 .95",
                        "mmoitems{type=WEAPON;item=LEGENDARY_SWORD;unidentified=true} 1 .2",
                        "mmoitems{type=ARMOR;item=DRAGON_HELMET;unidentified=true} 1 .15",
                        "mmoitems{type=CONSUMABLE;item=DRAGON_HEART} 1 .1",
                        "vanilla{item=diamond} 5to15 .8",
                        "mythicmobs{item=Ancient_Rune} 1 .3"
                ));
                exampleConfigs.put("ancient_dragon", dragonConfig);

                configValidator.saveDropConfig(exampleFile, exampleConfigs);
                plugin.getLogger().info("Created example configuration: example.yml");
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create example configuration: " + e.getMessage());
            }
        }
    }

    @NotNull
    private static MobDropConfig getMobDropConfig() {
        MobDropConfig spiderConfig = new MobDropConfig();
        spiderConfig.setMinDrops(1);
        spiderConfig.setMaxDrops(2);
        spiderConfig.setLoot(java.util.Arrays.asList(
                "mmoitems{type=ARMOR;item=MAGE_CROWN;unidentified=true} 1 .04",
                "mmoitems{type=MATERIAL;item=SPIDER_SILK} 2-5 .8",
                "mmoitems{type=MATERIAL;item=WEB_ESSENCE} 1to3 .6",
                "mmoitems{type=MATERIAL;item=RARE_SILK} 1 .2",
                "vanilla{item=iron_sword} 1 .5",
                "mythicmobs{item=DarkBlade} 1 .1"
        ));
        return spiderConfig;
    }
}
