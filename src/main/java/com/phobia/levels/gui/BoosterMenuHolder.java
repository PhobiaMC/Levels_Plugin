package com.phobia.levels.gui;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BoosterMenuHolder implements InventoryHolder {

    public enum ViewType { MAIN, PERSONAL, GLOBAL }

    private final ViewType viewType;
    private Inventory inventory;
    private final Map<Integer, String> slotToBoosterId = new HashMap<>();

    public BoosterMenuHolder(ViewType viewType) {
        this.viewType = viewType;
    }

    public ViewType getViewType() { return viewType; }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }

    public void mapSlot(int slot, String boosterId) {
        slotToBoosterId.put(slot, boosterId);
    }

    public String getBoosterId(int slot) {
        return slotToBoosterId.get(slot);
    }
}