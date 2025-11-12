package me.newtale.lootroll.common.config;

import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.lang.reflect.Field;
import java.util.*;

public class ConfigValidator {
    
    private static final int CURRENT_CONFIG_VERSION = 1;
    private final JavaPlugin plugin;
    private final Yaml yaml;
    
    public ConfigValidator(JavaPlugin plugin) {
        this.plugin = plugin;
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.DOUBLE_QUOTED);
        options.setSplitLines(false);
        options.setWidth(Integer.MAX_VALUE);
        
        Representer representer = new Representer(options) {
            @Override
            protected Node representScalar(org.yaml.snakeyaml.nodes.Tag tag, String value, DumperOptions.ScalarStyle style) {
                if (value != null && tag == org.yaml.snakeyaml.nodes.Tag.STR) {
                    style = DumperOptions.ScalarStyle.DOUBLE_QUOTED;
                }
                return super.representScalar(tag, value, style);
            }
        };
        
        this.yaml = new Yaml(representer, options);
    }
    
    public PluginConfig validateAndUpdateConfig(File configFile) throws IOException {
        PluginConfig config;
        
        if (!configFile.exists()) {
            config = new PluginConfig();
            config.setConfigVersion(CURRENT_CONFIG_VERSION);
            saveConfig(configFile, config);
            plugin.getLogger().info("Created new config.yml with default values");
            return config;
        }
        
        try {
            Map<String, Object> data = loadYaml(configFile);
            config = mapToPluginConfig(data);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load config.yml, creating backup and using defaults: " + e.getMessage());
            File backupFile = new File(configFile.getParentFile(), "config.yml.backup");
            if (configFile.exists()) {
                Files.copy(configFile.toPath(), backupFile.toPath());
            }
            config = new PluginConfig();
            config.setConfigVersion(CURRENT_CONFIG_VERSION);
            saveConfig(configFile, config);
            return config;
        }
        
        int configVersion = config.getConfigVersion();
        if (configVersion < CURRENT_CONFIG_VERSION) {
            plugin.getLogger().info("Config version " + configVersion + " is outdated. Updating to version " + CURRENT_CONFIG_VERSION);
            config = updateConfig(config, configVersion, CURRENT_CONFIG_VERSION);
            config.setConfigVersion(CURRENT_CONFIG_VERSION);
            saveConfig(configFile, config);
        } else if (configVersion > CURRENT_CONFIG_VERSION) {
            plugin.getLogger().warning("Config version " + configVersion + " is newer than supported version " + CURRENT_CONFIG_VERSION + ". Some features may not work correctly.");
        }
        
        config = validateConfig(config);
        saveConfig(configFile, config);
        
        return config;
    }
    
    private Map<String, Object> loadYaml(File file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return yaml.load(inputStream);
        }
    }
    
    private PluginConfig mapToPluginConfig(Map<String, Object> data) {
        PluginConfig config = new PluginConfig();
        
        if (data.containsKey("config-version")) {
            Object value = data.get("config-version");
            if (value instanceof Number) {
                config.setConfigVersion(((Number) value).intValue());
            }
        }
        
        if (data.containsKey("party-system")) {
            Object value = data.get("party-system");
            if (value != null) {
                config.setPartySystem(value.toString());
            }
        }
        
        if (data.containsKey("roll-distance")) {
            Object value = data.get("roll-distance");
            if (value instanceof Number) {
                config.setRollDistance(((Number) value).doubleValue());
            }
        }
        
        if (data.containsKey("roll-time")) {
            Object value = data.get("roll-time");
            if (value instanceof Number) {
                config.setRollTime(((Number) value).intValue());
            }
        }
        
        if (data.containsKey("messages")) {
            Object messagesData = data.get("messages");
            if (messagesData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> messagesMap = (Map<String, Object>) messagesData;
                config.setMessages(mapToMessagesConfig(messagesMap));
            }
        }
        
        return config;
    }
    
    private MessagesConfig mapToMessagesConfig(Map<String, Object> data) {
        MessagesConfig messages = new MessagesConfig();
        
        try {
            Field[] fields = MessagesConfig.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = getYamlFieldName(field);
                
                if (data.containsKey(fieldName)) {
                    Object value = data.get(fieldName);
                    if (value != null) {
                        field.set(messages, value.toString());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error mapping messages config: " + e.getMessage());
        }
        
        return messages;
    }
    
    private String getYamlFieldName(Field field) {
        String fieldName = field.getName();
        return camelToKebab(fieldName);
    }
    
    private String camelToKebab(String camel) {
        if (camel == null || camel.isEmpty()) {
            return camel;
        }
        StringBuilder kebab = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    kebab.append('-');
                }
                kebab.append(Character.toLowerCase(c));
            } else {
                kebab.append(c);
            }
        }
        return kebab.toString();
    }
    
    private PluginConfig validateConfig(PluginConfig config) {
        PluginConfig defaultConfig = new PluginConfig();
        boolean updated = false;
        
        if (config.getPartySystem() == null || config.getPartySystem().isEmpty()) {
            config.setPartySystem(defaultConfig.getPartySystem());
            updated = true;
        }
        
        if (config.getRollDistance() <= 0) {
            config.setRollDistance(defaultConfig.getRollDistance());
            updated = true;
        }
        
        if (config.getRollTime() <= 0) {
            config.setRollTime(defaultConfig.getRollTime());
            updated = true;
        }
        
        if (config.getMessages() == null) {
            config.setMessages(new MessagesConfig());
            updated = true;
        } else {
            MessagesConfig messages = config.getMessages();
            MessagesConfig defaultMessages = defaultConfig.getMessages();
            
            updated |= validateMessages(messages, defaultMessages);
        }
        
        if (updated) {
            plugin.getLogger().info("Added missing fields to config.yml with default values");
        }
        
        return config;
    }
    
    private boolean validateMessages(MessagesConfig messages, MessagesConfig defaultMessages) {
        boolean updated = false;
        
        try {
            Field[] fields = MessagesConfig.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(messages);
                
                if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                    Object defaultValue = field.get(defaultMessages);
                    field.set(messages, defaultValue);
                    updated = true;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error validating messages: " + e.getMessage());
        }
        
        return updated;
    }
    
    private PluginConfig updateConfig(PluginConfig config, int fromVersion, int toVersion) {
        return validateConfig(config);
    }
    
    public Map<String, MobDropConfig> validateDropConfig(File dropFile) throws IOException {
        Map<String, MobDropConfig> mobConfigs = new HashMap<>();
        
        if (!dropFile.exists()) {
            return mobConfigs;
        }
        
        try {
            Map<String, Object> rawConfig = loadYaml(dropFile);
            
            for (Map.Entry<String, Object> entry : rawConfig.entrySet()) {
                String mobId = entry.getKey();
                Object mobData = entry.getValue();
                
                if (mobData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mobMap = (Map<String, Object>) mobData;
                    
                    MobDropConfig mobConfig = convertToMobDropConfig(mobMap);
                    mobConfigs.put(mobId, mobConfig);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load drop config " + dropFile.getName() + ": " + e.getMessage());
        }
        
        return mobConfigs;
    }
    
    private MobDropConfig convertToMobDropConfig(Map<String, Object> mobMap) {
        MobDropConfig config = new MobDropConfig();
        
        if (mobMap.containsKey("min-drops")) {
            Object value = mobMap.get("min-drops");
            if (value instanceof Number) {
                config.setMinDrops(((Number) value).intValue());
            }
        }
        
        if (mobMap.containsKey("max-drops")) {
            Object value = mobMap.get("max-drops");
            if (value instanceof Number) {
                config.setMaxDrops(((Number) value).intValue());
            }
        }
        
        if (mobMap.containsKey("override-vanilla-drops")) {
            Object value = mobMap.get("override-vanilla-drops");
            if (value instanceof Boolean) {
                config.setOverrideVanillaDrops((Boolean) value);
            }
        }
        
        if (mobMap.containsKey("process-vanilla-drops")) {
            Object value = mobMap.get("process-vanilla-drops");
            if (value instanceof Boolean) {
                config.setProcessVanillaDrops((Boolean) value);
            }
        }
        
        if (mobMap.containsKey("loot")) {
            Object lootValue = mobMap.get("loot");
            if (lootValue instanceof List) {
                List<String> lootList = new ArrayList<>();
                for (Object item : (List<?>) lootValue) {
                    if (item instanceof String) {
                        lootList.add((String) item);
                    }
                }
                config.setLoot(lootList);
            }
        }
        
        return config;
    }
    
    public void saveConfig(File configFile, PluginConfig config) throws IOException {
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("# Configuration version - DO NOT MODIFY\n");
            writer.write("config-version: " + config.getConfigVersion() + "\n\n");
            writer.write("# Party system to use: 'parties' or 'mmocore'\n");
            writer.write("# parties - Uses the Parties plugin\n");
            writer.write("# mmocore - Uses MMOCore party system\n");
            writer.write("party-system: \"" + config.getPartySystem() + "\"\n\n");
            writer.write("# Distance in blocks in which players can roll for items\n");
            writer.write("roll-distance: " + config.getRollDistance() + "\n\n");
            writer.write("# Time in seconds players have to roll for items\n");
            writer.write("roll-time: " + config.getRollTime() + "\n\n");
            writer.write("# Messages\n");
            writer.write("messages:\n");
            
            Map<String, Object> messages = messagesConfigToMap(config.getMessages());
            for (Map.Entry<String, Object> entry : messages.entrySet()) {
                String value = entry.getValue().toString();
                value = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
                writer.write("  " + entry.getKey() + ": \"" + value + "\"\n");
            }
        }
    }
    
    public void saveDropConfig(File dropFile, Map<String, MobDropConfig> mobConfigs) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        for (Map.Entry<String, MobDropConfig> entry : mobConfigs.entrySet()) {
            data.put(entry.getKey(), mobDropConfigToMap(entry.getValue()));
        }
        try (FileWriter writer = new FileWriter(dropFile)) {
            yaml.dump(data, writer);
        }
    }
    
    private Map<String, Object> pluginConfigToMap(PluginConfig config) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("config-version", config.getConfigVersion());
        data.put("party-system", config.getPartySystem());
        data.put("roll-distance", config.getRollDistance());
        data.put("roll-time", config.getRollTime());
        data.put("messages", messagesConfigToMap(config.getMessages()));
        return data;
    }
    
    
    private Map<String, Object> messagesConfigToMap(MessagesConfig messages) {
        Map<String, Object> data = new LinkedHashMap<>();
        
        try {
            Field[] fields = MessagesConfig.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = getYamlFieldName(field);
                Object value = field.get(messages);
                if (value != null) {
                    data.put(fieldName, value);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error converting messages config to map: " + e.getMessage());
        }
        
        return data;
    }
    
    private Map<String, Object> mobDropConfigToMap(MobDropConfig config) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("min-drops", config.getMinDrops());
        data.put("max-drops", config.getMaxDrops());
        data.put("override-vanilla-drops", config.isOverrideVanillaDrops());
        data.put("process-vanilla-drops", config.isProcessVanillaDrops());
        data.put("loot", config.getLoot());
        return data;
    }
}
