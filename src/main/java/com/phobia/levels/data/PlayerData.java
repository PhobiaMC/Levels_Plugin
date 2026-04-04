package com.phobia.levels.data;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.phobia.levels.LevelPlugin;
import com.phobia.levels.api.PlayerLevelUpEvent;

public class PlayerData {

    private final Player player;
    private int level;
    private int xp;
    private int requiredXp;
    private int kills;
    private int mobKills;
    private int deaths;
    private int tokens; // This is now "Pocket Cash"
    private int bankBalance; // NEW: Bank balance

    public PlayerData(Player player) {
        this.player = player;
        this.level = 1;
        this.xp = 0;
        this.requiredXp = calculateRequiredXp(1);
        this.kills = 0;
        this.mobKills = 0;
        this.deaths = 0;
        this.tokens = 0;
        this.bankBalance = 0;
    }

    private int calculateRequiredXp(int currentLevel) {
        int base = 100;
        int linearScaling = currentLevel * 50;
        int exponentialScaling = (int) (Math.pow(currentLevel, 2) * 10);
        return base + linearScaling + exponentialScaling;
    }

    public void resetProgress() {
        this.level = 1;
        this.xp = 0;
        this.requiredXp = calculateRequiredXp(1);
        this.tokens = 0;
        this.bankBalance = 0;
        player.sendMessage("§c§lRESET! §7An administrator has reset your levels, XP, and bank account.");
    }

    public Player getPlayer() { return player; }

    public void load(FileConfiguration config) {
        this.level = config.getInt("level", 1);
        this.xp = config.getInt("xp", 0);
        this.requiredXp = calculateRequiredXp(this.level);
        this.kills = config.getInt("kills", 0);
        this.mobKills = config.getInt("mobKills", 0);
        this.deaths = config.getInt("deaths", 0);
        this.tokens = config.getInt("tokens", 0);
        this.bankBalance = config.getInt("bankBalance", 0); // NEW: Load bank data
    }

    public void save(FileConfiguration config) {
        config.set("level", level);
        config.set("xp", xp);
        config.set("requiredXp", requiredXp);
        config.set("kills", kills);
        config.set("mobKills", mobKills);
        config.set("deaths", deaths);
        config.set("tokens", tokens);
        config.set("bankBalance", bankBalance); // NEW: Save bank data
    }

    // --- NEW: Banking Logic ---
    public int getBankBalance() { return bankBalance; }

    public boolean deposit(int amount) {
        if (tokens >= amount) {
            tokens -= amount;
            bankBalance += amount;
            return true;
        }
        return false;
    }

    public boolean withdraw(int amount) {
        if (bankBalance >= amount) {
            bankBalance -= amount;
            tokens += amount;
            return true;
        }
        return false;
    }

    public int getLevel() { return level; }
    public int getXp() { return xp; }
    public int getRequiredXp() { return requiredXp; }
    public int getKills() { return kills; }
    public int getMobKills() { return mobKills; }
    public int getDeaths() { return deaths; }
    public int getTokens() { return tokens; }

    public void setLevel(int level) { 
        this.level = level; 
        this.requiredXp = calculateRequiredXp(level);
    }
    
    public void setXp(int xp) { this.xp = xp; }
    public void setRequiredXp(int requiredXp) { this.requiredXp = requiredXp; }
    public void setKills(int kills) { this.kills = kills; }
    public void setMobKills(int mobKills) { this.mobKills = mobKills; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    public void setTokens(int tokens) { this.tokens = tokens; }

    public void addXp(int amount) {
        this.xp += amount;
        checkLevelUp();
    }

    public void addKill() { this.kills++; }
    public void addMobKill() { this.mobKills++; }
    public void addDeath() { this.deaths++; }

    public void addTokens(int amount) {
        this.tokens += amount;
    }

    public boolean removeTokens(int amount) {
        if (tokens < amount) return false;
        tokens -= amount;
        return true;
    }

    public double getKdr() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public double getTkdr() {
        int totalKills = kills + mobKills;
        return deaths == 0 ? totalKills : (double) totalKills / deaths;
    }

    private void checkLevelUp() {
        FileConfiguration config = LevelPlugin.getInstance().getConfig();
        
        while (xp >= requiredXp) {
            xp -= requiredXp;
            level++;
            requiredXp = calculateRequiredXp(level);

            Bukkit.getPluginManager().callEvent(new PlayerLevelUpEvent(player, level));

            String path = "level-up.custom." + level;
            String message;
            int tokenReward;

            if (config.contains(path)) {
                message = config.getString(path + ".message");
                tokenReward = config.getInt(path + ".tokens", 25);
                
                if (config.contains(path + ".commands")) {
                    for (String cmd : config.getStringList(path + ".commands")) {
                        String formattedCmd = cmd.replace("%player%", player.getName()).replace("%level%", String.valueOf(level));
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd);
                    }
                }
            } else {
                message = config.getString("level-up.default-message", "§a§lLEVEL UP! §7You are now level §b§l%level%§7!");
                tokenReward = config.getInt("level-up.default-tokens", 25);
            }

            this.tokens += tokenReward;
            player.sendMessage(message.replace("%level%", String.valueOf(level)).replace("&", "§"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.sendMessage("§eYou received §6" + tokenReward + " tokens§e!");
        }
    }
}