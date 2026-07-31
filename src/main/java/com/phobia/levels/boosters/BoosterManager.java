package com.phobia.levels.boosters;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.phobia.levels.LevelPlugin;
import com.phobia.levels.data.PlayerData;

public class BoosterManager {

    private final LevelPlugin plugin;
    private final File globalFile;

    private ActiveBooster activeGlobalXp;
    private ActiveBooster activeGlobalTokens;

    public BoosterManager(LevelPlugin plugin) {
        this.plugin = plugin;
        this.globalFile = new File(plugin.getDataFolder(), "boosters.yml");
        loadGlobal();
    }

    // ---------------- Global persistence (boosters.yml) ----------------

    private void loadGlobal() {
        if (!globalFile.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(globalFile);

        ActiveBooster xp = ActiveBooster.deserialize(config.getString("global-xp"));
        if (xp != null && xp.isActive()) activeGlobalXp = xp;

        ActiveBooster tokens = ActiveBooster.deserialize(config.getString("global-tokens"));
        if (tokens != null && tokens.isActive()) activeGlobalTokens = tokens;
    }

    public void saveGlobal() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(globalFile);

        config.set("global-xp", (activeGlobalXp != null && activeGlobalXp.isActive()) ? activeGlobalXp.serialize() : null);
        config.set("global-tokens", (activeGlobalTokens != null && activeGlobalTokens.isActive()) ? activeGlobalTokens.serialize() : null);

        try {
            config.save(globalFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Called once per second by LevelPlugin's scheduler. Detects global expiry and broadcasts it. */
    public void tick() {
        if (activeGlobalXp != null && !activeGlobalXp.isActive()) {
            activeGlobalXp = null;
            saveGlobal();
            Bukkit.broadcastMessage(ChatColor.YELLOW + "The global XP boost has ended.");
        }

        if (activeGlobalTokens != null && !activeGlobalTokens.isActive()) {
            activeGlobalTokens = null;
            saveGlobal();
            Bukkit.broadcastMessage(ChatColor.YELLOW + "The global Token boost has ended.");
        }
    }

    // ---------------- Giving pending boosters (admin command) ----------------

    /** Works for online or offline targets. */
    public void givePendingBooster(OfflinePlayer target, BoosterType type, BoosterScope scope, double multiplier, int minutes) {
        PlayerData data = plugin.getPlayerDataManager().loadOfflineData(target);
        data.getPendingBoosters().add(new Booster(type, scope, multiplier, minutes));

        if (target.isOnline() && target.getPlayer() != null) {
            plugin.getPlayerDataManager().saveData(target.getPlayer());
        } else {
            plugin.getPlayerDataManager().saveOfflineData(target, data);
        }
    }

    // ---------------- Activation (from the GUI) ----------------

    public boolean activateBooster(Player player, String boosterId) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        Booster booster = data.findPendingBooster(boosterId);
        if (booster == null) return false;

        data.getPendingBoosters().remove(booster);

        if (booster.getScope() == BoosterScope.PERSONAL) {
            activatePersonal(player, data, booster);
        } else {
            activateGlobal(player, booster);
        }

        plugin.getPlayerDataManager().saveData(player);
        return true;
    }

    private void activatePersonal(Player player, PlayerData data, Booster booster) {
        ActiveBooster active = data.getActivePersonalBooster(booster.getType());
        if (active != null && active.isActive()) {
            active.extend(booster.getMultiplier(), booster.getMinutes());
        } else {
            long expiresAt = System.currentTimeMillis() + (booster.getMinutes() * 60_000L);
            active = new ActiveBooster(booster.getType(), booster.getMultiplier(), expiresAt);
            data.setActivePersonalBooster(booster.getType(), active);
        }

        String label = booster.getType() == BoosterType.XP ? "XP" : "TOKEN";
        String color = booster.getType() == BoosterType.XP ? ChatColor.LIGHT_PURPLE.toString() : ChatColor.YELLOW.toString();

        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "PERSONAL " + label + " BOOST ACTIVATED!");
        player.sendMessage(color + "Multiplier: " + ChatColor.WHITE + "x" + active.getMultiplier());
        player.sendMessage(color + "Duration: " + ChatColor.WHITE + formatTime(active.getRemainingSeconds()));

        // ADDED: personal activation sound — only the activating player hears it
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private void activateGlobal(Player player, Booster booster) {
        ActiveBooster active = booster.getType() == BoosterType.XP ? activeGlobalXp : activeGlobalTokens;

        if (active != null && active.isActive()) {
            active.extend(booster.getMultiplier(), booster.getMinutes());
        } else {
            long expiresAt = System.currentTimeMillis() + (booster.getMinutes() * 60_000L);
            active = new ActiveBooster(booster.getType(), booster.getMultiplier(), expiresAt);
        }

        if (booster.getType() == BoosterType.XP) {
            activeGlobalXp = active;
        } else {
            activeGlobalTokens = active;
        }
        saveGlobal();

        String label = booster.getType() == BoosterType.XP ? "XP" : "TOKEN";
        String color = booster.getType() == BoosterType.XP ? ChatColor.LIGHT_PURPLE.toString() : ChatColor.GOLD.toString();

        Bukkit.broadcastMessage(color + "" + ChatColor.BOLD + "GLOBAL " + label + " BOOST ACTIVATED!");
        Bukkit.broadcastMessage(ChatColor.WHITE + "Activated by: " + ChatColor.YELLOW + player.getName());
        Bukkit.broadcastMessage(ChatColor.GRAY + "Multiplier: " + ChatColor.WHITE + "x" + active.getMultiplier());
        Bukkit.broadcastMessage(ChatColor.GRAY + "Duration: " + ChatColor.WHITE + formatTime(active.getRemainingSeconds()));

        // ADDED: global activation sound — everyone online hears it
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.playSound(online.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    // ---------------- Queries (for scoreboard, XP/token math, etc.) ----------------

    public double getGlobalMultiplier(BoosterType type) {
        ActiveBooster active = type == BoosterType.XP ? activeGlobalXp : activeGlobalTokens;
        return (active != null && active.isActive()) ? active.getMultiplier() : 1.0;
    }

    public long getGlobalRemainingSeconds(BoosterType type) {
        ActiveBooster active = type == BoosterType.XP ? activeGlobalXp : activeGlobalTokens;
        return (active != null && active.isActive()) ? active.getRemainingSeconds() : 0;
    }

    /** Kept for compatibility with the existing /xpboost & /tokenboost instant-apply commands. */
    public void setGlobalBooster(BoosterType type, double multiplier, int seconds) {
        ActiveBooster active = (multiplier <= 1.0 || seconds <= 0)
                ? null
                : new ActiveBooster(type, multiplier, System.currentTimeMillis() + (seconds * 1000L));

        if (type == BoosterType.XP) {
            activeGlobalXp = active;
        } else {
            activeGlobalTokens = active;
        }
        saveGlobal();
    }

    public double getPersonalMultiplier(Player player, BoosterType type) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        ActiveBooster active = data.getActivePersonalBooster(type);
        return (active != null && active.isActive()) ? active.getMultiplier() : 1.0;
    }

    // ADDED: needed so the scoreboard (and eventually KillListener) can show/use remaining time
    public long getPersonalRemainingSeconds(Player player, BoosterType type) {
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        ActiveBooster active = data.getActivePersonalBooster(type);
        return (active != null && active.isActive()) ? active.getRemainingSeconds() : 0;
    }

    private String formatTime(long totalSeconds) {
        return (totalSeconds / 60) + "m " + (totalSeconds % 60) + "s";
    }
}