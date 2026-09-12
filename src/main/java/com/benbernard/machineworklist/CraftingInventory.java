package com.benbernard.machineworklist;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Optional integration with the exact pack version; no backpack dependency on other clients. */
final class CraftingInventory {

    static boolean fits(codechicken.nei.recipe.RecipeHandlerRef handler, GuiContainer gui) {
        if (!backpack(gui)) return handler.canFillCraftingGrid(gui);
        return handler.getOverlayHandler(gui) != null
            && new BackpackCraftingOverlay().canFillCraftingGrid(gui, handler.handler, handler.recipeIndex);
    }

    static boolean canCraft(codechicken.nei.recipe.RecipeHandlerRef handler, GuiContainer gui) {
        if (!backpack(gui)) return handler.canCraft(gui);
        return handler.getOverlayHandler(gui) != null
            && new BackpackCraftingOverlay().canCraft(gui, handler.handler, handler.recipeIndex);
    }

    static boolean craft(codechicken.nei.recipe.RecipeHandlerRef handler, GuiContainer gui, int batches) {
        if (batches < 1 || batches > CraftingBurst.MAX_BATCHES) return false;
        if (!backpack(gui)) return handler.craft(gui, batches);
        return handler.getOverlayHandler(gui) != null
            && new BackpackCraftingOverlay().craft(gui, handler.handler, handler.recipeIndex, batches);
    }

    static boolean backpack(GuiScreen screen) {
        if (!(screen instanceof GuiContainer)) return false;
        GuiContainer gui = (GuiContainer) screen;
        return gui.inventorySlots.getClass()
            .getName()
            .equals("com.darkona.adventurebackpack.inventory.ContainerBackpack")
            && gui.inventorySlots.inventorySlots.size() == 100
            && Minecraft.getMinecraft().thePlayer.openContainer == gui.inventorySlots;
    }

    static ItemStack[] snapshot(GuiScreen screen) {
        List<ItemStack> stock = new ArrayList<>();
        for (ItemStack item : Minecraft.getMinecraft().thePlayer.inventory.mainInventory)
            stock.add(item == null ? null : item.copy());
        if (backpack(screen)) {
            GuiContainer gui = (GuiContainer) screen;
            // Never include hidden mirror slots 90-98, result slot 99, or tool/fluid slots.
            for (int i = 36; i < 84; i++) {
                ItemStack item = gui.inventorySlots.getSlot(i)
                    .getStack();
                stock.add(item == null ? null : item.copy());
            }
        }
        return stock.toArray(new ItemStack[0]);
    }

    static boolean occupiedCraftingSlot(GuiContainer gui, Slot slot) {
        if (backpack(gui)) {
            return (BackpackLayout.craftingStorage(slot.slotNumber) || slot.slotNumber >= 90) && slot.getHasStack();
        }
        return !(slot.inventory instanceof net.minecraft.entity.player.InventoryPlayer) && slot.getHasStack();
    }

    private CraftingInventory() {}
}
