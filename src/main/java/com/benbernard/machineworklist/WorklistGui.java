package com.benbernard.machineworklist;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;

/** Shared keyboard focus and viewport-bounded item information. */
abstract class WorklistGui extends GuiScreen {

    protected int focusedButton = -1;
    private static final int CLOSE_WORKLIST = 10000;

    protected void addCloseButton() {
        buttonList.add(new GuiButton(CLOSE_WORKLIST, width - 128, 10, 56, 20, "Close"));
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        if (button == 0) for (Object value : buttonList) {
            GuiButton candidate = (GuiButton) value;
            if (candidate.id == CLOSE_WORKLIST && candidate.mousePressed(mc, x, y)) {
                closeWorklist();
                return;
            }
        }
        super.mouseClicked(x, y, button);
    }

    protected abstract GuiScreen parentScreen();

    protected void closeWorklist() {
        GuiScreen destination = parentScreen();
        while (destination instanceof WorklistGui) destination = ((WorklistGui) destination).parentScreen();
        returnTo(destination);
    }

    protected void returnTo(GuiScreen parent) {
        if (parent instanceof net.minecraft.client.gui.inventory.GuiContainer && (mc.thePlayer == null
            || mc.thePlayer.openContainer != ((net.minecraft.client.gui.inventory.GuiContainer) parent).inventorySlots))
            mc.displayGuiScreen(null);
        else mc.displayGuiScreen(parent);
    }

    protected boolean focusKey(int key) {
        if (ClientProxy.isOpenKey(key)) {
            if (!Keyboard.isRepeatEvent()) closeWorklist();
            return true;
        }
        if (key == Keyboard.KEY_TAB && !buttonList.isEmpty()) {
            int current = -1;
            for (int i = 0; i < buttonList.size(); i++)
                if (((GuiButton) buttonList.get(i)).id == focusedButton) current = i;
            int direction = isShiftKeyDown() ? -1 : 1;
            int next = current < 0 ? (direction > 0 ? 0 : buttonList.size() - 1)
                : (current + direction + buttonList.size()) % buttonList.size();
            focusedButton = ((GuiButton) buttonList.get(next)).id;
            return true;
        }
        if ((key == Keyboard.KEY_RETURN || key == Keyboard.KEY_SPACE) && focusedButton >= 0) {
            for (Object value : buttonList) {
                GuiButton button = (GuiButton) value;
                if (button.id == focusedButton) {
                    if (!Keyboard.isRepeatEvent()) {
                        if (button.id == CLOSE_WORKLIST) closeWorklist();
                        else actionPerformed(button);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void drawScreen(int x, int y, float ticks) {
        super.drawScreen(x, y, ticks);
        for (Object value : buttonList) {
            GuiButton button = (GuiButton) value;
            if (button.id != focusedButton) continue;
            int left = button.xPosition - 2, top = button.yPosition - 2;
            int right = left + button.width + 4, bottom = top + button.height + 4;
            drawRect(left, top, right, top + 1, 0xffffd578);
            drawRect(left, bottom - 1, right, bottom, 0xffffd578);
            drawRect(left, top, left + 1, bottom, 0xffffd578);
            drawRect(right - 1, top, right, bottom, 0xffffd578);
        }
    }

    static String itemName(ItemStack stack) {
        if (stack == null) return "Unknown item; reselect this recipe in NEI";
        String name = stack.getDisplayName();
        if (VirtualInputs.isProgrammedCircuit(stack)) name += " (configuration " + stack.getItemDamage() + ")";
        return name;
    }

    protected List<String> itemInformation(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        lines.add(itemName(stack));
        List<String> tooltip = stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);
        for (int i = 1; i < tooltip.size(); i++) lines.add(tooltip.get(i));
        return lines;
    }

    @Override
    protected void renderToolTip(ItemStack stack, int mouseX, int mouseY) {
        List<String> lines = new ArrayList<>();
        int maximum = Math.max(40, Math.min(300, width - 24));
        for (String text : itemInformation(stack))
            lines.addAll(fontRendererObj.listFormattedStringToWidth(text, maximum));
        int lineHeight = fontRendererObj.FONT_HEIGHT + 2;
        int limit = Math.max(1, (height - 24) / lineHeight);
        if (lines.size() > limit) {
            lines = new ArrayList<>(lines.subList(0, limit));
            lines.set(limit - 1, "More: select this row and press I");
        }
        int boxWidth = 0;
        for (String line : lines) boxWidth = Math.max(boxWidth, fontRendererObj.getStringWidth(line));
        int left = Math.max(8, Math.min(mouseX + 12, width - boxWidth - 8));
        int top = Math.max(8, Math.min(mouseY + 10, height - lines.size() * lineHeight - 8));
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        drawRect(left - 4, top - 4, left + boxWidth + 4, top + lines.size() * lineHeight + 3, 0xff090f18);
        for (int i = 0; i < lines.size(); i++)
            fontRendererObj.drawString(lines.get(i), left, top + i * lineHeight, 0xffffff);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        if (mc.thePlayer == null || mc.thePlayer.isDead || mc.thePlayer.getHealth() <= 0) mc.displayGuiScreen(null);
    }
}
