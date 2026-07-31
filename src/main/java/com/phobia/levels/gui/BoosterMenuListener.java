package com.phobia.levels.gui;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import com.phobia.levels.LevelPlugin;
import com.phobia.levels.boosters.BoosterScope;
import com.phobia.levels.gui.BoosterMenuHolder.ViewType;

public class BoosterMenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof BoosterMenuHolder menuHolder)) return;

        // Cancel everything by default — blocks shift-click, hotbar-swap (number keys),
        // and offhand swap from moving items in or out of this menu.
        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || !(event.getClickedInventory().getHolder() instanceof BoosterMenuHolder)) {
            return; // click landed in the player's own inventory while the menu was open
        }

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        if (menuHolder.getViewType() == ViewType.MAIN) {
            if (slot == 11) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f); // ADDED
                BoosterMenu.openScope(player, BoosterScope.PERSONAL);
            } else if (slot == 15) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f); // ADDED
                BoosterMenu.openScope(player, BoosterScope.GLOBAL);
            }
            return;
        }

        if (slot == 26) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f); // ADDED
            BoosterMenu.openMain(player);
            return;
        }

        String boosterId = menuHolder.getBoosterId(slot);
        if (boosterId == null) return;

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f); // ADDED
        boolean activated = LevelPlugin.getInstance().getBoosterManager().activateBooster(player, boosterId);
        if (activated) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BoosterMenuHolder) {
            event.setCancelled(true);
        }
    }
}