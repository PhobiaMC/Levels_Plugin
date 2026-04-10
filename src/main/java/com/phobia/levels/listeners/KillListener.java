package com.phobia.levels.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;

import com.phobia.levels.LevelPlugin;
import com.phobia.levels.data.PlayerData;
import com.phobia.levels.scoreboard.PlayerBoard;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class KillListener implements Listener {

    private final Map<Integer, Set<UUID>> mobContributors = new HashMap<>();
    private final Map<Integer, UUID> lastKiller = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity) || event.getEntity() instanceof Player) return;

        Player damager = getDamager(event.getDamager());
        if (damager == null) return;

        int entityId = event.getEntity().getEntityId();
        mobContributors.computeIfAbsent(entityId, k -> new HashSet<>()).add(damager.getUniqueId());
        lastKiller.put(entityId, damager.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;

        LivingEntity victim = event.getEntity();
        int entityId = victim.getEntityId();

        if (!mobContributors.containsKey(entityId)) return;

        Set<UUID> contributors = mobContributors.remove(entityId);
        UUID killerUUID = lastKiller.remove(entityId);

        if (contributors == null) return;

        Player killer = (killerUUID != null) ? Bukkit.getPlayer(killerUUID) : null;
        if (victim.getKiller() != null) killer = victim.getKiller();

        int mobLevel = 1;
        if (victim.hasMetadata("mob_level")) {
            for (MetadataValue value : victim.getMetadata("mob_level")) {
                mobLevel = value.asInt();
                break; 
            }
        }

        int finalXp = mobLevel; 
        int finalTokens = Math.max(1, (int) Math.ceil(mobLevel / 10.0));

        if (killer != null && killer.isOnline()) {
            reward(killer, finalTokens, finalXp, false, false); // isPlayerKill = false
            contributors.remove(killer.getUniqueId());
        }

        int assistTokens = Math.max(1, finalTokens / 3);
        int assistXp = Math.max(1, finalXp / 3);

        for (UUID uuid : contributors) {
            Player assistant = Bukkit.getPlayer(uuid);
            if (assistant != null && assistant.isOnline()) {
                reward(assistant, assistTokens, assistXp, true, false);
            }
        }
    }

    private void reward(Player player, int tokens, int baseXp, boolean isAssist, boolean isPlayerKill) {
        PlayerData data = LevelPlugin.getInstance().getPlayerDataManager().getData(player);
        
        // --- FIXED: Distinguish between Player kills and Mob kills ---
        if (!isAssist) {
            if (isPlayerKill) {
                data.addKill(); 
            } else {
                data.addMobKill();
            }
        }
        
        if (tokens > 0) data.addTokens(tokens);

        double multi = LevelPlugin.getInstance().getPlayerMultiplier(player);
        double global = LevelPlugin.getInstance().getGlobalBooster();
        double armorMulti = isWearingFullGold(player) ? 1.5 : 1.0;
        
        int finalXp = (int) Math.round(baseXp * multi * global * armorMulti);

        data.addXp(finalXp);
        updateBoard(player);
        LevelPlugin.getInstance().getPlayerDataManager().save(player);

        String prefix = isAssist ? ChatColor.GRAY + "[Assist] " : "";
        String bonusTag = (armorMulti > 1.0) ? ChatColor.GOLD + " (1.5x Bonus)" : "";
        
        player.spigot().sendMessage(
            ChatMessageType.ACTION_BAR,
            new TextComponent(prefix + ChatColor.GREEN + "+" + finalXp + " XP" + bonusTag
            + ChatColor.GRAY + " |" + ChatColor.YELLOW + " +" + tokens + " Tokens")
        );
    }

    private Player getDamager(org.bukkit.entity.Entity entity) {
        if (entity instanceof Player) return (Player) entity;
        if (entity instanceof Projectile) {
            Projectile p = (Projectile) entity;
            if (p.getShooter() instanceof Player) return (Player) p.getShooter();
        }
        return null;
    }

    // CHANGED: Set priority to LOWEST so DeathListener processes first
    // This prevents duplicate death counting since DeathListener handles it
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // REMOVED: Death tracking moved to DeathListener to prevent duplicates
        // Only handle kill rewards here
        
        if (killer == null) return;

        // FIXED: Now correctly passes 'true' for a player kill
        reward(killer, 5, 25, false, true); 
    }

    private void updateBoard(Player p) {
        PlayerBoard board = LevelPlugin.getInstance().getScoreboardHandler().getBoard(p);
        if (board != null) board.update();
    }

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
}