package com.phobia.levels.managers;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.phobia.levels.LevelPlugin;
import com.phobia.levels.data.PlayerData;

public class LevelManager {

    /**
     * Applies the XP amount, now including checks for Armor Set Bonuses.
     */
    public void addXp(Player player, int amount) {
        PlayerData data = LevelPlugin.getInstance().getPlayerDataManager().getData(player);
        
        // Block XP accumulation if the player is at the maximum level 120 cap
        if (data.getLevel() >= 120) {
            return;
        }

        // --- NEW: 1.5x Gold Armor XP Boost ---
        if (isWearingFullGold(player)) {
            amount = (int) (amount * 1.5);
        }
        
        data.addXp(amount);
    }

    /**
     * Helper to check if player is wearing full Golden Armor
     */
    private boolean isWearingFullGold(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor == null) return false;
        for (ItemStack item : armor) {
            if (item == null || !item.getType().name().startsWith("GOLDEN_")) {
                return false;
            }
        }
        return true;
    }

    public int getLevel(Player player) {
        return LevelPlugin.getInstance().getPlayerDataManager().getData(player).getLevel();
    }

    public int getXp(Player player) {
        return LevelPlugin.getInstance().getPlayerDataManager().getData(player).getXp();
    }

    public int getRequiredXp(Player player) {
        return LevelPlugin.getInstance().getPlayerDataManager().getData(player).getRequiredXp();
    }
}