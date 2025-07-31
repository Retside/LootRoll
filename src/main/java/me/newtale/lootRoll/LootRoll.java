package me.newtale.lootRoll;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import me.newtale.lootRoll.commands.RollCancelCommand;
import me.newtale.lootRoll.commands.RollCommand;
import me.newtale.lootRoll.commands.RollFCommand;
import me.newtale.lootRoll.managers.*;
import me.newtale.lootRoll.listeners.MobDeathListener;
import me.newtale.lootRoll.listeners.ItemPickupListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class LootRoll extends JavaPlugin {

    private ConfigManager configManager;
    private LootManager lootManager;
    private PartyManager partyManager;
    private ParticleEffectManager particleEffectManager;
    private RollManager rollManager;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("lootroll")) {
            if (!sender.hasPermission("lootroll.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            if (args.length > 0) {
                switch (args[0].toLowerCase()) {
                    case "reload":
                        configManager.reloadConfig();
                        lootManager.loadLootConfiguration();

                        String message = configManager.getMessage("reload-success", "&aConfiguration reloaded successfully!");
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                        return true;

                    case "info":
                        sender.sendMessage(ChatColor.YELLOW + " === LootRoll ===");
                        sender.sendMessage(ChatColor.GRAY + " Версія: " + ChatColor.WHITE + "1.0");
                        sender.sendMessage(ChatColor.GRAY + " Налаштовані моби: " + ChatColor.WHITE + lootManager.getTotalMobCount());
                        sender.sendMessage(ChatColor.GRAY + " Файли конфігурацій: " + ChatColor.WHITE + configManager.getDropConfigs().size());
                        sender.sendMessage(ChatColor.GRAY + " Дистанція ролу: " + ChatColor.WHITE + configManager.getRollDistance());
                        sender.sendMessage(ChatColor.GRAY + " Час ролу: " + ChatColor.WHITE + configManager.getRollTime() + "s");
                        return true;

                    case "list":
                        sender.sendMessage(ChatColor.YELLOW + " === Налаштовані моби ===");
                        if (lootManager.getTotalMobCount() == 0) {
                            sender.sendMessage(ChatColor.RED + "No mobs configured!");
                        } else {
                            for (String mobId : lootManager.getAllMobIds()) {
                                int lootCount = lootManager.getMobLoot(mobId).size();
                                sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + mobId +
                                        ChatColor.GRAY + " (" + lootCount + " loot items)");
                            }
                        }
                        return true;

                    case "drops":
                        sender.sendMessage(ChatColor.YELLOW + " === Налаштовані дропи ===");
                        if (configManager.getDropConfigs().isEmpty()) {
                            sender.sendMessage(ChatColor.RED + "No drop configuration files found!");
                        } else {
                            for (String fileName : configManager.getDropConfigs().keySet()) {
                                sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + fileName + ".yml");
                            }
                        }
                        return true;
                }
            }

            sender.sendMessage(ChatColor.YELLOW + " NewRPG LootRoll v1.0");
            sender.sendMessage(ChatColor.YELLOW + " Використання:");
            sender.sendMessage(ChatColor.GRAY + " ");
            sender.sendMessage(ChatColor.WHITE + "  /lootroll reload - Перезавантажити конфігурацію");
            sender.sendMessage(ChatColor.WHITE + "  /lootroll info - Інформація про плагін");
            sender.sendMessage(ChatColor.WHITE + "  /lootroll list - Список конфігурацій мобів");
            sender.sendMessage(ChatColor.WHITE + "  /lootroll drops - Список конфігурацій дропів");
            sender.sendMessage(ChatColor.WHITE + " ");
            return true;
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("lootroll")) {
            return List.of("reload", "info", "list", "drops");
        }
        return null;
    }

    @Override
    public void onEnable() {
        // Initialize configuration
        configManager = new ConfigManager(this);

        if (!checkDependencies()) {
            return;
        }
        PartiesAPI partiesAPI = Parties.getApi();

        // Initialize managers
        partyManager = new PartyManager(partiesAPI, configManager);
        lootManager = new LootManager(this, configManager);
        particleEffectManager = new ParticleEffectManager(this);
        rollManager = new RollManager(this, configManager, partyManager, particleEffectManager);

        lootManager.loadLootConfiguration();
        registerCommands();
        registerListeners();

        getLogger().info("NewRPG Loot System enabled! Developed by Ney #ney___");
        getLogger().info("Loaded " + lootManager.getTotalMobCount() + " mobs from " +
                configManager.getDropConfigs().size() + " drop configuration files");
    }

    @Override
    public void onDisable() {
        if (rollManager != null) {
            rollManager.cleanup();
        }
        getLogger().info("NewRPG Loot System disabled!");
    }

    private boolean checkDependencies() {
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().severe("ProtocolLib not found! Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }

        if (Bukkit.getPluginManager().getPlugin("Parties") == null) {
            getLogger().severe("Parties plugin not found! Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }

        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            getLogger().severe("MythicMobs plugin not found! Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }

        if (Bukkit.getPluginManager().getPlugin("MMOItems") == null) {
            getLogger().warning("MMOItems plugin not found! Some features may not work.");
        }

        getLogger().info("All dependencies found!");
        return true;
    }

    private void registerCommands() {
        // Register roll command
        RollCommand rollCommand = new RollCommand(rollManager);
        RollFCommand rollFCommand = new RollFCommand(rollManager);
        RollCancelCommand rollCancelCommand = new RollCancelCommand(rollManager);

        getCommand("roll").setExecutor(rollCommand);
        getCommand("roll").setTabCompleter(rollCommand);

        getCommand("rollf").setExecutor(rollFCommand);
        getCommand("rollf").setTabCompleter(rollFCommand);

        getCommand("rollcancel").setExecutor(rollCancelCommand);
        getCommand("rollcancel").setTabCompleter(rollCancelCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new MobDeathListener(lootManager, partyManager, rollManager), this);
        getServer().getPluginManager().registerEvents(new ItemPickupListener(rollManager), this);
    }

    // Getters for managers
    public ConfigManager getConfigManager() { return configManager; }
    public LootManager getLootManager() { return lootManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public RollManager getRollManager() { return rollManager; }
}