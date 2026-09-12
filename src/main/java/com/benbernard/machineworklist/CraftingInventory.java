package com.benbernard.machineworklist;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;

/** Optional integration with the exact pack version; no backpack dependency on other clients. */
final class CraftingInventory {

    static boolean fits(codechicken.nei.recipe.RecipeHandlerRef handler, GuiContainer gui) {
        if (station(gui))
            return handler.getOverlayHandler(gui) != null && new StationCraftingOverlay(gui.inventorySlots)
                .canFillCraftingGrid(gui, handler.handler, handler.recipeIndex);
        if (!backpack(gui)) return handler.canFillCraftingGrid(gui);
        return handler.getOverlayHandler(gui) != null
            && new BackpackCraftingOverlay().canFillCraftingGrid(gui, handler.handler, handler.recipeIndex);
    }

    static boolean canCraft(codechicken.nei.recipe.RecipeHandlerRef handler, GuiContainer gui) {
        if (station(gui)) return handler.getOverlayHandler(gui) != null
            && new StationCraftingOverlay(gui.inventorySlots).canCraft(gui, handler.handler, handler.recipeIndex);
        if (!backpack(gui)) return handler.canCraft(gui);
        return handler.getOverlayHandler(gui) != null
            && new BackpackCraftingOverlay().canCraft(gui, handler.handler, handler.recipeIndex);
    }

    static boolean craft(codechicken.nei.recipe.RecipeHandlerRef handler, GuiContainer gui, int batches) {
        if (batches < 1 || batches > CraftingBurst.MAX_BATCHES) return false;
        if (station(gui)) return handler.getOverlayHandler(gui) != null
            && new StationCraftingOverlay(gui.inventorySlots).craft(gui, handler.handler, handler.recipeIndex, batches);
        if (!backpack(gui)) return handler.craft(gui, batches);
        return handler.getOverlayHandler(gui) != null
            && new BackpackCraftingOverlay().craft(gui, handler.handler, handler.recipeIndex, batches);
    }

    static boolean backpack(GuiScreen screen) {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.thePlayer != null && backpack(screen, mc.thePlayer.openContainer);
    }

    static boolean backpack(GuiScreen screen, Container active) {
        GuiContainer gui = activeGui(screen, active);
        return gui != null && gui.inventorySlots.getClass()
            .getName()
            .equals("com.darkona.adventurebackpack.inventory.ContainerBackpack")
            && gui.inventorySlots.inventorySlots.size() == 100;
    }

    /** Only unwrap an NEI recipe view while its original container is still active. */
    static GuiContainer activeGui(GuiScreen screen, Container active) {
        if (screen instanceof codechicken.nei.recipe.GuiRecipe)
            screen = ((codechicken.nei.recipe.GuiRecipe<?>) screen).firstGui;
        if (!(screen instanceof GuiContainer)) return null;
        GuiContainer gui = (GuiContainer) screen;
        return active != null && gui.inventorySlots == active ? gui : null;
    }

    static boolean station(GuiContainer gui) {
        return gui.inventorySlots.getClass()
            .getName()
            .equals("tconstruct.tools.inventory.CraftingStationContainer");
    }

    static String context(GuiScreen screen) {
        Minecraft mc = Minecraft.getMinecraft();
        GuiContainer gui = mc.thePlayer == null ? null : activeGui(screen, mc.thePlayer.openContainer);
        if (gui == null) return "Grid closed; reopen your table";
        if (backpack(gui)) return "Backpack 3x3 / Inventory + open backpack";
        if (station(gui)) return "Tinkers' station 3x3 / Inventory + hotbar";
        if (gui instanceof net.minecraft.client.gui.inventory.GuiInventory) return "Player 2x2 / Inventory + hotbar";
        if (gui instanceof net.minecraft.client.gui.inventory.GuiCrafting) return "Table 3x3 / Inventory + hotbar";
        return "Current container / Inventory + hotbar";
    }

    static ItemStack[] snapshot(GuiScreen screen) {
        List<ItemStack> stock = new ArrayList<>();
        for (ItemStack item : Minecraft.getMinecraft().thePlayer.inventory.mainInventory)
            stock.add(item == null ? null : item.copy());
        if (backpack(screen)) {
            GuiContainer gui = activeGui(screen, Minecraft.getMinecraft().thePlayer.openContainer);
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
        return occupiedCraftingSlot(gui, slot, Minecraft.getMinecraft().thePlayer.openContainer);
    }

    static boolean occupiedCraftingSlot(GuiContainer gui, Slot slot, Container active) {
        if (backpack(gui, active)) {
            return (BackpackLayout.craftingStorage(slot.slotNumber) || slot.slotNumber >= 90) && slot.getHasStack();
        }
        return occupiedGridSlot(slot);
    }

    static boolean occupiedGridSlot(Slot slot) {
        return (slot instanceof SlotCrafting || slot.inventory instanceof InventoryCrafting) && slot.getHasStack();
    }

    static ItemStack[] containerSnapshot(GuiContainer gui) {
        List<ItemStack> result = new ArrayList<>();
        for (Slot slot : gui.inventorySlots.inventorySlots) {
            ItemStack stack = slot.getStack();
            result.add(stack == null ? null : stack.copy());
        }
        ItemStack cursor = gui.mc.thePlayer.inventory.getItemStack();
        result.add(cursor == null ? null : cursor.copy());
        return result.toArray(new ItemStack[0]);
    }

    static boolean sameSnapshot(ItemStack[] before, ItemStack[] after) {
        if (before == null || before.length != after.length) return false;
        for (int i = 0; i < before.length; i++) if (!ItemStack.areItemStacksEqual(before[i], after[i])) return false;
        return true;
    }

    private CraftingInventory() {}
}
