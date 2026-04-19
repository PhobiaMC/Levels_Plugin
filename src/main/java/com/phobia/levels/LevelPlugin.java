package com.phobia.levels;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.phobia.levels.commands.BalanceTopCommand;
import com.phobia.levels.commands.GiveTokensCommand;
import com.phobia.levels.commands.GiveXpCommand;
import com.phobia.levels.commands.LevelCommand;
import com.phobia.levels.commands.ProfileCommand;
import com.phobia.levels.commands.TokenAdminCommand;
import com.phobia.levels.commands.XpBoostCommand;
import com.phobia.levels.listeners.DeathListener;
import com.phobia.levels.listeners.KillListener;
import com.phobia.levels.listeners.PlayerJoinListener;
import com.phobia.levels.listeners.PlayerQuitListener;
import com.phobia.levels.managers.LevelManager;
import com.phobia.levels.managers.PlayerDataManager;
import com.phobia.levels.scoreboard.ScoreboardHandler;

public class LevelPlugin extends JavaPlugin {

    private static LevelPlugin instance;

    private PlayerDataManager playerDataManager;
    private LevelManager levelManager;
    private ScoreboardHandler scoreboardHandler;

    private double globalXpBoost = 1.0;
    private long boostExpireTime = 0;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.playerDataManager = new PlayerDataManager();
        this.levelManager = new LevelManager();
        this.scoreboardHandler = new ScoreboardHandler();
        BalanceTopCommand balTop = new BalanceTopCommand();

        File dataFolder = new File(getDataFolder(), "playerdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();

        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(), this);
        Bukkit.getPluginManager().registerEvents(new KillListener(), this);

        getCommand("level").setExecutor(new LevelCommand());
        getCommand("profile").setExecutor(new ProfileCommand());
        getCommand("givetokens").setExecutor(new GiveTokensCommand());
        getCommand("givexp").setExecutor(new GiveXpCommand());
        getCommand("xpboost").setExecutor(new XpBoostCommand());
        getCommand("tokenadmin").setExecutor(new TokenAdminCommand());
        getCommand("baltop").setExecutor(balTop);
        getCommand("banktop").setExecutor(balTop);

        this.scoreboardHandler.start();

        Bukkit.getPluginManager().registerEvents(new DeathListener(), this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (boostExpireTime != 0 && System.currentTimeMillis() > boostExpireTime) {
                globalXpBoost = 1.0;
                boostExpireTime = 0;
                Bukkit.broadcastMessage("§eThe global XP boost has ended.");
            }
        }, 20L, 20L);

        Bukkit.getConsoleSender().sendMessage("§a[Levels] Plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) playerDataManager.saveAll();
        if (scoreboardHandler != null) scoreboardHandler.shutdown();
        Bukkit.getConsoleSender().sendMessage("§c[Levels] Plugin disabled.");
    }

    public static LevelPlugin getInstance() {
        return instance;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public ScoreboardHandler getScoreboardHandler() {
        return scoreboardHandler;
    }

    public double getPlayerMultiplier(org.bukkit.entity.Player p) {
        double highest = 1.0;

        for (var perm : p.getEffectivePermissions()) {
            String name = perm.getPermission().toLowerCase();

            if (name.startsWith("levels.multiplier.")) {
                String raw = name.replace("levels.multiplier.", "");
                try {
                    double value = Double.parseDouble(raw);
                    if (value > highest) highest = value;
                } catch (Exception ignored) {}
            }
        }

        return highest;
    }

    public double getGlobalBooster() {
        return globalXpBoost;
    }

    public void setGlobalBooster(double multiplier, int seconds) {
        this.globalXpBoost = multiplier;
        this.boostExpireTime = System.currentTimeMillis() + (seconds * 1000L);
    }

    public long getBoosterTimeRemaining() {
        if (boostExpireTime == 0) return 0;
        long diff = boostExpireTime - System.currentTimeMillis();
        return Math.max(diff / 1000, 0);
    }
}