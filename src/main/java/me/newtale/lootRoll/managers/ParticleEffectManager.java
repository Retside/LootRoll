package me.newtale.lootRoll.managers;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParticleEffectManager {

    private final JavaPlugin plugin;
    private final Map<Integer, BukkitTask> activeAnimations; // entityId -> animation task

    public ParticleEffectManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.activeAnimations = new HashMap<>();
    }

    public void startItemAnimation(Item item, ItemStack itemStack, List<Player> viewers) {
        stopItemAnimation(item.getEntityId());

        Color particleColor = getItemColor(itemStack);

        BukkitTask animationTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!item.isValid() || ticks > 60) { // 3 секунди анімації
                    stopItemAnimation(item.getEntityId());
                    cancel();
                    return;
                }

                Location itemLocation = item.getLocation().add(0, 0.5, 0);

                if (ticks % 2 == 0) {
                    spawnItemParticles(itemLocation, particleColor, viewers);
                }

                if (ticks < 20) {
                    spawnBurstParticles(itemLocation, particleColor, viewers);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);

        activeAnimations.put(item.getEntityId(), animationTask);
    }

    public void stopItemAnimation(int entityId) {
        BukkitTask task = activeAnimations.remove(entityId);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    private void spawnItemParticles(Location center, Color color, List<Player> viewers) {
        for (Player viewer : viewers) {
            if (!viewer.isOnline()) continue;

            double x = center.getX();
            double z = center.getZ();
            double y = center.getY();

            Location particleLocation = new Location(center.getWorld(), x, y, z);

            sendParticleToPlayer(viewer, particleLocation, color);
        }
    }

    private void spawnBurstParticles(Location center, Color color, List<Player> viewers) {
        for (Player viewer : viewers) {
            if (!viewer.isOnline()) continue;

            for (int i = 0; i < 5; i++) {
                double offsetX = (ThreadLocalRandom.current().nextDouble() - 0.5);
                double offsetY = ThreadLocalRandom.current().nextDouble() * 0.5;
                double offsetZ = (ThreadLocalRandom.current().nextDouble() - 0.5);

                Location particleLocation = center.clone().add(offsetX, offsetY, offsetZ);
                sendParticleToPlayer(viewer, particleLocation, color);
            }
        }
    }

    private void sendParticleToPlayer(Player player, Location location, Color color) {
        try {
            Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.0f);
            player.spawnParticle(Particle.DUST, location, 1, 0, 0, 0, 0, dustOptions);
        } catch (Exception e) {
            try {
                player.spawnParticle(Particle.ENCHANT, location, 1, 0, 0, 0, 0.1);
            } catch (Exception ignored) {

            }
        }
    }

    public Color getItemColor(ItemStack item) {
        String displayName = getItemDisplayName(item);

        Color miniMessageColor = extractMiniMessageColor(displayName);
        if (miniMessageColor != null) {
            return miniMessageColor;
        }

        Color legacyColor = extractLegacyColor(displayName);
        if (legacyColor != null) {
            return legacyColor;
        }

        return Color.WHITE;
    }

    private Color extractMiniMessageColor(String text) {
        Pattern colorPattern = Pattern.compile("<(#[0-9a-fA-F]{6}|[a-zA-Z_]+|color:[^>]+)>");
        Matcher matcher = colorPattern.matcher(text);

        if (matcher.find()) {
            String colorStr = matcher.group(1);
            return parseMiniMessageColor(colorStr);
        }

        return null;
    }

    private Color parseMiniMessageColor(String colorStr) {
        try {
            if (colorStr.startsWith("#")) {
                int rgb = Integer.parseInt(colorStr.substring(1), 16);
                return Color.fromRGB(rgb);
            } else if (colorStr.startsWith("color:")) {
                String actualColor = colorStr.substring(6);
                return parseNamedColor(actualColor);
            } else {
                return parseNamedColor(colorStr);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private Color extractLegacyColor(String text) {
        Pattern legacyPattern = Pattern.compile("§([0-9a-fA-F])");
        Matcher matcher = legacyPattern.matcher(text);

        if (matcher.find()) {
            char colorCode = matcher.group(1).charAt(0);
            return getLegacyColor(colorCode);
        }

        return null;
    }

    private Color parseNamedColor(String colorName) {
        return switch (colorName.toLowerCase()) {
            case "black" -> Color.fromRGB(0x000000);
            case "dark_blue" -> Color.fromRGB(0x0000AA);
            case "dark_green" -> Color.fromRGB(0x00AA00);
            case "dark_aqua" -> Color.fromRGB(0x00AAAA);
            case "dark_red" -> Color.fromRGB(0xAA0000);
            case "dark_purple" -> Color.fromRGB(0xAA00AA);
            case "gold" -> Color.fromRGB(0xFFAA00);
            case "gray" -> Color.fromRGB(0xAAAAAA);
            case "dark_gray" -> Color.fromRGB(0x555555);
            case "blue" -> Color.fromRGB(0x5555FF);
            case "green" -> Color.fromRGB(0x55FF55);
            case "aqua" -> Color.fromRGB(0x55FFFF);
            case "red" -> Color.fromRGB(0xFF5555);
            case "light_purple" -> Color.fromRGB(0xFF55FF);
            case "yellow" -> Color.fromRGB(0xFFFF55);
            case "white" -> Color.fromRGB(0xFFFFFF);
            default -> null;
        };
    }

    private Color getLegacyColor(char colorCode) {
        return switch (colorCode) {
            case '0' -> Color.fromRGB(0x000000); // BLACK
            case '1' -> Color.fromRGB(0x0000AA); // DARK_BLUE
            case '2' -> Color.fromRGB(0x00AA00); // DARK_GREEN
            case '3' -> Color.fromRGB(0x00AAAA); // DARK_AQUA
            case '4' -> Color.fromRGB(0xAA0000); // DARK_RED
            case '5' -> Color.fromRGB(0xAA00AA); // DARK_PURPLE
            case '6' -> Color.fromRGB(0xFFAA00); // GOLD
            case '7' -> Color.fromRGB(0xAAAAAA); // GRAY
            case '8' -> Color.fromRGB(0x555555); // DARK_GRAY
            case '9' -> Color.fromRGB(0x5555FF); // BLUE
            case 'a' -> Color.fromRGB(0x55FF55); // GREEN
            case 'b' -> Color.fromRGB(0x55FFFF); // AQUA
            case 'c' -> Color.fromRGB(0xFF5555); // RED
            case 'd' -> Color.fromRGB(0xFF55FF); // LIGHT_PURPLE
            case 'e' -> Color.fromRGB(0xFFFF55); // YELLOW
            case 'f' -> Color.fromRGB(0xFFFFFF); // WHITE
            default -> null;
        };
    }

    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name().toLowerCase().replace("_", " ");
    }

    public void cleanup() {
        for (BukkitTask task : activeAnimations.values()) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
        activeAnimations.clear();
    }

    public boolean hasActiveAnimation(int entityId) {
        return activeAnimations.containsKey(entityId);
    }

    public int getActiveAnimationsCount() {
        return activeAnimations.size();
    }
}