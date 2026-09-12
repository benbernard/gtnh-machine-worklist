package com.benbernard.machineworklist;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;

import codechicken.nei.NEIClientConfig;
import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.recipe.AutoCraftingManager;
import codechicken.nei.recipe.RecipeHandlerRef;

/** Read-only checks shared by button previews and every submitted bulk transfer. */
final class CraftingAvailability {

    final List<String> reasons = new ArrayList<>();
    long batches;
    String limit;

    static CraftingAvailability inspect(GuiScreen parent, WorklistPlan.Step step) {
        CraftingAvailability result = new CraftingAvailability();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.isDead) {
            result.reasons.add("No active player. Reopen the worklist after entering the world.");
            return result;
        }
        if (step == null || !step.crafting) {
            result.reasons
                .add("Use the machine shown in NEI for this recipe; only crafting-grid recipes can be crafted here.");
            return result;
        }
        RecipeHandlerRef handler = RecipeHandlerRef.of(step.id);
        if (handler == null)
            result.reasons.add("Recipe unavailable. Choose its recipe again in NEI and reopen the group.");
        if (!NEIClientConfig.autocraftingEnabled())
            result.reasons.add("NEI autocrafting is off. Enable Autocrafting in NEI Options > Inventory.");
        if (AutoCraftingManager.processing())
            result.reasons.add("NEI is already crafting. Wait for it to finish before starting another request.");
        if (!(parent instanceof GuiContainer)) {
            result.reasons.add("Open a crafting table or backpack first, then press the worklist key.");
            return result;
        }
        GuiContainer gui = (GuiContainer) parent;
        if (mc.thePlayer.openContainer != gui.inventorySlots) {
            result.reasons.add("The original container has closed. Reopen it and press the worklist key again.");
            return result;
        }
        if (mc.thePlayer.inventory.getItemStack() != null)
            result.reasons.add("An item is on your cursor. Put it in an inventory slot first.");
        boolean outputSlot = false, occupied = false;
        for (Slot slot : gui.inventorySlots.inventorySlots) {
            if (slot instanceof SlotCrafting) outputSlot = true;
            if (CraftingInventory.occupiedCraftingSlot(gui, slot)) occupied = true;
        }
        if (!outputSlot) result.reasons
            .add("This container has no crafting grid. Open a crafting table, then press the worklist key.");
        if (occupied) result.reasons.add(
            "The crafting grid or result slot is occupied. Empty it before starting; stored backpack supplies may stay.");
        if (outputSlot && handler != null && !CraftingInventory.fits(handler, gui)) result.reasons.add(
            "This recipe does not fit this grid. Open a 3x3 crafting table or supported backpack, then press the worklist key.");
        ItemStack[] inventory = CraftingInventory.snapshot(parent);
        if (step.readyRuns == 0) {
            boolean named = false;
            java.util.Map<String, BookmarkItem> inputs = new java.util.LinkedHashMap<>();
            java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
            for (BookmarkItem input : step.inputs) {
                if (VirtualInputs.isCircuitSetting(input)) continue;
                String key = WorklistPlan.outputKey(input);
                inputs.putIfAbsent(key, input);
                counts.put(
                    key,
                    input.factor == 0 ? Math.max(1, counts.getOrDefault(key, 0L))
                        : Math.addExact(counts.getOrDefault(key, 0L), input.factor));
            }
            for (java.util.Map.Entry<String, BookmarkItem> entry : inputs.entrySet()) {
                BookmarkItem input = entry.getValue();
                long available = 0;
                for (ItemStack stack : inventory)
                    if (stack != null && WorklistPlan.matchesInput(input, BookmarkItem.of(0, stack)))
                        available += BookmarkItem.of(0, stack).amount;
                long needed = counts.get(entry.getKey());
                if (available < needed) {
                    result.reasons.add(
                        "Need " + (needed - available)
                            + " more "
                            + WorklistGui.itemName(input.itemStack)
                            + (input.factor == 0 ? " (reusable tool)." : " for one batch."));
                    named = true;
                }
            }
            if (!named) result.reasons.add(
                "Ingredients overlap or do not match the selected recipe. Check its inputs and required tool settings in NEI.");
        }
        long space = CraftingSpace.plan(gui, step, step.readyRuns).batches;
        if (step.readyRuns > 0 && space == 0) result.reasons.add(
            "Not enough usable space for crafting transfers, outputs and returned tools. "
                + "Free player slots or space in the open backpack/attached storage.");
        if (result.reasons.isEmpty() && !CraftingInventory.canCraft(handler, gui)) result.reasons.add(
            "NEI cannot transfer these inputs in this container. Check the exact recipe and use a supported crafting table or backpack.");
        result.batches = result.reasons.isEmpty() ? Math.min(step.readyRuns, space) : 0;
        result.limit = space < Math.min(step.readyRuns, CraftingBurst.MAX_BATCHES)
            ? "Limited by output/returned-tool space; free player slots or open storage."
            : step.readyRuns < step.runs ? "Limited by ingredients or reusable tools; see recipe inputs."
                : "All remaining batches of this recipe are ready.";
        return result;
    }

    static long outputCapacity(ItemStack[] inventory, List<BookmarkItem> outputs, long maximum) {
        return outputCapacity(inventory, outputs, maximum, 0);
    }

    static long outputCapacity(ItemStack[] inventory, List<BookmarkItem> outputs, long maximum, int returnedSlots) {
        long low = 0, high = maximum;
        while (low < high) {
            long middle = low + (high - low) / 2 + 1;
            if (outputsFit(inventory, outputs, middle, returnedSlots)) low = middle;
            else high = middle - 1;
        }
        return low;
    }

    private static boolean outputsFit(ItemStack[] inventory, List<BookmarkItem> outputs, long batches,
        int returnedSlots) {
        ItemStack[] slots = new ItemStack[inventory.length];
        for (int i = 0; i < inventory.length; i++) slots[i] = inventory[i] == null ? null : inventory[i].copy();
        for (BookmarkItem output : outputs) {
            if (output.factor <= 0 || batches > Long.MAX_VALUE / output.factor) return false;
            long remaining = batches * output.factor;
            ItemStack item = output.itemStack;
            for (ItemStack slot : slots) {
                if (slot == null || !slot.isItemEqual(item) || !ItemStack.areItemStackTagsEqual(slot, item)) continue;
                int add = (int) Math.min(remaining, Math.max(0, Math.min(64, slot.getMaxStackSize()) - slot.stackSize));
                slot.stackSize += add;
                remaining -= add;
            }
            for (int i = 0; i < slots.length && remaining > 0; i++) if (slots[i] == null) {
                slots[i] = item.copy();
                slots[i].stackSize = (int) Math.min(remaining, Math.min(64, item.getMaxStackSize()));
                remaining -= slots[i].stackSize;
            }
            if (remaining > 0) return false;
        }
        int empty = 0;
        for (ItemStack slot : slots) if (slot == null) empty++;
        return empty >= returnedSlots;
    }
}
