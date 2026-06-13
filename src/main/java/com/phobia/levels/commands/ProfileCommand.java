package com.phobia.levels.commands;

import java.io.File;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

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
            sendProfile(player, player.getName(), player, data);
            return true;
        }

        // ---- /profile <player> ----
        String targetName = args[0];
        Player online = Bukkit.getPlayerExact(targetName);
        
        if (online != null) {
            PlayerData data = manager.getData(online);
            sendProfile(sender, online.getName(), online, data);
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
        sendProfile(sender, offline.getName(), offline, offlineData);
        return true;
    }

    private void sendProfile(CommandSender viewer, String name, OfflinePlayer targetPlayer, PlayerData data) {
        // Formatted KDRs
        String pKDR = String.format("%.2f", data.getKdr());
        String tKDR = String.format("%.2f", data.getTkdr());

        // --- Dynamic Prestige Icon Pulling ---
        String prestigeIcon = "";
        int currentPrestige = data.getPrestige();
        if (currentPrestige > 0) {
            String rawIcon = LevelPlugin.getInstance().getConfig().getString("prestige-system.icons." + currentPrestige, "");
            prestigeIcon = ChatColor.translateAlternateColorCodes('&', rawIcon);
        }

        // --- SAFE REFLECTION: Cross-Plugin JSON Fetching (Supports Offline Targets) ---
        String badgesJson = "[]";
        
        if (Bukkit.getPluginManager().isPluginEnabled("mcgunsbase")) {
            try {
                Class<?> apiClass = Class.forName("com.phobia.mcgunsbase.api.LevelsAPI");
                // Updated method parameters to fetch using OfflinePlayer.class
                Method getBadgesMethod = apiClass.getMethod("getPlayerBadgesJson", OfflinePlayer.class);
                badgesJson = (String) getBadgesMethod.invoke(null, targetPlayer);
            } catch (Exception e) {
                badgesJson = "[]";
            }
        }

        viewer.sendMessage("");
        viewer.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + " PROFILE " + ChatColor.YELLOW + name);
        
        // Progress Section
        viewer.sendMessage(ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + "Level: " + prestigeIcon + ChatColor.GREEN + data.getLevel() 
            + ChatColor.DARK_GRAY + " (" + ChatColor.AQUA + data.getXp() + ChatColor.GRAY + "/" + ChatColor.AQUA + data.getRequiredXp() + " XP" + ChatColor.DARK_GRAY + ")");
        
        // Combat Section
        viewer.sendMessage(ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + "Combat: " 
            + ChatColor.RED + data.getKills() + "⚔ " + ChatColor.DARK_RED + data.getDeaths() + "☠");
        
        viewer.sendMessage(ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + "Mob Kills: " + ChatColor.LIGHT_PURPLE + data.getMobKills());

        viewer.sendMessage(ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + "KDR: " + ChatColor.GOLD + pKDR 
            + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "TKDR: " + ChatColor.GOLD + tKDR);
        
        // Economy Section
        viewer.sendMessage(ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + "Pocket: " + ChatColor.YELLOW + data.getTokens() + "⛁"
            + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "Bank: " + ChatColor.GOLD + data.getBankBalance() + "⛁");
        
        // --- Interactive Badges Line Building ---
        TextComponent badgesRow = new TextComponent(ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + "Badges: ");

        if (badgesJson.equals("[]")) {
            badgesRow.addExtra(new TextComponent(ChatColor.GRAY + "None"));
        } else {
            // Regex engine to extract fields from JSON string entries safely without heavy dependencies
            Pattern pattern = Pattern.compile("\\{\"icon\":\"(.*?)\",\"name\":\"(.*?)\",\"desc\":\"(.*?)\"\\}");
            Matcher matcher = pattern.matcher(badgesJson);
            boolean first = true;

            while (matcher.find()) {
                String icon = matcher.group(1);
                String displayName = ChatColor.translateAlternateColorCodes('&', matcher.group(2));
                String description = matcher.group(3);

                if (!first) {
                    badgesRow.addExtra(new TextComponent(" "));
                }
                first = false;

                // Create individual hover component for this specific badge
                TextComponent badgeIconComponent = new TextComponent(ChatColor.RESET + icon);
                
                String hoverText = displayName + "\n" + ChatColor.GRAY + description;
                badgeIconComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    new ComponentBuilder(hoverText).create()));

                badgesRow.addExtra(badgeIconComponent);
            }
        }

        // Send chat components to players natively, fallback to normal messaging for console log visibility
        if (viewer instanceof Player) {
            ((Player) viewer).spigot().sendMessage(badgesRow);
        } else {
            viewer.sendMessage(badgesRow.toLegacyText());
        }

        viewer.sendMessage("");
    }
}