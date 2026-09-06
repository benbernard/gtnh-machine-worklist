package com.benbernard.machineworklist;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.StackInfo;

/** Machine queue with a separate scrollable recipe detail view. */
public class WorklistScreen extends GuiScreen {

    private static final int TOP = 96;
    private static final int ROW = 60;
    private static final RenderItem ICONS = new RenderItem();
    private final GuiScreen parent;
    private final WorklistPlan plan;
    private List<WorklistPlan.Step> steps = new ArrayList<>();
    private WorklistPlan.Step selected;
    private ItemStack[] previousInventory;
    private int scroll;
    private int ticks;
    private int tab; // 0 machines, 1 crafting, 2 all
    private int missingScroll;
    private boolean readyOnly;
    private boolean showMissing;
    private String error;

    public WorklistScreen(GuiScreen parent, WorklistPlan plan) {
        this.parent = parent;
        this.plan = plan;
    }

    private boolean splitPane() {
        return width >= 620;
    }

    private int listRight() {
        return splitPane() ? width - Math.max(210, width / 3) : width;
    }

    private int missingRows() {
        return Math.max(1, (height - TOP - 48) / 32);
    }

    private void drawMissingPane(int mouseX, int mouseY) {
        int left = listRight() + 4;
        drawRect(left, TOP - 22, width - 12, height - 34, 0xff172432);
        line("Overall missing inputs", left + 8, TOP - 16, width - left - 28, 0x67dbc4);
        missingScroll = Math.max(0, Math.min(missingScroll, Math.max(0, plan.missingMaterials.size() - missingRows())));
        if (plan.missingMaterials.isEmpty())
            line("No missing external inputs/tools", left + 8, TOP + 6, width - left - 28, 0xa9b7cb);
        for (int i = missingScroll; i < Math.min(plan.missingMaterials.size(), missingScroll + missingRows()); i++) {
            BookmarkItem item = plan.missingMaterials.get(i);
            int y = TOP + (i - missingScroll) * 32;
            icon(item.itemStack, left + 8, y + 4);
            line(
                (item.factor == 0 ? "Reusable: " : "") + quantity(item, item.amount),
                left + 30,
                y + 8,
                width - left - 48,
                0xe9bd72);
            if (mouseX >= left && mouseX < width - 12 && mouseY >= y && mouseY < y + 32)
                renderToolTip(item.itemStack, mouseX, mouseY);
        }
        line(
            "" + plan.missingMaterials.size() + " inputs / scroll this pane",
            left + 8,
            height - 48,
            width - left - 28,
            0xa9b7cb);
    }

    private boolean canCraftSelected() {
        if (error != null || selected == null
            || !selected.crafting
            || selected.readyRuns < 1
            || !codechicken.nei.NEIClientConfig.autocraftingEnabled()
            || codechicken.nei.recipe.AutoCraftingManager.processing()
            || !(parent instanceof net.minecraft.client.gui.inventory.GuiContainer)) return false;
        net.minecraft.client.gui.inventory.GuiContainer gui = (net.minecraft.client.gui.inventory.GuiContainer) parent;
        if (mc.thePlayer.openContainer != gui.inventorySlots || mc.thePlayer.inventory.getItemStack() != null
            || mc.thePlayer.inventory.getFirstEmptyStack() < 0) return false;
        // Start with an empty real grid, so NEI never has to clear unrelated ingredients.
        boolean resultSlot = false;
        for (net.minecraft.inventory.Slot slot : gui.inventorySlots.inventorySlots) {
            if (slot instanceof net.minecraft.inventory.SlotCrafting) resultSlot = true;
            if (CraftingInventory.occupiedCraftingSlot(gui, slot)) return false;
        }
        codechicken.nei.recipe.RecipeHandlerRef handler = codechicken.nei.recipe.RecipeHandlerRef.of(selected.id);
        return resultSlot && handler != null && handler.canCraft(gui);
    }

    @Override
    public void initGui() {
        if (splitPane()) showMissing = false;
        refresh();
        buttons();
    }

