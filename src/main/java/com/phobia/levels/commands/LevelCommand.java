package com.phobia.levels.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.phobia.levels.LevelPlugin;
import com.phobia.levels.data.PlayerData;

public class LevelCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // --- Logic: Reload Command ---
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("levels.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to reload the config.");
                return true;
            }

            LevelPlugin.getInstance().reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Levels configuration reloaded successfully!");
            return true;
        }

        // --- Logic: Admin Reset Command ---
        if (args.length >= 2 && args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("levels.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to reset levels.");
                return true;
            }

            String targetName = args[1];
            Player target = Bukkit.getPlayer(targetName);

            if (target != null) {
                // Online Reset
                PlayerData targetData = LevelPlugin.getInstance().getPlayerDataManager().getData(target);
                targetData.resetProgress();
                
                LevelPlugin.getInstance().getPlayerDataManager().save(target);
                
                // FIX: Remove hardcoded Tab name. 
                // Setting it to null or the player's name allows Tab/Rank plugins to refresh it.
                target.setPlayerListName(null); 
                
                // Refresh Scoreboard
                var board = LevelPlugin.getInstance().getScoreboardHandler().getBoard(target);
                if (board != null) board.update();

                sender.sendMessage(ChatColor.GREEN + "Successfully reset " + target.getName() + " (Online).");
            } else {
                // Offline Reset
                OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                if (!offlineTarget.hasPlayedBefore()) {
                    sender.sendMessage(ChatColor.RED + "Player has never played before.");
                    return true;
                }

                PlayerData offlineData = LevelPlugin.getInstance().getPlayerDataManager().loadOfflineData(offlineTarget);
                offlineData.setLevel(1);
                offlineData.setXp(0);
                // Ensure consistency with your quadratic formula logic
                offlineData.setRequiredXp(160); 
                
                LevelPlugin.getInstance().getPlayerDataManager().saveOfflineData(offlineTarget, offlineData);
                sender.sendMessage(ChatColor.GREEN + "Successfully reset " + offlineTarget.getName() + " (Offline).");
            }
            return true;
        }

        // --- Logic: Standard Level Check ---
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Usage: /level <reset|reload> [player]");
            return true;
        }

        Player player = (Player) sender;
        PlayerData data = LevelPlugin.getInstance().getPlayerDataManager().getData(player);

        player.sendMessage(ChatColor.WHITE + "=== " + ChatColor.GRAY + "Level Info" + ChatColor.WHITE + " ===");
        player.sendMessage(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + data.getLevel());
        player.sendMessage(ChatColor.GRAY + "XP: " + ChatColor.YELLOW + data.getXp() + " / " + data.getRequiredXp());
        
        double progress = (double) data.getXp() / data.getRequiredXp();
        player.sendMessage(ChatColor.GRAY + "Progress: " + getProgressBar(progress));

        return true;
    }

    private String getProgressBar(double percentage) {
        int bars = 20;
        int completed = (int) (bars * percentage);
        StringBuilder sb = new StringBuilder(ChatColor.GREEN.toString());
        for (int i = 0; i < bars; i++) {
            if (i == completed) sb.append(ChatColor.GRAY);
            sb.append("|");
        }
        return sb.toString();
    }
}