package me.newtale.lootRoll.utils;

import org.bukkit.inventory.ItemStack;

public class ItemUtils {

    public static String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name().toLowerCase().replace("_", " ");
    }

    public static String getItemDisplayNameWithoutColors(ItemStack item) {
        String displayName;

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            displayName = item.getItemMeta().getDisplayName();
        } else {
            displayName = item.getType().name().toLowerCase().replace("_", " ");
        }

        return MessageUtils.stripMiniMessageColors(displayName);
    }
}