    private void buttons() {
        buttonList.clear();
        buttonList.add(new GuiButton(6, width - 174, 10, 100, 20, "Record completed"));
        buttonList.add(new GuiButton(0, width - 68, 10, 56, 20, selected == null ? "Close" : "Back"));
        if (selected != null) {
            GuiButton recipe = new GuiButton(4, 12, 42, 120, 20, "Open NEI recipe");
            recipe.enabled = selected.recipeAvailable;
            buttonList.add(recipe);
            if (selected.crafting) {
                GuiButton craft = new GuiButton(7, 138, 42, 150, 20, "Craft 1 batch via NEI");
                craft.enabled = canCraftSelected();
                buttonList.add(craft);
            }
        } else {
            buttonList.add(new GuiButton(10, 12, 42, 70, 20, tab == 0 ? "[Machines]" : "Machines"));
            buttonList.add(new GuiButton(11, 86, 42, 70, 20, tab == 1 ? "[Crafting]" : "Crafting"));
            buttonList.add(new GuiButton(12, 160, 42, 44, 20, tab == 2 ? "[All]" : "All"));
            buttonList.add(new GuiButton(2, 208, 42, 94, 20, readyOnly ? "Ready only" : "All statuses"));
            if (!splitPane())
                buttonList.add(new GuiButton(3, 12, 68, 120, 20, showMissing ? "Work queue" : "Missing inputs"));
        }
    }

    private void refresh() {
        try {
            if (mc.thePlayer == null) return;
            List<WorklistPlan.Step> all = plan.calculate(availableInventory());
            if (selected != null) {
                WorklistPlan.Step replacement = null;
                for (WorklistPlan.Step step : all) if (step.id.equals(selected.id)) replacement = step;
                selected = replacement;
            }
            steps = all;
            if (tab == 0) steps.removeIf(step -> step.crafting);
            if (tab == 1) steps.removeIf(step -> !step.crafting);
            if (readyOnly) steps.removeIf(step -> step.readyRuns == 0);
            previousInventory = copyInventory();
            error = null;
            clampScroll();
        } catch (RuntimeException exception) {
            error = "Could not calculate this group: " + exception.getClass()
                .getSimpleName() + ": " + exception.getMessage();
        }
    }

    ItemStack[] availableInventory() {
        return CraftingInventory.snapshot(parent);
    }

    private ItemStack[] copyInventory() {
        return availableInventory();
    }

    private boolean inventoryChanged() {
        ItemStack[] items = availableInventory();
        if (previousInventory == null || items.length != previousInventory.length) return true;
        for (int i = 0; i < items.length; i++)
            if (!ItemStack.areItemStacksEqual(items[i], previousInventory[i])) return true;
        return false;
    }

    private int rowHeight() {
        if (selected != null) {
            int result = 32;
            for (String note : selected.notes) result = Math.max(
                result,
                fontRendererObj.listFormattedStringToWidth(note, Math.max(1, listRight() - 40))
                    .size() * fontRendererObj.FONT_HEIGHT + 8);
            return result;
        }
        return selected == null && !showMissing ? ROW : 32;
    }

    private int visibleRows() {
        return Math.max(1, (height - TOP - 30) / rowHeight());
    }

