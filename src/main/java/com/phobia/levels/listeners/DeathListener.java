package com.phobia.levels.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.phobia.levels.LevelPlugin;
import com.phobia.levels.LevelsAPI;
import com.phobia.levels.data.PlayerData;

public class DeathListener implements Listener {

    private final NamespacedKey tokenKey;

    public DeathListener() {
        this.tokenKey = new NamespacedKey(LevelPlugin.getInstance(), "dropped_tokens");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData data = LevelPlugin.getInstance().getPlayerDataManager().getData(player);

        // Track the death
        data.addDeath();

        // --- Logic: Token Drop ---
        int currentPocket = data.getTokens();
        if (currentPocket > 0) {
            int toDrop = currentPocket / 2;
            data.setTokens(currentPocket - toDrop); // Update data in memory

            String formattedDrop = LevelsAPI.format(toDrop);

            // Notify player of loss
            player.sendMessage("");
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "DEATH! " + ChatColor.GRAY + "You died and dropped " + ChatColor.GOLD + formattedDrop + " Tokens " + ChatColor.GRAY + "from your pocket.");
            player.sendMessage(ChatColor.YELLOW + "Tip: " + ChatColor.GRAY + "Use an ATM to deposit tokens so they stay safe!");
            player.sendMessage("");

            // Create the Token Nugget
            ItemStack nugget = new ItemStack(Material.GOLD_NUGGET);
            ItemMeta meta = nugget.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "$" + formattedDrop + " Tokens");
                meta.getPersistentDataContainer().set(tokenKey, PersistentDataType.INTEGER, toDrop);
                nugget.setItemMeta(meta);
            }

            // Spawn the item
            Item dropped = player.getWorld().dropItemNaturally(player.getLocation(), nugget);
            dropped.setCustomName(ChatColor.YELLOW + "Dropped Tokens: " + ChatColor.GOLD + "$" + formattedDrop);
            dropped.setCustomNameVisible(true);
        }

        // Save data
        LevelPlugin.getInstance().getPlayerDataManager().save(player);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        ItemStack itemStack = event.getItem().getItemStack();
        if (itemStack.getType() != Material.GOLD_NUGGET || !itemStack.hasItemMeta()) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta.getPersistentDataContainer().has(tokenKey, PersistentDataType.INTEGER)) {
            Player player = (Player) event.getEntity();
            int amount = meta.getPersistentDataContainer().get(tokenKey, PersistentDataType.INTEGER);

            // Give tokens to collector
            PlayerData data = LevelPlugin.getInstance().getPlayerDataManager().getData(player);
            data.addTokens(amount);

            // Handle pickup mechanics
            event.setCancelled(true); 
            event.getItem().remove(); 

            player.sendMessage(ChatColor.GOLD + "§l+ " + LevelsAPI.format(amount) + " Tokens §7(Picked up)");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        }
    }
}