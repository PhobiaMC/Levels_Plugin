package com.phobia.levels;

import java.text.NumberFormat;
import java.util.Locale;

import org.bukkit.entity.Player;

import com.phobia.levels.data.PlayerData;

public class LevelsAPI {

    private final LevelPlugin plugin;

    public LevelsAPI(LevelPlugin plugin) {
        this.plugin = plugin;
    }

    public int getLevel(Player p) {
        PlayerData data = plugin.getPlayerDataManager().getData(p);
        return (data != null) ? data.getLevel() : 1;
    }

    // Helper for formatting numbers with commas (e.g., 1,000)
    public static String format(int amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }

    // =========================================================
    // >>> TOKEN & BANK METHODS FOR CROSS-PLUGIN ACCESS
    // =========================================================

    public static int getTokens(Player p) {
        PlayerData data = LevelPlugin.getInstance().getPlayerDataManager().getData(p);
        return (data != null) ? data.getTokens() : 0;
    }

    public static int getBankBalance(Player p) {
        PlayerData data = LevelPlugin.getInstance().getPlayerDataManager().getData(p);
        return (data != null) ? data.getBankBalance() : 0;
    }

    public static boolean hasTokens(Player p, int amount) {
        PlayerData data = LevelPlugin.getInstance().getPlayerDataManager().getData(p);
        return data != null && data.getTokens() >= amount;
    }

    public static void addTokens(Player p, int amount) {
        PlayerData data = LevelPlugin.getInstance().getPlayerDataManager().getData(p);
        if (data != null) {
            data.addTokens(amount);
            LevelPlugin.getInstance().getPlayerDataManager().save(p);
        }
    }

    public static boolean takeTokens(Player p, int amount) {
        PlayerData data = LevelPlugin.getInstance().getPlayerDataManager().getData(p);
        if (data == null) return false;

        boolean success = data.removeTokens(amount);
        if (success) LevelPlugin.getInstance().getPlayerDataManager().save(p);
        return success;
    }

    public static boolean deposit(Player p, int amount) {
        PlayerData data = LevelPlugin.getInstance().getPlayerDataManager().getData(p);
        if (data == null) return false;

        boolean success = data.deposit(amount);
        if (success) LevelPlugin.getInstance().getPlayerDataManager().save(p);
        return success;
    }

    public static boolean withdraw(Player p, int amount) {
        PlayerData data = LevelPlugin.getInstance().getPlayerDataManager().getData(p);
        if (data == null) return false;

        boolean success = data.withdraw(amount);
        if (success) LevelPlugin.getInstance().getPlayerDataManager().save(p);
        return success;
    }
}