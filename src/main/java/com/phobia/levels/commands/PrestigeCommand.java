package com.phobia.levels.commands;

import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.phobia.levels.LevelPlugin;
import com.phobia.levels.data.PlayerData;

public class PrestigeCommand implements CommandExecutor {

    private final HashSet<UUID> confirmCache = new HashSet<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can change prestige tiers.");
            return true;
        }

        Player player = (Player) sender;
        LevelPlugin plugin = LevelPlugin.getInstance();
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        FileConfiguration config = plugin.getConfig();

        if (data == null) {
            player.sendMessage("§cYour player data profile hasn't loaded yet.");
            return true;
        }

        int currentPrestige = data.getPrestige();
        int maxPrestige = config.getInt("prestige-system.max-prestige", 3);

        if (currentPrestige >= maxPrestige) {
            player.sendMessage("§cYou are already at the maximum prestige tier (" + maxPrestige + ")!");
            return true;
        }

        if (data.getLevel() < 120) {
            player.sendMessage("§cYou must reach level 120 before you can prestige!");
            return true;
        }

        int targetPrestige = currentPrestige + 1;
        int cost = config.getInt("prestige-system.costs." + targetPrestige, 5000);

        if (data.getTokens() < cost) {
            player.sendMessage("§cYou need §e" + cost + " tokens §cto unlock Prestige " + targetPrestige + ". You have §e" + data.getTokens() + "§c.");
            return true;
        }

        // --- Confirmation handling engine ---
        if (!confirmCache.contains(player.getUniqueId())) {
            confirmCache.add(player.getUniqueId());
            player.sendMessage(" ");
            player.sendMessage("§6§l⚠️ PRESTIGE CONFIRMATION ⚠️");
            player.sendMessage("§7This will reset your level to §bLevel 1 §7and cost §e" + cost + " tokens§7.");
            player.sendMessage("§7You will advance to §d§lPrestige Tier " + targetPrestige + "§7.");
            player.sendMessage("§7Type §a/prestige §7again within 15 seconds to confirm!");
            player.sendMessage(" ");

            // Evict from confirmation buffer cache after 15 seconds
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (confirmCache.remove(player.getUniqueId())) {
                    player.sendMessage("§cPrestige request timed out.");
                }
            }, 20L * 15);
            return true;
        }

        // Execute prestige action
        confirmCache.remove(player.getUniqueId());

        if (data.removeTokens(cost)) {
            data.setPrestige(targetPrestige);
            data.setLevel(1);
            data.setXp(0);
            plugin.getPlayerDataManager().save(player);

            String bcMsg = config.getString("prestige-system.messages.broadcast", "§6§lPRESTIGE! §b%player% §7has advanced to §d§lPrestige %prestige%§7!");
            String successMsg = config.getString("prestige-system.messages.success", "§a§lSUCCESS! §7You reset to level 1 and unlocked §d§lPrestige %prestige%§7!");

            bcMsg = bcMsg.replace("%player%", player.getName()).replace("%prestige%", String.valueOf(targetPrestige));
            successMsg = successMsg.replace("%prestige%", String.valueOf(targetPrestige));

            Bukkit.broadcastMessage(bcMsg);
            player.sendMessage(successMsg);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        } else {
            player.sendMessage("§cAn error occurred processing your token transaction. Action aborted.");
        }

        return true;
    }
}