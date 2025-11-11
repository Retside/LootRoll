package me.newtale.lootroll.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemUtils {

    public static Component getItemDisplayComponent(ItemStack item) {
        Component baseComponent;
        
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
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

    public static String getItemDisplayNameWithoutColors(ItemStack item) {
        String displayName = getItemDisplayName(item);
        return MessageUtils.stripMiniMessageColors(displayName);
    }
}