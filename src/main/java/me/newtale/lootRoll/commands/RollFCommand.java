package me.newtale.lootRoll.commands;

import me.newtale.lootRoll.managers.RollManager;
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

public class RollFCommand implements CommandExecutor, TabCompleter {
    private final RollManager rollManager;

    public RollFCommand(RollManager rollManager) {
        this.rollManager = rollManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        if (args.length == 0) {
            rollManager.handleFallbackRollCommand(player);
        } else {
            rollManager.handleFallbackRollCommand(player, String.join(" ", args));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        List<String> availableItems = rollManager.getAvailableFallbackItemNames(player);

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