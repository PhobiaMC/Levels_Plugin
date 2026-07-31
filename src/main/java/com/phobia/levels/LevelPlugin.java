package com.phobia.levels;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.phobia.levels.boosters.BoosterManager;
import com.phobia.levels.boosters.BoosterType;
import com.phobia.levels.commands.BalanceTopCommand;
import com.phobia.levels.commands.BoosterMenuCommand;
import com.phobia.levels.commands.GiveBoosterCommand;
import com.phobia.levels.commands.GiveTokensCommand;
import com.phobia.levels.commands.GiveXpCommand;
import com.phobia.levels.commands.LevelCommand;
import com.phobia.levels.commands.PrestigeCommand;
import com.phobia.levels.commands.ProfileCommand;
import com.phobia.levels.commands.TokenAdminCommand;
import com.phobia.levels.commands.TokenBoostCommand;
import com.phobia.levels.commands.XpBoostCommand;
import com.phobia.levels.gui.BoosterMenuListener;
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
    private BoosterManager boosterManager; // ADDED

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.playerDataManager = new PlayerDataManager();
        this.levelManager = new LevelManager();
        this.scoreboardHandler = new ScoreboardHandler();
        this.boosterManager = new BoosterManager(this); // ADDED — must exist before any command/listener touches it
        BalanceTopCommand balTop = new BalanceTopCommand();

        File dataFolder = new File(getDataFolder(), "playerdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();

        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(), this);
        Bukkit.getPluginManager().registerEvents(new KillListener(), this);
        Bukkit.getPluginManager().registerEvents(new BoosterMenuListener(), this); // ADDED

        getCommand("level").setExecutor(new LevelCommand());
        getCommand("profile").setExecutor(new ProfileCommand());
        getCommand("givetokens").setExecutor(new GiveTokensCommand());
        getCommand("givexp").setExecutor(new GiveXpCommand());
        getCommand("xpboost").setExecutor(new XpBoostCommand());
        getCommand("tokenboost").setExecutor(new TokenBoostCommand());
        getCommand("tokenadmin").setExecutor(new TokenAdminCommand());
        getCommand("baltop").setExecutor(balTop);
        getCommand("banktop").setExecutor(balTop);
        getCommand("prestige").setExecutor(new PrestigeCommand());
        getCommand("givebooster").setExecutor(new GiveBoosterCommand()); // ADDED
        getCommand("boosters").setExecutor(new BoosterMenuCommand()); // ADDED

        this.scoreboardHandler.start();

        Bukkit.getPluginManager().registerEvents(new DeathListener(), this);

        // CHANGED: this timer now just asks BoosterManager to check + broadcast expiry,
        // instead of holding the multiplier/expiry fields here directly.
        Bukkit.getScheduler().runTaskTimer(this, boosterManager::tick, 20L, 20L);

        Bukkit.getConsoleSender().sendMessage("§a[Levels] Plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) playerDataManager.saveAll();
        if (scoreboardHandler != null) scoreboardHandler.shutdown();
        if (boosterManager != null) boosterManager.saveGlobal(); // ADDED, cheap safety net
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

    public BoosterManager getBoosterManager() { // ADDED
        return boosterManager;
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

    // CHANGED: these six methods now delegate to BoosterManager instead of
    // holding their own fields, so PlayerBoard / TokenBoostCommand / XpBoostCommand
    // don't need to change at all.

    public double getGlobalBooster() {
        return boosterManager.getGlobalMultiplier(BoosterType.XP);
    }

    public void setGlobalBooster(double multiplier, int seconds) {
        boosterManager.setGlobalBooster(BoosterType.XP, multiplier, seconds);
    }

    public long getBoosterTimeRemaining() {
        return boosterManager.getGlobalRemainingSeconds(BoosterType.XP);
    }

    public double getTokenBooster() {
        return boosterManager.getGlobalMultiplier(BoosterType.TOKENS);
    }

    public void setTokenBooster(double multiplier, int seconds) {
        boosterManager.setGlobalBooster(BoosterType.TOKENS, multiplier, seconds);
    }

    public long getTokenBoosterTimeRemaining() {
        return boosterManager.getGlobalRemainingSeconds(BoosterType.TOKENS);
    }
}