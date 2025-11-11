package me.newtale.lootroll.util;

import me.newtale.lootroll.manager.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import me.newtale.lootroll.LootRoll;

public class ItemUtils {

    private static NamespacedKey LOOT_TYPE_KEY;
    private static NamespacedKey LOOT_AMOUNT_KEY;

    public static void initialize(LootRoll plugin) {
        LOOT_TYPE_KEY = new NamespacedKey(plugin, "loot_type");
        LOOT_AMOUNT_KEY = new NamespacedKey(plugin, "loot_amount");
    }

    public static ItemStack createExpItemStack(int amount, ConfigManager configManager) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(LOOT_TYPE_KEY, PersistentDataType.STRING, "EXP");
            pdc.set(LOOT_AMOUNT_KEY, PersistentDataType.INTEGER, amount);
            
            // Set display name using placeholder from config
            if (configManager != null) {
                String placeholder = configManager.getMessage("exp-placeholder", "<green><amount> EXP</green>");
                MiniMessage miniMessage = MiniMessage.miniMessage();
                Component displayName = miniMessage.deserialize(placeholder, Placeholder.unparsed("amount", String.valueOf(amount)));
                meta.displayName(displayName);
            }
            
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createMoneyItemStack(int amount, ConfigManager configManager) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(LOOT_TYPE_KEY, PersistentDataType.STRING, "MONEY");
            pdc.set(LOOT_AMOUNT_KEY, PersistentDataType.INTEGER, amount);
            
            // Set display name using placeholder from config
            if (configManager != null) {
                String placeholder = configManager.getMessage("money-placeholder", "<gold><amount></gold>");
                MiniMessage miniMessage = MiniMessage.miniMessage();
                Component displayName = miniMessage.deserialize(placeholder, Placeholder.unparsed("amount", String.valueOf(amount)));
                meta.displayName(displayName);
            }
            
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isExpLoot(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(LOOT_TYPE_KEY, PersistentDataType.STRING) && 
               "EXP".equals(pdc.get(LOOT_TYPE_KEY, PersistentDataType.STRING));
    }

    public static boolean isMoneyLoot(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(LOOT_TYPE_KEY, PersistentDataType.STRING) && 
               "MONEY".equals(pdc.get(LOOT_TYPE_KEY, PersistentDataType.STRING));
    }

    public static int getLootAmount(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(LOOT_AMOUNT_KEY, PersistentDataType.INTEGER)) {
            return pdc.get(LOOT_AMOUNT_KEY, PersistentDataType.INTEGER);
        }
        return item.getAmount();
    }

    public static Component getItemDisplayComponent(ItemStack item) {
        return getItemDisplayComponent(item, null);
    }

    public static Component getItemDisplayComponent(ItemStack item, ConfigManager configManager) {
        if (item == null) {
            return Component.text("Unknown");
        }

        // Check for exp loot
        if (isExpLoot(item)) {
            int amount = getLootAmount(item);
            if (configManager != null) {
                String placeholder = configManager.getMessage("exp-placeholder", "<green><amount> EXP</green>");
                MiniMessage miniMessage = MiniMessage.miniMessage();
                return miniMessage.deserialize(placeholder, Placeholder.unparsed("amount", String.valueOf(amount)));
            }
            return Component.text(amount + " EXP");
        }

        // Check for money loot
        if (isMoneyLoot(item)) {
            int amount = getLootAmount(item);
            if (configManager != null) {
                String placeholder = configManager.getMessage("money-placeholder", "<gold><amount></gold>");
                MiniMessage miniMessage = MiniMessage.miniMessage();
                return miniMessage.deserialize(placeholder, Placeholder.unparsed("amount", String.valueOf(amount)));
            }
            return Component.text(amount + " Money");
        }

        // Regular item handling
        Component baseComponent;
        
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                Component displayName = meta.displayName();
                if (displayName != null) {
                    baseComponent = displayName;
                } else {
                    baseComponent = getDefaultItemComponent(item);
                }
            } else {
                baseComponent = getDefaultItemComponent(item);
            }
        } else {
            baseComponent = getDefaultItemComponent(item);
        }
        
        // Add amount prefix if more than 1
        int amount = item.getAmount();
        if (amount > 1) {
            return Component.text(amount + "x ").append(baseComponent);
        }
        
        return baseComponent;
    }
    
    private static Component getDefaultItemComponent(ItemStack item) {
        Material material = item.getType();
        if (material.isItem() && material != Material.AIR) {
            try {
                String translationKey = material.translationKey();
                return Component.translatable(translationKey);
            } catch (Exception e) {
                return Component.text(material.name().toLowerCase().replace("_", " "));
            }
        }
        
        return Component.text(item.getType().name().toLowerCase().replace("_", " "));
    }

    public static String getItemDisplayName(ItemStack item) {
        Component component = getItemDisplayComponent(item);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public static String getItemDisplayName(ItemStack item, ConfigManager configManager) {
        Component component = getItemDisplayComponent(item, configManager);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public static String getItemDisplayNameWithoutColors(ItemStack item) {
        String displayName = getItemDisplayName(item);
        return MessageUtils.stripMiniMessageColors(displayName);
    }

    public static String getItemDisplayNameWithoutColors(ItemStack item, ConfigManager configManager) {
        String displayName = getItemDisplayName(item, configManager);
        return MessageUtils.stripMiniMessageColors(displayName);
    }
}