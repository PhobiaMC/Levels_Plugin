package com.phobia.levels.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.phobia.levels.LevelPlugin;
import com.phobia.levels.LevelsAPI;
import com.phobia.levels.data.PlayerData;

public class TokenAdminCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Updated Usage: /tokenadmin <setpocket/setbank/reset> <player> [amount]
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /tokenadmin <setpocket/setbank/reset> <player> [amount]");
            return true;
        }

        // Permission check
        if (!sender.hasPermission("levels.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];
        
        // --- Offline Player Support ---
        // First check if player is online, otherwise look up offline data
        Player onlineTarget = Bukkit.getPlayer(targetName);
        OfflinePlayer offlineTarget = (onlineTarget != null) ? onlineTarget : Bukkit.getOfflinePlayer(targetName);

        // Check if the player has ever played before (to avoid creating junk files)
        if (!offlineTarget.hasPlayedBefore() && onlineTarget == null) {
            sender.sendMessage(ChatColor.RED + "Player has never joined the server.");
            return true;
        }

        // Load data using the appropriate method
        PlayerData data;
        if (onlineTarget != null) {
            data = LevelPlugin.getInstance().getPlayerDataManager().getData(onlineTarget);
        } else {
            data = LevelPlugin.getInstance().getPlayerDataManager().loadOfflineData(offlineTarget);
        }

        if (data == null) {
            sender.sendMessage(ChatColor.RED + "Data not found for this player.");
            return true;
        }

        // Handle Reset Action
        if (action.equals("reset")) {
            data.setTokens(0);
            setBankInternal(data, 0);
            sender.sendMessage(ChatColor.GREEN + "Reset all token balances for " + offlineTarget.getName() + ".");
            if (onlineTarget != null) {
                onlineTarget.sendMessage(ChatColor.RED + "Your token and bank balances have been reset by an administrator.");
            }
            saveData(offlineTarget, data);
            return true;
        }

        // Amount check for setpocket and setbank
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /tokenadmin " + action + " <player> <amount>");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Amount must be a number.");
            return true;
        }

        if (amount < 0) {
            sender.sendMessage(ChatColor.RED + "Amount cannot be negative.");
            return true;
        }

        String formattedAmount = LevelsAPI.format(amount);

        if (action.equals("setpocket")) {
            data.setTokens(amount);
            sender.sendMessage(ChatColor.GREEN + "Set pocket tokens for " + offlineTarget.getName() + " to " + ChatColor.YELLOW + formattedAmount + "⛁");
            if (onlineTarget != null) {
                onlineTarget.sendMessage(ChatColor.GRAY + "An administrator set your pocket tokens to " + ChatColor.YELLOW + formattedAmount + "⛁");
            }
        } 
        else if (action.equals("setbank")) {
            setBankInternal(data, amount);
            sender.sendMessage(ChatColor.GREEN + "Set bank balance for " + offlineTarget.getName() + " to " + ChatColor.GOLD + formattedAmount + "⛁");
            if (onlineTarget != null) {
                onlineTarget.sendMessage(ChatColor.GRAY + "An administrator set your bank balance to " + ChatColor.GOLD + formattedAmount + "⛁");
            }
        } 
        else {
            sender.sendMessage(ChatColor.RED + "Invalid action. Use 'setpocket', 'setbank', or 'reset'.");
            return true;
        }

        // Final Save
        saveData(offlineTarget, data);
        return true;
    }

    // Helper to handle saving based on online status
    private void saveData(OfflinePlayer target, PlayerData data) {
        if (target.isOnline() && target.getPlayer() != null) {
            LevelPlugin.getInstance().getPlayerDataManager().save(target.getPlayer());
        } else {
            LevelPlugin.getInstance().getPlayerDataManager().saveOfflineData(target, data);
        }
    }

    // Helper to ensure bank is set correctly if PlayerData setter is missing
    private void setBankInternal(PlayerData data, int amount) {
        int currentBank = data.getBankBalance();
        int difference = amount - currentBank;
        
        if (difference > 0) {
            int oldPocket = data.getTokens();
            data.setTokens(difference);
            data.deposit(difference);
            data.setTokens(oldPocket);
        } else if (difference < 0) {
            data.withdraw(Math.abs(difference));
            data.setTokens(data.getTokens() - Math.abs(difference));
        }
    }
}