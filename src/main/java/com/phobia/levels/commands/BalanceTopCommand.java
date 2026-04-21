package com.phobia.levels.commands;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import com.phobia.levels.LevelPlugin;
import com.phobia.levels.LevelsAPI;

public class BalanceTopCommand implements CommandExecutor {

    private final int ENTRIES_PER_PAGE = 10;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // label determines if we are doing /baltop or /banktop
        boolean isBank = label.equalsIgnoreCase("banktop");
        String typeName = isBank ? "Bank" : "Pocket";
        String color = isBank ? "§6" : "§e";

        // Handle Pagination
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid page number.");
                return true;
            }
        }

        sender.sendMessage(ChatColor.GRAY + "Loading leaderboard, please wait...");

        // --- UPDATED FOLDER PATH TO MATCH PLAYERDATAMANAGER ---
        File folder = new File(LevelPlugin.getInstance().getDataFolder(), "playerdata");
        
        if (!folder.exists() || folder.listFiles() == null) {
            sender.sendMessage(ChatColor.RED + "No player data found in /playerdata/ folder.");
            return true;
        }

        Map<String, Integer> balances = new HashMap<>();

        for (File file : folder.listFiles()) {
            // Only process .yml files and skip common non-player files
            if (!file.getName().endsWith(".yml") || file.getName().equalsIgnoreCase("config.yml")) continue;

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String uuidStr = file.getName().replace(".yml", "");
            
            // Get the balance based on which command was used
            int balance = isBank ? config.getInt("bankBalance", 0) : config.getInt("tokens", 0);
            
            try {
                // Get name from Bukkit (cached) or fallback to UUID
                UUID uuid = UUID.fromString(uuidStr);
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                String name = (op.getName() != null) ? op.getName() : "Unknown (" + uuidStr.substring(0, 5) + ")";
                
                balances.put(name, balance);
            } catch (IllegalArgumentException e) {
                // This skips files that aren't named as valid UUIDs
                continue;
            }
        }

        // Sort by value descending
        List<Map.Entry<String, Integer>> sortedList = balances.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        if (sortedList.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Leaderboard is currently empty.");
            return true;
        }

        int totalPages = (int) Math.ceil((double) sortedList.size() / ENTRIES_PER_PAGE);
        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        // Header
        sender.sendMessage("");
        sender.sendMessage("§8§m-------§r §6§lTOP " + typeName.toUpperCase() + " BALANCES §7(Page " + page + "/" + totalPages + ") §8§m-------");

        int start = (page - 1) * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, sortedList.size());

        for (int i = start; i < end; i++) {
            Map.Entry<String, Integer> entry = sortedList.get(i);
            sender.sendMessage(" §7" + (i + 1) + ". " + color + entry.getKey() + " §8» " + "§f" + LevelsAPI.format(entry.getValue()) + "⛁");
        }

        sender.sendMessage("§8§m------------------------------------------");

        return true;
    }
}