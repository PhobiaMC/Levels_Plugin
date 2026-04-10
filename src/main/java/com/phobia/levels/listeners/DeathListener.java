package com.phobia.levels.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.phobia.levels.LevelPlugin;
import com.phobia.levels.LevelsAPI;
import com.phobia.levels.data.PlayerData;
import com.phobia.levels.scoreboard.PlayerBoard;

public class DeathListener implements Listener {

    private final NamespacedKey tokenKey;

    public DeathListener() {
        this.tokenKey = new NamespacedKey(LevelPlugin.getInstance(), "dropped_tokens");
    }

    // CHANGED: Set priority to HIGHEST to ensure this processes FIRST
    // This way death counting happens once, before KillListener handles kill rewards
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData data = LevelPlugin.getInstance().getPlayerDataManager().getData(player);

        if (data == null) return;

        // FIXED: Re-enabled death tracking - this is the ONLY place deaths are counted now
        data.addDeath();
        
        // Update scoreboard for the death
        updateBoard(player);

        // --- Logic: Token Drop ---
        int currentPocket = data.getTokens();
        if (currentPocket > 0) {
            int toDrop = currentPocket / 2;
            data.setTokens(currentPocket - toDrop); 

            String formattedDrop = LevelsAPI.format(toDrop);

            player.sendMessage("");
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "DEATH! " + ChatColor.GRAY + "You died and dropped " + ChatColor.GOLD + formattedDrop + " Tokens " + ChatColor.GRAY + "from your pocket.");
            player.sendMessage(ChatColor.YELLOW + "Tip: " + ChatColor.GRAY + "Use an ATM to deposit tokens so they stay safe!");
            player.sendMessage("");

            ItemStack nugget = new ItemStack(Material.GOLD_NUGGET);
            ItemMeta meta = nugget.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "$" + formattedDrop + " Tokens");
                meta.getPersistentDataContainer().set(tokenKey, PersistentDataType.INTEGER, toDrop);
                nugget.setItemMeta(meta);
            }

            Item dropped = player.getWorld().dropItemNaturally(player.getLocation(), nugget);
            dropped.setCustomName(ChatColor.YELLOW + "Dropped Tokens: " + ChatColor.GOLD + "$" + formattedDrop);
            dropped.setCustomNameVisible(true);
        }

        // Save data immediately
        LevelPlugin.getInstance().getPlayerDataManager().save(player);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        ItemStack itemStack = event.getItem().getItemStack();
        if (itemStack == null || itemStack.getType() != Material.GOLD_NUGGET || !itemStack.hasItemMeta()) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta.getPersistentDataContainer().has(tokenKey, PersistentDataType.INTEGER)) {
            Player player = (Player) event.getEntity();
            Integer amount = meta.getPersistentDataContainer().get(tokenKey, PersistentDataType.INTEGER);

            if (amount != null) {
                PlayerData data = LevelPlugin.getInstance().getPlayerDataManager().getData(player);
                data.addTokens(amount);

                event.setCancelled(true); 
                event.getItem().remove(); 

                player.sendMessage(ChatColor.GOLD + "§l+ " + LevelsAPI.format(amount) + " Tokens §7(Picked up)");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            }
        }
    }
    
    private void updateBoard(Player p) {
        PlayerBoard board = LevelPlugin.getInstance().getScoreboardHandler().getBoard(p);
        if (board != null) board.update();
    }
}