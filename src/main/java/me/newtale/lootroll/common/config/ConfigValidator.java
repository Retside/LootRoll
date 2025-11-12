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
            return config;
        }
        
        try {
            List<String> fileLines = Files.readAllLines(configFile.toPath());
            Map<String, Object> data = loadYaml(configFile);
            validateUnknownProperties(data, configFile.getName(), fileLines);
            config = mapToPluginConfig(data, fileLines);
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
    
    private PluginConfig mapToPluginConfig(Map<String, Object> data, List<String> fileLines) {
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
                validateUnknownMessageProperties(messagesMap, fileLines);
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
            List<String> fileLines = Files.readAllLines(dropFile.toPath());
            Map<String, Object> rawConfig = loadYaml(dropFile);
            
            for (Map.Entry<String, Object> entry : rawConfig.entrySet()) {
                String mobId = entry.getKey();
                Object mobData = entry.getValue();
                
                if (mobData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mobMap = (Map<String, Object>) mobData;
                    
                    int mobLineNumber = findLineNumber(fileLines, mobId);
                    MobDropConfig mobConfig = convertToMobDropConfig(mobMap, dropFile.getName(), mobLineNumber);
                    mobConfigs.put(mobId, mobConfig);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load drop config " + dropFile.getName() + ": " + e.getMessage());
        }
        
        return mobConfigs;
    }
    
    private int findLineNumber(List<String> lines, String key) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.startsWith(key + ":") || line.equals(key + ":")) {
                return i + 1;
            }
        }
        return -1;
    }
    
    private MobDropConfig convertToMobDropConfig(Map<String, Object> mobMap, String fileName, int baseLineNumber) {
        validateUnknownDropProperties(mobMap, fileName, baseLineNumber);
        
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
                @SuppressWarnings("unchecked")
                List<Object> lootItems = (List<Object>) lootValue;
                
                try {
                    List<String> fileLines = Files.readAllLines(new File(plugin.getDataFolder(), "drops/" + fileName).toPath());
                    int lootLineNumber = findLootLineNumber(fileLines, mobMap, baseLineNumber);
                    
                    for (int i = 0; i < lootItems.size(); i++) {
                        Object item = lootItems.get(i);
                        if (item instanceof String) {
                            String lootLine = (String) item;
                            int actualLineNumber = findSpecificLootLineNumber(fileLines, lootLine, lootLineNumber, i);
                            validateLootType(lootLine, fileName, actualLineNumber);
                            lootList.add(lootLine);
                        }
                    }
                } catch (Exception e) {
                    for (Object item : lootItems) {
                        if (item instanceof String) {
                            String lootLine = (String) item;
                            validateLootType(lootLine, fileName, -1);
                            lootList.add(lootLine);
                        }
                    }
                }
                config.setLoot(lootList);
            }
        }
        
        return config;
    }
    
    private int findLootLineNumber(List<String> lines, Map<String, Object> mobMap, int baseLineNumber) {
        for (int i = baseLineNumber - 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.startsWith("loot:") || line.equals("loot:")) {
                return i + 1;
            }
        }
        return baseLineNumber + 1;
    }
    
    private int findSpecificLootLineNumber(List<String> lines, String lootLine, int lootStartLine, int itemIndex) {
        int foundItems = 0;
        boolean inLootSection = false;
        
        for (int i = lootStartLine - 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            
            if (line.startsWith("loot:") || line.equals("loot:")) {
                inLootSection = true;
                continue;
            }
            
            if (inLootSection && !line.isEmpty() && !line.startsWith(" ") && !line.startsWith("-")) {
                break;
            }
            
            if (inLootSection && line.startsWith("-")) {
                String itemValue = line.substring(1).trim();
                if (itemValue.contains(lootLine.substring(0, Math.min(20, lootLine.length()))) ||
                    lootLine.contains(itemValue.substring(0, Math.min(20, itemValue.length())))) {
                    if (foundItems == itemIndex) {
                        return i + 1;
                    }
                    foundItems++;
                }
            }
        }
        
        return lootStartLine + itemIndex + 1;
    }
    
    private static final Set<String> VALID_LOOT_TYPES = Set.of(
        "MMOITEMS", "MM", "MYTHICMOBS", "VANILLA", "EXP", "MONEY"
    );
    
    private static final Set<String> VALID_CONFIG_PROPERTIES = Set.of(
        "config-version", "party-system", "roll-distance", "roll-time", "messages"
    );
    
    private static final Set<String> VALID_DROP_PROPERTIES = Set.of(
        "min-drops", "max-drops", "override-vanilla-drops", "process-vanilla-drops", "loot"
    );
    
    private void validateUnknownProperties(Map<String, Object> data, String fileName, List<String> fileLines) {
        Set<String> unknownProperties = new HashSet<>();
        for (String key : data.keySet()) {
            if (!VALID_CONFIG_PROPERTIES.contains(key)) {
                unknownProperties.add(key);
            }
        }
        
        if (!unknownProperties.isEmpty()) {
            for (String prop : unknownProperties) {
                int lineNumber = findPropertyLineNumber(fileLines, prop, 0);
                if (lineNumber > 0) {
                    plugin.getLogger().warning("Unknown property '" + prop + "' in " + fileName + " at line " + lineNumber);
                } else {
                    plugin.getLogger().warning("Unknown property '" + prop + "' in " + fileName);
                }
            }
        }
    }
    
    private void validateUnknownMessageProperties(Map<String, Object> messagesMap, List<String> fileLines) {
        try {
            Set<String> validFields = new HashSet<>();
            Field[] fields = MessagesConfig.class.getDeclaredFields();
            for (Field field : fields) {
                validFields.add(getYamlFieldName(field));
            }
            
            Set<String> unknownProperties = new HashSet<>();
            for (String key : messagesMap.keySet()) {
                if (!validFields.contains(key)) {
                    unknownProperties.add(key);
                }
            }
            
            if (!unknownProperties.isEmpty()) {
                int messagesLine = findPropertyLineNumber(fileLines, "messages", 0);
                for (String prop : unknownProperties) {
                    int lineNumber = findPropertyLineNumber(fileLines, prop, messagesLine);
                    if (lineNumber > 0) {
                        plugin.getLogger().warning("Unknown message property '" + prop + "' at line " + lineNumber);
                    } else {
                        plugin.getLogger().warning("Unknown message property '" + prop + "'");
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error validating message properties: " + e.getMessage());
        }
    }
    
    private void validateUnknownDropProperties(Map<String, Object> mobMap, String fileName, int baseLineNumber) {
        Set<String> unknownProperties = new HashSet<>();
        for (String key : mobMap.keySet()) {
            if (!VALID_DROP_PROPERTIES.contains(key)) {
                unknownProperties.add(key);
            }
        }
        
        if (!unknownProperties.isEmpty()) {
            try {
                List<String> fileLines = Files.readAllLines(new File(plugin.getDataFolder(), "drops/" + fileName).toPath());
                for (String prop : unknownProperties) {
                    int lineNumber = findPropertyLineNumber(fileLines, prop, baseLineNumber);
                    if (lineNumber > 0) {
                        plugin.getLogger().warning("Unknown drop property '" + prop + "' in " + fileName + " at line " + lineNumber);
                    } else {
                        plugin.getLogger().warning("Unknown drop property '" + prop + "' in " + fileName);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Unknown drop properties: " + String.join(", ", unknownProperties) + " in " + fileName);
            }
        }
    }
    
    private int findPropertyLineNumber(List<String> lines, String property, int baseLineNumber) {
        for (int i = baseLineNumber - 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.startsWith(property + ":") || line.equals(property + ":")) {
                return i + 1;
            }
        }
        return -1;
    }
    
    private void validateLootType(String lootLine, String fileName, int lineNumber) {
        try {
            String trimmed = lootLine.trim();
            int braceStart = trimmed.indexOf('{');
            if (braceStart == -1) {
                return;
            }
            
            String lootType = trimmed.substring(0, braceStart).trim().toUpperCase();
            
            if (!VALID_LOOT_TYPES.contains(lootType)) {
                String suggestion = findClosestLootType(lootType);
                String location = lineNumber > 0 ? " at line " + lineNumber : "";
                if (suggestion != null) {
                    plugin.getLogger().warning("Unknown loot type '" + lootType + "' in " + fileName + location + ": " + lootLine);
                    plugin.getLogger().warning("Did you mean '" + suggestion + "'?");
                } else {
                    plugin.getLogger().warning("Unknown loot type '" + lootType + "' in " + fileName + location + ": " + lootLine);
                }
            }
        } catch (Exception e) {
        }
    }
    
    private String findClosestLootType(String invalidType) {
        String closest = null;
        int minDistance = Integer.MAX_VALUE;
        
        for (String validType : VALID_LOOT_TYPES) {
            int distance = levenshteinDistance(invalidType, validType);
            if (distance < minDistance && distance <= 2) {
                minDistance = distance;
                closest = validType;
            }
        }
        
        return closest;
    }
    
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1)
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
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
