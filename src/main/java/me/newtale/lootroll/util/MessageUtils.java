package me.newtale.lootroll.util;

import me.newtale.lootroll.manager.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MessageUtils {

    public static Component createLootMessage(String messageTemplate, String itemName, String itemNameNoColors, ItemStack item, ConfigManager configManager, TagResolver... additionalResolvers) {
        return processMessageWithVariables(messageTemplate, itemName, itemNameNoColors, item, configManager, additionalResolvers);
    }

    public static Component processMessageWithVariables(String messageTemplate, String itemName, String itemNameNoColors, ItemStack item, TagResolver... additionalResolvers) {
        return processMessageWithVariables(messageTemplate, itemName, itemNameNoColors, item, null, additionalResolvers);
    }

    public static Component processMessageWithVariables(String messageTemplate, String itemName, String itemNameNoColors, ItemStack item, ConfigManager configManager, TagResolver... additionalResolvers) {
        if (itemNameNoColors != null) {
            messageTemplate = messageTemplate.replace("<item_name>", itemNameNoColors);
        }

        MiniMessage miniMessage = MiniMessage.miniMessage();
        List<TagResolver> resolvers = new ArrayList<>();

        if (item != null) {
            Component itemDisplayComponent = ItemUtils.getItemDisplayComponent(item, configManager);
            Component itemComponent = itemDisplayComponent
                    .hoverEvent(HoverEvent.showItem(item.asHoverEvent().value()));
            resolvers.add(Placeholder.component("item", itemComponent));
        }
        if (additionalResolvers != null) {
            resolvers.addAll(Arrays.asList(additionalResolvers));
        }

        if (resolvers.isEmpty()) {
            return miniMessage.deserialize(messageTemplate);
        } else {
            return miniMessage.deserialize(messageTemplate, TagResolver.resolver(resolvers));
        }
    }

    public static String stripMiniMessageColors(String text) {
        if (text == null) return "";

        text = text.replaceAll("<[^>]*>", "");

        text = text.replaceAll("§.", "");

        return text;
    }
}