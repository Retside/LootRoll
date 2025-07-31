package me.newtale.lootRoll.commands;

import me.newtale.lootRoll.managers.RollManager;
import me.newtale.lootRoll.models.RollSession;
import me.newtale.lootRoll.utils.SessionHelper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RollCommand implements CommandExecutor, TabCompleter {
    private final RollManager rollManager;

    public RollCommand(RollManager rollManager) {
        this.rollManager = rollManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        if (args.length == 0) {
            rollManager.handleRollCommand(player);
        } else {
            rollManager.handleRollCommand(player, String.join(" ", args));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        List<RollSession> sessions = rollManager.getAvailableSessionsForPlayer(player);
        List<String> availableItems = SessionHelper.getAvailableItemNamesForTabCompletion(sessions);

        if (args.length <= 1) {
            String input = args.length == 0 ? "" : args[0].toLowerCase();
            return availableItems.stream()
                    .filter(item -> item.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        String input = String.join(" ", args).toLowerCase();
        return availableItems.stream()
                .filter(item -> item.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}