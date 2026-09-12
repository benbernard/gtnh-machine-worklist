package com.benbernard.machineworklist;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;

import codechicken.nei.guihook.IContainerDrawHandler;

/** Keep occupied slots in their real GUI: GuiContainer.onGuiClosed can clear them. */
final class EntryFeedback extends Gui implements IContainerDrawHandler {

    private static GuiContainer explained;

    static boolean allow(GuiContainer gui) {
        if (reasons(gui).isEmpty()) {
            explained = null;
            return true;
        }
        explained = gui;
        return false;
    }

    static List<String> reasons(GuiContainer gui) {
        List<String> reasons = new ArrayList<>();
        if (gui.mc.thePlayer.inventory.getItemStack() != null)
            reasons.add("Put the item on your cursor in a slot before opening the worklist.");
        boolean hasGrid = false;
        for (Slot slot : gui.inventorySlots.inventorySlots) if (slot instanceof SlotCrafting) hasGrid = true;
        if (hasGrid)
            for (Slot slot : gui.inventorySlots.inventorySlots) if (CraftingInventory.occupiedCraftingSlot(gui, slot)) {
                reasons.add(
                    (CraftingInventory.backpack(gui)
                        ? "Empty the open backpack's bottom-right 3x3 grid and result slot."
                        : "Empty this container's crafting grid and result slot.") + " Your items stay here.");
                break;
            }
        return reasons;
    }

    @Override
    public void postRenderObjects(GuiContainer gui, int mousex, int mousey) {
        if (gui != explained) return;
        List<String> messages = reasons(gui);
        if (messages.isEmpty()) {
            explained = null;
            return;
        }
        int maximum = Math.min(320, gui.width - 24);
        List<String> lines = new ArrayList<>();
        lines.add("Worklist unavailable:");
        for (String message : messages)
            lines.addAll(gui.mc.fontRenderer.listFormattedStringToWidth(message, maximum - 16));
        int left = (gui.width - maximum) / 2;
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        drawRect(left, 8, left + maximum, 24 + lines.size() * 10, 0xff101723);
        for (int i = 0; i < lines.size(); i++)
            gui.mc.fontRenderer.drawString(lines.get(i), left + 8, 15 + i * 10, 0xffd578);
    }

    @Override
    public void onPreDraw(GuiContainer gui) {}

    @Override
    public void renderObjects(GuiContainer gui, int x, int y) {}

    @Override
    public void renderSlotUnderlay(GuiContainer gui, Slot slot) {}

    @Override
    public void renderSlotOverlay(GuiContainer gui, Slot slot) {}
}
