package com.phobia.levels.commands;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.phobia.levels.LevelPlugin;
import com.phobia.levels.data.PlayerData;
import com.phobia.levels.managers.PlayerDataManager;

public class ProfileCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        PlayerDataManager manager = LevelPlugin.getInstance().getPlayerDataManager();

        // ---- /profile ----
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must use /profile <player>");
                return true;
            }
            Player player = (Player) sender;
            PlayerData data = manager.getData(player);
            sendProfile(player, player.getName(), data);
            return true;
        }

        // ---- /profile <player> ----
        String targetName = args[0];
        Player online = Bukkit.getPlayerExact(targetName);
        
        if (online != null) {
            PlayerData data = manager.getData(online);
            sendProfile(sender, online.getName(), data);
            return true;
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
        if (!offline.hasPlayedBefore() && !offline.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        File playerFile = manager.getOfflinePlayerFile(offline);
        if (!playerFile.exists()) {
            sender.sendMessage(ChatColor.RED + "No saved data for this player.");
            return true;
        }

        PlayerData offlineData = manager.loadOfflineData(offline);
        sendProfile(sender, offline.getName(), offlineData);
        return true;
    }

    private void sendProfile(CommandSender viewer, String name, PlayerData data) {
        // Formatted KDRs
        String pKDR = String.format("%.2f", data.getKdr());
        String tKDR = String.format("%.2f", data.getTkdr());

        viewer.sendMessage("");
        viewer.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + " PROFILE " + ChatColor.YELLOW + name);
        
        // Progress Section
        viewer.sendMessage(ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + "Level: " + ChatColor.GREEN + data.getLevel() 
            + ChatColor.DARK_GRAY + " (" + ChatColor.AQUA + data.getXp() + ChatColor.GRAY + "/" + ChatColor.AQUA + data.getRequiredXp() + " XP" + ChatColor.DARK_GRAY + ")");
        
        // Combat Section
        viewer.sendMessage(ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + "Combat: " 
            + ChatColor.RED + data.getKills() + "⚔ " + ChatColor.DARK_RED + data.getDeaths() + "☠");
        
        viewer.sendMessage(ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + "Mob Kills: " + ChatColor.LIGHT_PURPLE + data.getMobKills());

        viewer.sendMessage(ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + "KDR: " + ChatColor.GOLD + pKDR 
            + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "TKDR: " + ChatColor.GOLD + tKDR);
        
        // Economy Section (Updated for Banking)
        viewer.sendMessage(ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + "Pocket: " + ChatColor.YELLOW + data.getTokens() + "⛁"
            + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "Bank: " + ChatColor.GOLD + data.getBankBalance() + "⛁");
        
        viewer.sendMessage("");
    }
}