package me.newtale.lootroll.command;

import me.newtale.lootroll.LootRoll;
import me.newtale.lootroll.manager.ConfigManager;
import me.newtale.lootroll.manager.LootManager;
import me.newtale.lootroll.manager.PartyManager;
import me.newtale.lootroll.util.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LootRollAdminCommand implements CommandExecutor, TabCompleter {

	private final ConfigManager configManager;
	private final LootManager lootManager;
	private final PartyManager partyManager;
	private final LootRoll plugin;

	public LootRollAdminCommand(LootRoll plugin) {
		this.configManager = plugin.getConfigManager();
		this.lootManager = plugin.getLootManager();
		this.partyManager = plugin.getPartyManager();
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!sender.hasPermission("lootroll.admin")) {
			sender.sendMessage(Component.text("You don't have permission to use this command!"));
			return true;
		}

		if (args.length > 0) {
			switch (args[0].toLowerCase()) {
				case "reload": {
					configManager.reloadConfig();
					lootManager.loadLootConfiguration();
					String template = configManager.getMessage("reload-success", "<green>Configuration reloaded successfully!</green>");
					sender.sendMessage(MessageUtils.processMessageWithVariables(template, null, null, null));
					return true;
				}
				case "info": {
					String pluginVersion = plugin.getPluginMeta().getVersion();
					List<Component> lines = Arrays.asList(
							MessageUtils.processMessageWithVariables("<yellow>=== LootRoll ===</yellow>", null, null, null),
							MessageUtils.processMessageWithVariables("<gray>Version</gray> <white><ver></white>", null, null, null,
									Placeholder.unparsed("ver", pluginVersion)),
							MessageUtils.processMessageWithVariables("<gray>Mobs:</gray> <white><num></white>", null, null, null,
									Placeholder.unparsed("num", String.valueOf(lootManager.getTotalMobCount()))),
							MessageUtils.processMessageWithVariables("<gray>Configs:</gray> <white><num></white>", null, null, null,
									Placeholder.unparsed("num", String.valueOf(configManager.getDropConfigs().size()))),
							MessageUtils.processMessageWithVariables("<gray>Roll distance:</gray> <white><num></white>", null, null, null,
									Placeholder.unparsed("num", String.valueOf(configManager.getRollDistance()))),
							MessageUtils.processMessageWithVariables("<gray>Roll time:</gray> <white><num>s</white>", null, null, null,
									Placeholder.unparsed("num", String.valueOf(configManager.getRollTime()))),
							MessageUtils.processMessageWithVariables("<gray>Party system:</gray> <white><name></white>", null, null, null,
									Placeholder.unparsed("name", partyManager.getPartyType().name()))
					);
					lines.forEach(sender::sendMessage);
					return true;
				}
				case "list": {
					sender.sendMessage(MessageUtils.processMessageWithVariables("<yellow>=== Mobs ===</yellow>", null, null, null));
					if (lootManager.getTotalMobCount() == 0) {
						sender.sendMessage(MessageUtils.processMessageWithVariables("<red>No mobs configured!</red>", null, null, null));
					} else {
						for (String mobId : lootManager.getAllMobIds()) {
							int lootCount = lootManager.getMobLoot(mobId).size();
							sender.sendMessage(MessageUtils.processMessageWithVariables("<gray>-</gray> <white><mob></white> <gray>(<num> loot items)</gray>", null, null, null,
									Placeholder.unparsed("mob", mobId),
									Placeholder.unparsed("num", String.valueOf(lootCount))));
						}
					}
					return true;
				}
				case "drops": {
					sender.sendMessage(MessageUtils.processMessageWithVariables("<yellow>=== Drops ===</yellow>", null, null, null));
					if (configManager.getDropConfigs().isEmpty()) {
						sender.sendMessage(MessageUtils.processMessageWithVariables("<red>No drop configuration files found!</red>", null, null, null));
					} else {
						for (String fileName : configManager.getDropConfigs().keySet()) {
							sender.sendMessage(MessageUtils.processMessageWithVariables("<gray>-</gray> <white><file>.yml</white>", null, null, null,
									Placeholder.unparsed("file", fileName)));
						}
					}
					return true;
				}
			}
		}

		String pluginVersion = plugin.getPluginMeta().getVersion();
		List<Component> help = Arrays.asList(
				MessageUtils.processMessageWithVariables("<yellow>LootRoll v<ver></yellow>", null, null, null,
						Placeholder.unparsed("ver", pluginVersion)),
				MessageUtils.processMessageWithVariables("<yellow>Usage:</yellow>", null, null, null),
				MessageUtils.processMessageWithVariables("<white>/lootroll reload</white> <gray>- Reload config</gray>", null, null, null),
				MessageUtils.processMessageWithVariables("<white>/lootroll info</white> <gray>- Plugin info</gray>", null, null, null),
				MessageUtils.processMessageWithVariables("<white>/lootroll list</white> <gray>- Mobs config list</gray>", null, null, null),
				MessageUtils.processMessageWithVariables("<white>/lootroll drops</white> <gray>- Drops config list</gray>", null, null, null)
		);
		help.forEach(sender::sendMessage);
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		if (!sender.hasPermission("lootroll.admin")) return List.of();
		List<String> base = List.of("reload", "info", "list", "drops");
		if (args.length == 1) {
			return base.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
		}
		return List.of();
	}
}

