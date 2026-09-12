package com.benbernard.machineworklist;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;

import codechicken.nei.bookmark.BookmarkItem;

/** Recover only returned/reusable inputs from the transfer just submitted, never its result. */
final class CraftingReturns {

    private final List<ItemStack> tools = new ArrayList<>();

    CraftingReturns(List<BookmarkItem> inputs) {
        for (BookmarkItem input : inputs) if (input.factor == 0 || input.itemStack.getItem()
            .hasContainerItem(input.itemStack)) tools.add(input.itemStack.copy());
    }

    boolean hasTools() {
        return !tools.isEmpty();
    }

    boolean recognizes(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return false;
        // Durability/NBT may change, but metadata that selects a GT tool type must still match.
        for (ItemStack tool : tools) if (stack.getItem() == tool.getItem()
            && (tool.getItemDamage() == 32767 || stack.isItemStackDamageable() && !stack.getHasSubtypes()
                || stack.getItemDamage() == tool.getItemDamage()))
            return true;
        return false;
    }

    List<Slot> toolSlots(List<Slot> slots, boolean backpack) {
        List<Slot> result = new ArrayList<>();
        for (Slot slot : slots) {
            boolean input = backpack ? BackpackLayout.craftingStorage(slot.slotNumber)
                : slot.inventory instanceof net.minecraft.inventory.InventoryCrafting;
            if (input && !(slot instanceof SlotCrafting) && recognizes(slot.getStack())) result.add(slot);
        }
        return result;
    }

    boolean canRecover(GuiContainer gui) {
        return recognizes(gui.mc.thePlayer.inventory.getItemStack())
            || !toolSlots(gui.inventorySlots.inventorySlots, CraftingInventory.backpack(gui)).isEmpty();
    }

    void refreshAfterConfirmation(GuiContainer gui) {
        // NEI already cleared the real grid. A stale derived mirror needs no additional slot clicks.
        if (CraftingInventory.backpack(gui)) synchronizeBackpack(gui);
    }

    void recover(GuiContainer gui) {
        boolean backpack = CraftingInventory.backpack(gui);
        if (backpack) synchronizeBackpack(gui);
        ItemStack cursor = gui.mc.thePlayer.inventory.getItemStack();
        if (cursor != null) {
            if (!recognizes(cursor)) return;
            // Despite its name, this helper only places into an empty player slot, never on the ground.
            codechicken.nei.FastTransferManager.dropHeldItem(gui);
            if (gui.mc.thePlayer.inventory.getItemStack() != null) return;
        }
        for (Slot slot : toolSlots(gui.inventorySlots.inventorySlots, backpack)) {
            if (gui.mc.thePlayer.inventory.getFirstEmptyStack() < 0) return;
            if (slot.canTakeStack(gui.mc.thePlayer))
                codechicken.nei.FastTransferManager.clickSlot(gui, slot.slotNumber, 0, 1);
        }
        if (backpack) synchronizeBackpack(gui);
    }

    private static void synchronizeBackpack(GuiContainer gui) {
        try {
            // This pack's own storage -> hidden matrix refresh; no real slots are overwritten.
            java.lang.reflect.Method sync = gui.inventorySlots.getClass()
                .getDeclaredMethod("syncCraftMatrixWithInventory", boolean.class);
            sync.setAccessible(true);
            sync.invoke(gui.inventorySlots, true);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not refresh the backpack crafting mirror", failure);
        }
    }
}
