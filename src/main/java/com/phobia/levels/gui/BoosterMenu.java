package com.phobia.levels.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.phobia.levels.LevelPlugin;
import com.phobia.levels.boosters.Booster;
import com.phobia.levels.boosters.BoosterScope;
import com.phobia.levels.boosters.BoosterType;
import com.phobia.levels.data.PlayerData;
import com.phobia.levels.gui.BoosterMenuHolder.ViewType;

public class BoosterMenu {

    public static void openMain(Player player) {
        BoosterMenuHolder holder = new BoosterMenuHolder(ViewType.MAIN);
        Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.DARK_GRAY + "Booster Menu");
        holder.setInventory(inv);

        inv.setItem(11, namedItem(Material.PLAYER_HEAD, ChatColor.AQUA + "" + ChatColor.BOLD + "Personal Boosters",
                ChatColor.GRAY + "Click to view your personal", ChatColor.GRAY + "boosters."));

        inv.setItem(15, namedItem(Material.GRASS_BLOCK, ChatColor.GOLD + "" + ChatColor.BOLD + "Global Boosters",
                ChatColor.GRAY + "Click to view your server-wide", ChatColor.GRAY + "boosters."));

        player.openInventory(inv);
    }

    public static void openScope(Player player, BoosterScope scope) {
        ViewType viewType = scope == BoosterScope.PERSONAL ? ViewType.PERSONAL : ViewType.GLOBAL;
        BoosterMenuHolder holder = new BoosterMenuHolder(viewType);

        String title = scope == BoosterScope.PERSONAL
                ? ChatColor.AQUA + "Personal Boosters"
                : ChatColor.GOLD + "Global Boosters";

        Inventory inv = Bukkit.createInventory(holder, 27, title);
        holder.setInventory(inv);

        PlayerData data = LevelPlugin.getInstance().getPlayerDataManager().getData(player);

        List<Booster> matching = new ArrayList<>();
        for (Booster booster : data.getPendingBoosters()) {
            if (booster.getScope() == scope) {
                matching.add(booster);
            }
        }

        int slot = 0;
        for (Booster booster : matching) {
            if (slot >= 26) break; // slot 26 reserved for the back button

            Material material = booster.getType() == BoosterType.XP ? Material.EXPERIENCE_BOTTLE : Material.GOLD_NUGGET;
            String displayName = booster.getType() == BoosterType.XP
                    ? ChatColor.LIGHT_PURPLE + "XP Booster"
                    : ChatColor.YELLOW + "Token Booster";

            ItemStack item = namedItem(material, displayName,
                    ChatColor.GRAY + "Multiplier: " + ChatColor.WHITE + "x" + booster.getMultiplier(),
                    ChatColor.GRAY + "Duration: " + ChatColor.WHITE + booster.getMinutes() + "m",
                    "",
                    ChatColor.GREEN + "Click to activate!");

            inv.setItem(slot, item);
            holder.mapSlot(slot, booster.getId());
            slot++;
        }

        inv.setItem(26, namedItem(Material.ARROW, ChatColor.RED + "Back"));

        player.openInventory(inv);
    }

    private static ItemStack namedItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(List.of(lore));
        }
        item.setItemMeta(meta);
        return item;
    }
}