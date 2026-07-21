package com.phobia.levels.managers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.phobia.levels.LevelPlugin;
import com.phobia.levels.data.PlayerData;

public class PlayerDataManager {

    private final Map<Player, PlayerData> dataMap = new HashMap<>();

    public PlayerData getData(Player player) {
        if (dataMap.containsKey(player)) {
            return dataMap.get(player);
        }
        PlayerData data = new PlayerData(player);
        load(player, data);
        dataMap.put(player, data);
        return data;
    }

    private void load(Player player, PlayerData data) {
        File file = getPlayerFile(player);
        if (!file.exists()) {
            save(player);
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        data.load(config);
    }

    public void saveAll() {
        for (Player player : dataMap.keySet()) {
            save(player);
        }
    }

    public void save(Player player) {
        PlayerData data = dataMap.get(player);
        if (data == null) return;
        File file = getPlayerFile(player);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        data.save(config);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveData(Player player) {
        save(player);
    }

    public void saveAndRemove(Player player) {
        save(player);
        dataMap.remove(player);
    }

    public void unload(Player player) {
        save(player);
        dataMap.remove(player);
    }

    public File getPlayerFile(Player player) {
        return new File(LevelPlugin.getInstance().getDataFolder(),
                "playerdata/" + player.getUniqueId() + ".yml");
    }

    // -------- OFFLINE PLAYER SUPPORT ----------------

    public File getOfflinePlayerFile(OfflinePlayer offline) {
        return new File(LevelPlugin.getInstance().getDataFolder(),
                "playerdata/" + offline.getUniqueId() + ".yml");
    }

    /**
     * If the player is currently online, returns their LIVE in-memory data
     * to avoid a stale file read racing against unsaved in-memory state.
     * Otherwise loads directly from their YML file on disk.
     */
    public PlayerData loadOfflineData(OfflinePlayer offline) {
        // If they're online, return the live object — never read stale file data
        if (offline.isOnline() && offline.getPlayer() != null) {
            return getData(offline.getPlayer());
        }

        File file = getOfflinePlayerFile(offline);
        if (!file.exists()) return new PlayerData(null);

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        PlayerData data = new PlayerData(null);
        data.load(config);
        return data;
    }

    /**
     * Saves a PlayerData object for an offline player directly to their YML file.
     * Should only be called when the player is confirmed offline — if they're
     * online their live dataMap entry would be out of sync.
     */
    public void saveOfflineData(OfflinePlayer offline, PlayerData data) {
        File file = getOfflinePlayerFile(offline);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        data.save(config);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}