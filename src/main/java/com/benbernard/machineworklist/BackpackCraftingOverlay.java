package com.benbernard.machineworklist;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;

import codechicken.nei.recipe.DefaultOverlayHandler;
import codechicken.nei.recipe.IRecipeHandler;

/** NEI's rectangular grid discovery otherwise includes all adjacent backpack storage. */
final class BackpackCraftingOverlay extends DefaultOverlayHandler {

    BackpackCraftingOverlay() {
        super(127, 55);
    }

    static Set<Slot> craftingSlots(List<Slot> slots) {
        Set<Slot> result = new HashSet<>();
        for (Slot slot : slots) if (BackpackLayout.craftingStorage(slot.slotNumber)) result.add(slot);
        return result;
    }

    @Override
    protected Set<Slot> getCraftMatrixSlots(GuiContainer gui, IRecipeHandler handler) {
        return craftingSlots(gui.inventorySlots.inventorySlots);
    }

    @Override
    public boolean canMoveFrom(Slot slot, GuiContainer gui) {
        return slot.slotNumber >= 0 && slot.slotNumber < 36 || BackpackLayout.storage(slot.slotNumber);
    }

    @Override
    public boolean canFillCraftingGrid(GuiContainer gui, IRecipeHandler handler, int recipeIndex) {
        for (Slot[] slots : mapIngredSlots(gui, handler.getIngredientStacks(recipeIndex))) {
            if (slots.length == 0) return false;
            for (Slot slot : slots) if (!BackpackLayout.craftingStorage(slot.slotNumber)) return false;
        }
        return true;
    }
}
