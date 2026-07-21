package com.phobia.levels.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.phobia.levels.LevelPlugin;

public class TokenBoostCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // -----------------------------------------
        //  /tokenboost → anyone with view permission
        // -----------------------------------------
        if (args.length == 0) {

            if (!sender.hasPermission("levels.tokenboost.view") &&
                !sender.hasPermission("levels.tokenboost.manage")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission.");
                return true;
            }

            double global = LevelPlugin.getInstance().getTokenBooster();
            long remaining = LevelPlugin.getInstance().getTokenBoosterTimeRemaining();

            if (global <= 1.0) {
                sender.sendMessage(ChatColor.YELLOW + "There is no active Token boost.");
                return true;
            }

            long mins = remaining / 60;
            long secs = remaining % 60;

            sender.sendMessage(ChatColor.GOLD + "Global Token Boost Active:");
            sender.sendMessage(ChatColor.GRAY + "  Multiplier: " + ChatColor.YELLOW + "x" + global);
            sender.sendMessage(ChatColor.GRAY + "  Time Left: " + ChatColor.YELLOW + mins + "m " + secs + "s");
            return true;
        }

        // -----------------------------------------
        //  The remaining commands require manage
        // -----------------------------------------
        if (!sender.hasPermission("levels.tokenboost.manage")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        // /tokenboost stop ----------------------------
        if (args.length == 1 && args[0].equalsIgnoreCase("stop")) {

            if (LevelPlugin.getInstance().getTokenBooster() <= 1.0) {
                sender.sendMessage(ChatColor.YELLOW + "There is no active Token boost to stop.");
                return true;
            }

            LevelPlugin.getInstance().setTokenBooster(1.0, 0);

            Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD +
                    "GLOBAL TOKEN BOOST HAS BEEN STOPPED!");
            return true;
        }

        // /tokenboost <multiplier> <seconds> ----------
        if (args.length != 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /tokenboost <multiplier> <seconds>");
            sender.sendMessage(ChatColor.YELLOW + "       /tokenboost stop");
            return true;
        }

        double multiplier;
        int seconds;

        try {
            multiplier = Double.parseDouble(args[0]);
            seconds = Integer.parseInt(args[1]);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Invalid number.");
            return true;
        }

        if (multiplier < 1.0) {
            sender.sendMessage(ChatColor.RED + "Multiplier must be at least 1.0.");
            return true;
        }
        if (seconds <= 0) {
            sender.sendMessage(ChatColor.RED + "Seconds must be > 0.");
            return true;
        }

        LevelPlugin.getInstance().setTokenBooster(multiplier, seconds);

        long mins = seconds / 60;
        long secs = seconds % 60;

        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD +
                "GLOBAL TOKEN BOOST ACTIVATED!");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "Multiplier: " + ChatColor.WHITE + multiplier + "x");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "Duration: " + ChatColor.WHITE + mins + "m " + secs + "s");
        return true;
    }
}