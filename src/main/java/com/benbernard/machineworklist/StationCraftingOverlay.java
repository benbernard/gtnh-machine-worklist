package com.benbernard.machineworklist;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.DefaultOverlayHandler;
import codechicken.nei.recipe.IRecipeHandler;

/** Tinkers' station moves its grid when a chest is attached. Resolve it for every preview/transfer. */
final class StationCraftingOverlay extends DefaultOverlayHandler {

    private final Set<Slot> matrix = new HashSet<>();
    private final Set<Slot> storage;

    StationCraftingOverlay(Container container) {
        storage = new HashSet<>(ContainerStorage.slots(container, false));
        for (Slot slot : container.inventorySlots) if (slot.inventory instanceof InventoryCrafting) {
            matrix.add(slot);
            if (slot.getSlotIndex() == 0) {
                // NEI shaped/shapeless ingredient origin is (25, 6).
                offsetx = slot.xDisplayPosition - 25;
                offsety = slot.yDisplayPosition - 6;
            }
        }
    }

    @Override
    protected Set<Slot> getCraftMatrixSlots(GuiContainer gui, IRecipeHandler handler) {
        return matrix;
    }

    @Override
    public Slot[][] mapIngredSlots(GuiContainer gui, List<PositionedStack> ingredients) {
        Slot[][] mapped = new Slot[ingredients.size()][];
        for (int i = 0; i < ingredients.size(); i++) {
            PositionedStack ingredient = ingredients.get(i);
            mapped[i] = matrix.stream()
                .filter(
                    slot -> slot.xDisplayPosition == ingredient.relx + offsetx
                        && slot.yDisplayPosition == ingredient.rely + offsety)
                .toArray(Slot[]::new);
        }
        return mapped;
    }

    @Override
    public boolean canFillCraftingGrid(GuiContainer gui, IRecipeHandler handler, int recipeIndex) {
        if (matrix.size() != 9) return false;
        List<PositionedStack> ingredients = handler.getIngredientStacks(recipeIndex);
        if (ingredients.isEmpty()) return false;
        for (Slot[] slots : mapIngredSlots(gui, ingredients)) if (slots.length != 1) return false;
        return true;
    }

    @Override
    public boolean canMoveFrom(Slot slot, GuiContainer gui) {
        return (super.canMoveFrom(slot, gui) || storage.contains(slot)) && ContainerStorage.canTake(slot, gui);
    }
}