    private int rowCount() {
        if (selected != null) return selected.outputs.size() + selected.inputs.size()
            + selected.dependencies.size()
            + selected.notes.size();
        return showMissing ? plan.missingMaterials.size() : steps.size();
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rowCount() - visibleRows())));
    }

    @Override
    public void updateScreen() {
        if (mc.thePlayer == null || mc.thePlayer.isDead || mc.thePlayer.getHealth() <= 0) {
            // Let Minecraft select its death/disconnection screen rather than retaining
            // a worklist (or restoring an inventory screen) for a dead player.
            mc.displayGuiScreen(null);
            return;
        }
        if (++ticks % 10 == 0 && mc.thePlayer != null && inventoryChanged()) {
            refresh();
            buttons();
        }
    }

    private void line(String text, int x, int y, int maximumWidth, int color) {
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(text, Math.max(0, maximumWidth)), x, y, color);
    }

    private String quantity(BookmarkItem item, long count) {
        return count + (StackInfo.getFluid(item.itemStack) == null ? " x " : " mB ") + item.itemStack.getDisplayName();
    }

    private void icon(ItemStack source, int x, int y) {
        ItemStack stack = source.copy();
        stack.stackSize = 1;
        GL11.glPushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        ICONS.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Empty lists must establish the same render state as lists containing item icons.
        // Otherwise depth left by the previous GUI can hide updated controls and text.
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        RenderHelper.disableStandardItemLighting();
        drawRect(0, 0, width, height, 0xf5101723);
        line(selected == null ? "MACHINE WORKLIST" : selected.machine, 14, 14, width - 194, 0x67dbc4);
        String subtitle = selected == null
            ? "NEI group " + plan.groupId
                + (CraftingInventory.backpack(parent) ? " / Inventory + open backpack" : " / Inventory + hotbar")
                + (plan.progressWarning != null ? " / Check manual progress"
                    : plan.completed.isEmpty() ? "" : " + manual completion")
            : selected.runs + " runs remaining / " + selected.readyRuns + " ready with current inventory";
        line(subtitle, 14, 28, width - 28, 0xa9b7cb);
        if (error != null) fontRendererObj.drawSplitString(error, 14, TOP, width - 28, 0xff8989);
        else if (rowCount() == 0) line(
            showMissing ? "No missing external inputs or tools in this chain." : "No remaining steps match this view.",
            14,
            TOP,
            width - 28,
            0x67dbc4);
        else for (int index = scroll; index < Math.min(rowCount(), scroll + visibleRows()); index++) {
            int y = TOP + (index - scroll) * rowHeight();
            boolean hovered = mouseX >= 12 && mouseX < listRight() - 12 && mouseY >= y && mouseY < y + rowHeight() - 4;
            drawRect(12, y, listRight() - 12, y + rowHeight() - 4, hovered ? 0xff293b52 : 0xff202d40);
            if (selected != null) drawDetail(index, y);
            else if (showMissing) {
                BookmarkItem item = plan.missingMaterials.get(index);
                drawMaterial(item, item.factor == 0 ? "Reusable missing: " : "Missing: ", y, 0xe9bd72);
            } else drawStep(steps.get(index), y);
        }
        if (error == null && rowCount() > visibleRows()) {
            int track = visibleRows() * rowHeight() - 4;
            int thumb = Math.max(8, track * visibleRows() / rowCount());
            int offset = (track - thumb) * scroll / (rowCount() - visibleRows());
            drawRect(listRight() - 8, TOP, listRight() - 4, TOP + track, 0xff202d40);
            drawRect(listRight() - 8, TOP + offset, listRight() - 4, TOP + offset + thumb, 0xff67dbc4);
            line(
                "Rows " + (scroll + 1)
                    + "-"
                    + Math.min(rowCount(), scroll + visibleRows())
                    + " of "
                    + rowCount()
                    + " / scroll for more",
                14,
                height - 30,
                width - 28,
                0xa9b7cb);
        }
        line(
            selected == null ? "Click a step for inputs. Ready counts apply to each step separately."
                : selected.crafting
                    ? "NEI craft needs an open compatible empty grid, empty cursor and a free inventory slot."
                    : "Inputs show the full remaining batch. Molds/circuits are reusable.",
            14,
            height - 18,
            width - 28,
            0xa9b7cb);
        if (splitPane()) drawMissingPane(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawItemTooltip(mouseX, mouseY);
    }

    private void drawItemTooltip(int x, int y) {
        if (error != null || x < 12 || x >= listRight() - 12 || y < TOP || y >= TOP + visibleRows() * rowHeight())
            return;
        int index = scroll + (y - TOP) / rowHeight();
        if (index >= rowCount()) return;
        BookmarkItem item = null;
        if (selected != null) {
            if (index < selected.outputs.size()) item = selected.outputs.get(index);
            else if (index < selected.outputs.size() + selected.inputs.size())
                item = selected.inputs.get(index - selected.outputs.size());
        } else if (showMissing) item = plan.missingMaterials.get(index);
        else if (x < 38) item = steps.get(index).outputs.get(0);
        if (item != null) renderToolTip(item.itemStack, x, y);
    }

    private void drawStep(WorklistPlan.Step step, int y) {
        icon(step.outputs.get(0).itemStack, 18, y + 6);
        line(step.machine + " / " + step.runs + " runs", 40, y + 6, listRight() - 60, 0xffffff);
        StringBuilder output = new StringBuilder();
        for (BookmarkItem item : step.outputs) {
            if (output.length() > 0) output.append(", ");
            output.append(quantity(item, item.amount));
        }
        line(output.toString(), 40, y + 20, listRight() - 60, 0x67dbc4);
        String status = !step.recipeAvailable ? "Recipe unavailable in NEI"
            : step.readyRuns > 0 ? "READY: " + step.readyRuns + " runs"
                : "WAITING: " + step.dependencies.size() + " upstream steps / check inputs";
        if (!step.notes.isEmpty()) status = "CHECK RECIPE NOTES / " + status;
        line(status, 18, y + 39, listRight() - 40, step.readyRuns > 0 ? 0x67dbc4 : 0xe9bd72);
    }

    private void drawMaterial(BookmarkItem item, String prefix, int y, int color) {
        icon(item.itemStack, 18, y + 5);
        line(prefix + quantity(item, item.amount), 40, y + 9, listRight() - 60, color);
    }

    private void drawDetail(int index, int y) {
        if (index < selected.outputs.size()) drawMaterial(selected.outputs.get(index), "Produces: ", y, 0x67dbc4);
        else if (index < selected.outputs.size() + selected.inputs.size()) {
            BookmarkItem item = selected.inputs.get(index - selected.outputs.size());
            if (item.factor == 0) {
                icon(item.itemStack, 18, y + 5);
                line("Reusable: " + item.itemStack.getDisplayName(), 40, y + 9, listRight() - 60, 0xe9bd72);
            } else drawMaterial(item, "Input: ", y, 0xffffff);
        } else if (index < selected.outputs.size() + selected.inputs.size() + selected.dependencies.size()) {
            int dependency = index - selected.outputs.size() - selected.inputs.size();
            line(
                "Upstream: " + selected.dependencies.get(dependency)
                    .getHandleName(),
                18,
                y + 9,
                listRight() - 40,
                0xa9b7cb);
        } else {
            int note = index - selected.outputs.size() - selected.inputs.size() - selected.dependencies.size();
            fontRendererObj.drawSplitString(selected.notes.get(note), 18, y + 3, listRight() - 40, 0xe9bd72);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 7) {
            refresh();
            if (canCraftSelected()) {
                codechicken.nei.recipe.RecipeHandlerRef handler = codechicken.nei.recipe.RecipeHandlerRef
                    .of(selected.id);
                mc.displayGuiScreen(parent);
                if (!handler.craft((net.minecraft.client.gui.inventory.GuiContainer) parent, 1))
                    mc.thePlayer.addChatMessage(
                        new net.minecraft.util.ChatComponentText(
                            "NEI could not craft this batch. Check the grid and inventory."));
            }
            return;
        }
        if (button.id >= 10 && button.id <= 12) {
            tab = button.id - 10;
            showMissing = false;
        }
        if (button.id == 6) {
            mc.displayGuiScreen(new ProgressScreen(this, plan, selected == null ? null : selected.outputs.get(0)));
            return;
        }
        if (button.id == 0) {
            if (selected == null) {
                mc.displayGuiScreen(parent);
                return;
            }
            selected = null;
        }
        if (button.id == 2) {
            readyOnly = !readyOnly;
            showMissing = false;
        }
        if (button.id == 3) showMissing = !showMissing;
        if (button.id == 4 && selected != null) {
            GuiCraftingRecipe.openRecipeGui("recipeId", selected.id.getResult(), selected.id);
            return;
        }
        scroll = 0;
        refresh();
        buttons();
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        if (button != 0 || selected != null
            || showMissing
            || x < 12
            || x >= listRight() - 12
            || y < TOP
            || y >= TOP + visibleRows() * rowHeight()) return;
        int index = scroll + (y - TOP) / rowHeight();
        if (index < steps.size()) {
            selected = steps.get(index);
            scroll = 0;
            buttons();
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta != 0 && splitPane() && Mouse.getEventX() * width / mc.displayWidth >= listRight()) {
            missingScroll = Math.max(
                0,
                Math.min(
                    missingScroll + (delta < 0 ? 1 : -1),
                    Math.max(0, plan.missingMaterials.size() - missingRows())));
            return;
        }
        if (delta != 0) {
            scroll += delta < 0 ? 1 : -1;
            clampScroll();
        }
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == org.lwjgl.input.Keyboard.KEY_C) {
            mc.displayGuiScreen(new ProgressScreen(this, plan, selected == null ? null : selected.outputs.get(0)));
            return;
        }
        if (key == 1) {
            if (selected == null) mc.displayGuiScreen(parent);
            else {
                selected = null;
                scroll = 0;
                buttons();
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
