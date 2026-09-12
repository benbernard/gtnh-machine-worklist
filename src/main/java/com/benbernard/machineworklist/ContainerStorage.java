package com.benbernard.machineworklist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;

/** Storage exposed by the currently open supported container; never scan neighboring blocks. */
final class ContainerStorage {

    private ContainerStorage() {}

    static boolean canTake(Slot slot, net.minecraft.client.gui.inventory.GuiContainer gui) {
        return slot.canTakeStack(gui == null || gui.mc == null ? null : gui.mc.thePlayer);
    }

    static List<Slot> outputSlots(Container container, boolean backpack) {
        List<Slot> result = slots(container, backpack);
        if (backpack) result.removeIf(slot -> BackpackLayout.craftingStorage(slot.slotNumber));
        return result;
    }

    static List<Slot> slots(Container container, boolean backpack) {
        List<Slot> result = new ArrayList<>();
        Map<IInventory, Set<Integer>> seen = new IdentityHashMap<>();
        for (Slot slot : container.inventorySlots) {
            boolean storage = backpack ? BackpackLayout.storage(slot.slotNumber)
                : slot.inventory != null && !(slot.inventory instanceof InventoryPlayer)
                    && !(slot.inventory instanceof net.minecraft.inventory.InventoryCraftResult)
                    && !(slot.inventory instanceof InventoryCrafting)
                    && !(slot instanceof SlotCrafting);
            if (storage && seen.computeIfAbsent(slot.inventory, ignored -> new HashSet<>())
                .add(slot.getSlotIndex())) result.add(slot);
        }
        return result;
    }
}
