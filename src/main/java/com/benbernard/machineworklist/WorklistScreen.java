package com.benbernard.machineworklist;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Mouse;

import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.recipe.GuiCraftingRecipe;

/** Initial functional screen; layout and readiness are refined against in-game tests. */
public class WorklistScreen extends GuiScreen {

    private final GuiScreen parent;
    private final WorklistPlan plan;
    private List<WorklistPlan.Step> steps = new ArrayList<>();
    private int scroll;
    private int ticks;
    private boolean showCrafting;
    private String error;

    public WorklistScreen(GuiScreen parent, WorklistPlan plan) {
        this.parent = parent;
        this.plan = plan;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width - 76, 12, 64, 20, "Close"));
        buttonList.add(new GuiButton(1, 12, 38, 140, 20, showCrafting ? "All steps" : "Machine steps"));
        refresh();
    }

    private void refresh() {
        try {
            steps = plan.calculate(mc.thePlayer.inventory.mainInventory);
            if (!showCrafting) steps.removeIf(step -> step.crafting);
            error = null;
            scroll = Math.max(0, Math.min(scroll, Math.max(0, steps.size() - visibleRows())));
        } catch (RuntimeException exception) {
            error = exception.getClass()
                .getSimpleName() + ": "
                + exception.getMessage();
        }
    }

    private int visibleRows() {
        return Math.max(1, (height - 100) / 54);
    }

    @Override
    public void updateScreen() {
        if (++ticks % 20 == 0) refresh();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xf0101723);
        fontRendererObj.drawString("MACHINE WORKLIST", 14, 15, 0x67dbc4);
        fontRendererObj.drawString("NEI group " + plan.groupId + "  /  Inventory + hotbar", 14, 28, 0xa9b7cb);
        if (error != null) fontRendererObj.drawSplitString(error, 14, 72, width - 28, 0xff8989);
        else if (steps.isEmpty()) fontRendererObj.drawString("No remaining steps in this view.", 14, 76, 0x67dbc4);
        else for (int index = scroll; index < Math.min(steps.size(), scroll + visibleRows()); index++) {
            WorklistPlan.Step step = steps.get(index);
            int y = 68 + (index - scroll) * 54;
            drawRect(12, y, width - 12, y + 50, 0xff202d40);
            fontRendererObj.drawString(step.machine + "  |  " + step.runs + " runs", 20, y + 5, 0xffffff);
            StringBuilder output = new StringBuilder();
            for (BookmarkItem item : step.outputs) {
                if (output.length() > 0) output.append(", ");
                output.append(item.amount)
                    .append(" ")
                    .append(item.itemStack.getDisplayName());
            }
            fontRendererObj
                .drawString(fontRendererObj.trimStringToWidth(output.toString(), width - 40), 20, y + 19, 0x67dbc4);
            fontRendererObj.drawString(
                step.readyRuns > 0 ? "Ready: " + step.readyRuns + " runs  |  Click for recipe"
                    : "Waiting for ingredients  |  Click for recipe",
                20,
                y + 34,
                step.readyRuns > 0 ? 0x67dbc4 : 0xe9bd72);
        }
        fontRendererObj.drawString("Scroll to browse. Inventory refreshes every second.", 14, height - 18, 0xa9b7cb);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) mc.displayGuiScreen(parent);
        if (button.id == 1) {
            showCrafting = !showCrafting;
            scroll = 0;
            initGui();
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        if (button != 0 || x < 12 || x >= width - 12 || y < 68 || y >= 68 + visibleRows() * 54) return;
        int index = scroll + (y - 68) / 54;
        if (index < steps.size()) {
            WorklistPlan.Step step = steps.get(index);
            GuiCraftingRecipe.openRecipeGui("recipeId", step.id.getResult(), step.id);
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta != 0)
            scroll = Math.max(0, Math.min(Math.max(0, steps.size() - visibleRows()), scroll + (delta < 0 ? 1 : -1)));
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == 1) mc.displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
