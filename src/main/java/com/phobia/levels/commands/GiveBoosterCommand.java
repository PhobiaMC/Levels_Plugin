package com.phobia.levels.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.phobia.levels.LevelPlugin;
import com.phobia.levels.boosters.BoosterScope;
import com.phobia.levels.boosters.BoosterType;

public class GiveBoosterCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("levels.givebooster")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        if (args.length != 5) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /givebooster <name> <xp|tokens> <personal|global> <multiplier> <minutes>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "That player has never joined this server.");
            return true;
        }

        BoosterType type;
        if (args[1].equalsIgnoreCase("xp")) {
            type = BoosterType.XP;
        } else if (args[1].equalsIgnoreCase("tokens")) {
            type = BoosterType.TOKENS;
        } else {
            sender.sendMessage(ChatColor.RED + "Booster type must be 'xp' or 'tokens'.");
            return true;
        }

        BoosterScope scope;
        if (args[2].equalsIgnoreCase("personal")) {
            scope = BoosterScope.PERSONAL;
        } else if (args[2].equalsIgnoreCase("global")) {
            scope = BoosterScope.GLOBAL;
        } else {
            sender.sendMessage(ChatColor.RED + "Scope must be 'personal' or 'global'.");
            return true;
        }

        double multiplier;
        int minutes;
        try {
            multiplier = Double.parseDouble(args[3]);
            minutes = Integer.parseInt(args[4]);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Invalid number.");
            return true;
        }

        if (multiplier < 1.0) {
            sender.sendMessage(ChatColor.RED + "Multiplier must be at least 1.0.");
            return true;
        }
        if (minutes <= 0) {
            sender.sendMessage(ChatColor.RED + "Minutes must be > 0.");
            return true;
        }

        LevelPlugin.getInstance().getBoosterManager().givePendingBooster(target, type, scope, multiplier, minutes);

        sender.sendMessage(ChatColor.GREEN + "Gave " + target.getName() + " a " + scope.name().toLowerCase()
                + " " + type.name().toLowerCase() + " booster (x" + multiplier + ", " + minutes + "m). It will appear in their booster menu.");

        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(ChatColor.GREEN + "You received a new booster! Check your booster menu to activate it.");
        }

        return true;
    }
}