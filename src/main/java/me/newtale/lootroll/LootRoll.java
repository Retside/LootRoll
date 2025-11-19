package me.newtale.lootroll;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import me.newtale.lootroll.command.RollCancelCommand;
import me.newtale.lootroll.command.RollCommand;
import me.newtale.lootroll.command.GreedCommand;
import me.newtale.lootroll.listener.ItemPickupListener;
import me.newtale.lootroll.listener.MobDeathListener;
import me.newtale.lootroll.manager.*;
import me.newtale.lootroll.command.LootRollAdminCommand;
import me.newtale.lootroll.util.ItemUtils;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;

import java.util.Objects;

public final class LootRoll extends JavaPlugin {

    private ConfigManager configManager;
    private LootManager lootManager;
    private PartyManager partyManager;
    private ParticleEffectManager particleEffectManager;
    private RollManager rollManager;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().init();
        
        ItemUtils.initialize(this);
        configManager = new ConfigManager(this);

        if (!checkDependencies()) {
            return;
        }

        partyManager = new PartyManager(configManager);
        lootManager = new LootManager(this, configManager);
        particleEffectManager = new ParticleEffectManager(this);
        rollManager = new RollManager(this, configManager, partyManager, particleEffectManager);

        lootManager.loadLootConfiguration();
        registerCommands();
        registerListeners();

        getLogger().info("Loaded " + lootManager.getTotalMobCount() + " mobs.");
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
        if (rollManager != null) {
            rollManager.cleanup();
        }
    }

    private boolean checkDependencies() {
        if (Bukkit.getPluginManager().getPlugin("PacketEvents") == null) {
            getLogger().severe("PacketEvents not found! Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }

        String partySystemType = configManager.getPartySystem().toLowerCase();

        switch (partySystemType) {
            case "mmocore":
                if (Bukkit.getPluginManager().getPlugin("MMOCore") == null) {
                    getLogger().severe("MMOCore plugin not found but specified in config! Disabling...");
                    getServer().getPluginManager().disablePlugin(this);
                    return false;
                }
                break;
            case "parties":
            default:
                if (Bukkit.getPluginManager().getPlugin("Parties") == null) {
                    getLogger().severe("Parties plugin not found but specified in config! Disabling...");
                    getServer().getPluginManager().disablePlugin(this);
                    return false;
                }
                break;
        }

        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            getLogger().info("MythicMobs plugin not found! Some features may not work.");
        }

        if (Bukkit.getPluginManager().getPlugin("MMOItems") == null) {
            getLogger().warning("MMOItems plugin not found! Some features may not work.");
        }

        if (Bukkit.getPluginManager().getPlugin("Nexo") == null) {
            getLogger().warning("Nexo plugin not found! Some features may not work.");
        }

        if (Bukkit.getPluginManager().getPlugin("CraftEngine") == null) {
            getLogger().warning("CraftEngine plugin not found! Some features may not work.");
        }

        return true;
    }

    private void registerCommands() {
        RollCommand rollCommand = new RollCommand(rollManager);
        GreedCommand greedCommand = new GreedCommand(rollManager);
        RollCancelCommand rollCancelCommand = new RollCancelCommand(rollManager);
        LootRollAdminCommand adminCommand = new LootRollAdminCommand(this);

        Objects.requireNonNull(getCommand("roll")).setExecutor(rollCommand);
        Objects.requireNonNull(getCommand("roll")).setTabCompleter(rollCommand);

        Objects.requireNonNull(getCommand("greed")).setExecutor(greedCommand);
        Objects.requireNonNull(getCommand("greed")).setTabCompleter(greedCommand);

        Objects.requireNonNull(getCommand("pass")).setExecutor(rollCancelCommand);
        Objects.requireNonNull(getCommand("pass")).setTabCompleter(rollCancelCommand);

        Objects.requireNonNull(getCommand("lootroll")).setExecutor(adminCommand);
        Objects.requireNonNull(getCommand("lootroll")).setTabCompleter(adminCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new MobDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemPickupListener(rollManager), this);
    }

    public ConfigManager getConfigManager() { return configManager; }
    public LootManager getLootManager() { return lootManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public RollManager getRollManager() { return rollManager; }
}