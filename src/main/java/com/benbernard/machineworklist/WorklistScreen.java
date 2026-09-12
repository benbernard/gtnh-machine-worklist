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
public class WorklistScreen extends WorklistGui {

    private static final int TOP = 96;
    private static final int ROW = 60;
    private static final RenderItem ICONS = new RenderItem();
    private final GuiScreen parent;
    private final WorklistPlan plan;
    private List<WorklistPlan.Step> steps = new ArrayList<>();
    private List<WorklistPlan.Step> allSteps = new ArrayList<>();
    private final List<String> explanations = new ArrayList<>();
    private CraftingAvailability crafting;
    private String notice;
    private boolean initialTab = true;
    private WorklistPlan.Step selected;
    private ItemStack[] previousInventory;
    private int scroll;
    private int ticks;
    private int tab; // 0 machines, 1 crafting, 2 all
    private int missingScroll;
    private int keyboardRow = -1;
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
        }
        line(
            "" + plan.missingMaterials.size() + " inputs / scroll this pane",
            left + 8,
            height - 48,
            width - left - 28,
            0xa9b7cb);
    }

    private boolean canCraftSelected() {
        return error == null && crafting != null && crafting.batches > 0 && !CraftingSession.running();
    }

    @Override
    public void initGui() {
        if (splitPane()) showMissing = false;
        refresh();
        buttons();
    }

    private void buttons() {
        crafting = selected != null && selected.crafting ? CraftingAvailability.inspect(parent, selected) : null;
        buttonList.clear();
        buttonList.add(new GuiButton(6, width - 192, 10, 118, 20, "Available stock"));
        buttonList.add(new GuiButton(0, width - 68, 10, 56, 20, selected == null ? "Close" : "Back"));
        if (selected != null) {
            int buttonWidth = (width - 32) / 3;
            GuiButton recipe = new GuiButton(4, 12, 42, buttonWidth, 20, "NEI recipe [N]");
            recipe.enabled = selected.recipeAvailable;
            buttonList.add(recipe);
            if (selected.crafting) {
                GuiButton craft = new GuiButton(7, 16 + buttonWidth, 42, buttonWidth, 20, "Craft 1 batch [F]");
                craft.enabled = canCraftSelected();
                buttonList.add(craft);
                GuiButton all = new GuiButton(8, 20 + buttonWidth * 2, 42, buttonWidth, 20, "Craft all ready [G]");
                all.enabled = canCraftSelected();
                buttonList.add(all);
            }
        } else {
            int machines = (int) allSteps.stream()
                .filter(step -> !step.crafting)
                .count();
            int crafts = allSteps.size() - machines;
            buttonList.add(new GuiButton(10, 12, 42, 86, 20, (tab == 0 ? "> " : "") + "Machines " + machines));
            buttonList.add(new GuiButton(11, 102, 42, 86, 20, (tab == 1 ? "> " : "") + "Crafting " + crafts));
            buttonList.add(new GuiButton(12, 192, 42, 50, 20, (tab == 2 ? "> " : "") + "All " + allSteps.size()));
            buttonList.add(
                new GuiButton(2, 246, 42, Math.min(94, width - 258), 20, readyOnly ? "Ready only" : "All statuses"));
            if (!splitPane())
                buttonList.add(new GuiButton(3, 12, 68, 120, 20, showMissing ? "Work queue" : "Missing inputs"));
            buttonList.add(new GuiButton(9, width - 118, 68, 106, 20, "Choose group [B]"));
        }
        explainSelection();
    }

    private void explainSelection() {
        explanations.clear();
        if (notice != null) explanations.add(notice);
        if (selected == null) return;
        if (!selected.recipeAvailable) explanations
            .add("Recipe unavailable in NEI. Choose the recipe again in your bookmarks and reopen this group.");
        if (selected.crafting) {
            crafting = CraftingAvailability.inspect(parent, selected);
            if (crafting.reasons.isEmpty()) {
                StringBuilder preview = new StringBuilder("Craft all ready: ").append(crafting.batches)
                    .append(" batches");
                for (BookmarkItem output : selected.outputs) preview.append(" / ")
                    .append(crafting.batches * output.factor)
                    .append(" x ")
                    .append(itemName(output.itemStack));
                explanations.add(preview.toString());
                explanations.add(crafting.limit);
            } else explanations.addAll(crafting.reasons);
        }
    }

    void craftingFinished(String message) {
        notice = message;
        mc.displayGuiScreen(this);
        mc.displayGuiScreen(
            new InformationScreen(this, "Crafting result", java.util.Collections.singletonList(message)));
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
            allSteps = all;
            if (initialTab) {
                if (!all.isEmpty() && all.stream()
                    .allMatch(step -> step.crafting)) tab = 1;
                initialTab = false;
            }
            steps = new ArrayList<>(all);
            if (tab == 0) steps.removeIf(step -> step.crafting);
            if (tab == 1) steps.removeIf(step -> !step.crafting);
            if (readyOnly) steps.removeIf(step -> step.readyRuns == 0);
            previousInventory = copyInventory();
            error = null;
            crafting = selected != null && selected.crafting ? CraftingAvailability.inspect(parent, selected) : null;
            explainSelection();
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
            for (String note : explanations) result = Math.max(
                result,
                fontRendererObj.listFormattedStringToWidth(note, Math.max(1, listRight() - 40))
                    .size() * fontRendererObj.FONT_HEIGHT + 8);
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
        if (selected != null) return explanations.size() + selected.outputs.size()
            + selected.inputs.size()
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
        if (++ticks % 10 == 0 && mc.thePlayer != null) {
            if (inventoryChanged()) refresh();
            crafting = selected != null && selected.crafting ? CraftingAvailability.inspect(parent, selected) : null;
            buttons();
        }
    }

    private void line(String text, int x, int y, int maximumWidth, int color) {
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(text, Math.max(0, maximumWidth)), x, y, color);
    }

    private String quantity(BookmarkItem item, long count) {
        return count + (StackInfo.getFluid(item.itemStack) == null ? " x " : " mB ") + itemName(item.itemStack);
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
            ? (plan.groupId == -1 ? "Example: 8 crafting tables" : "NEI group " + plan.groupId)
                + (CraftingInventory.backpack(parent) ? " / Inventory + open backpack" : " / Inventory + hotbar")
                + (plan.progressWarning != null ? " / Check manual progress"
                    : plan.completed.isEmpty() ? "" : " + manual completion")
            : selected.runs + " runs remaining / " + selected.readyRuns + " ready with current inventory";
        line(subtitle, 14, 28, width - 28, 0xa9b7cb);
        if (error != null) fontRendererObj.drawSplitString(error, 14, TOP, width - 28, 0xff8989);
        else if (rowCount() == 0) line(
            showMissing ? "No missing external inputs or tools in this chain."
                : allSteps.isEmpty() ? "No work remains. C: review available stock records."
                    : readyOnly ? "Nothing ready here. R: all statuses; M: missing inputs."
                        : "No recipes in this tab. Press 3 to show all remaining work.",
            14,
            TOP,
            width - 28,
            0x67dbc4);
        else for (int index = scroll; index < Math.min(rowCount(), scroll + visibleRows()); index++) {
            int y = TOP + (index - scroll) * rowHeight();
            boolean hovered = index == keyboardRow
                || (mouseX >= 12 && mouseX < listRight() - 12 && mouseY >= y && mouseY < y + rowHeight() - 4);
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
            selected == null ? "Arrows + Enter: recipe; I: item; Tab: buttons; C: stock."
                : selected.crafting ? "F: one; G: all; B: blockers; I: item; Tab: buttons; Esc: back."
                    : "N: NEI; C: stock; Enter: upstream; I: item; Tab: buttons.",
            14,
            height - 18,
            width - 28,
            0xa9b7cb);
        if (splitPane()) drawMissingPane(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawItemTooltip(mouseX, mouseY);
        if (splitPane() && mouseX >= listRight() + 4
            && mouseX < width - 12
            && mouseY >= TOP
            && mouseY < TOP + missingRows() * 32) {
            int index = missingScroll + (mouseY - TOP) / 32;
            if (index < plan.missingMaterials.size())
                renderToolTip(plan.missingMaterials.get(index).itemStack, mouseX, mouseY);
        }
    }

    private void drawItemTooltip(int x, int y) {
        if (error != null || x < 12 || x >= listRight() - 12 || y < TOP || y >= TOP + visibleRows() * rowHeight())
            return;
        int index = scroll + (y - TOP) / rowHeight();
        if (index >= rowCount()) return;
        BookmarkItem item = null;
        if (selected != null) {
            index -= explanations.size();
            if (index >= 0 && index < selected.outputs.size()) item = selected.outputs.get(index);
            else if (index >= selected.outputs.size() && index < selected.outputs.size() + selected.inputs.size())
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
        if (index < explanations.size()) {
            fontRendererObj.drawSplitString(explanations.get(index), 18, y + 5, listRight() - 40, 0xe9bd72);
            return;
        }
        index -= explanations.size();
        if (index < selected.outputs.size()) drawMaterial(selected.outputs.get(index), "Produces: ", y, 0x67dbc4);
        else if (index < selected.outputs.size() + selected.inputs.size()) {
            BookmarkItem item = selected.inputs.get(index - selected.outputs.size());
            if (item.factor == 0) {
                icon(item.itemStack, 18, y + 5);
                line("Reusable: " + itemName(item.itemStack), 40, y + 9, listRight() - 60, 0xe9bd72);
            } else drawMaterial(item, "Input: ", y, 0xffffff);
        } else if (index < selected.outputs.size() + selected.inputs.size() + selected.dependencies.size()) {
            int dependency = index - selected.outputs.size() - selected.inputs.size();
            line(
                "Upstream: " + dependencyLabel(selected.dependencies.get(dependency)),
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
        if (button.id == 7 || button.id == 8) {
            refresh();
            if (canCraftSelected()) {
                CraftingSession.start(
                    this,
                    (net.minecraft.client.gui.inventory.GuiContainer) parent,
                    plan,
                    selected.id,
                    button.id == 7 ? 1 : crafting.batches);
            } else mc.displayGuiScreen(
                new InformationScreen(
                    this,
                    "Why crafting is unavailable",
                    crafting == null
                        ? java.util.Collections.singletonList(
                            error == null ? "No remaining crafting recipe is selected." : error)
                        : crafting.reasons));
            return;
        }
        if (button.id == 9) {
            mc.displayGuiScreen(new GroupScreen(parent));
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
            if (!selected.recipeAvailable) {
                mc.displayGuiScreen(
                    new InformationScreen(
                        this,
                        "Recipe unavailable",
                        java.util.Collections.singletonList(
                            "Choose this recipe again in NEI, then reopen the group. The saved recipe no longer resolves in the current pack.")));
                return;
            }
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
        if (button == 0 && selected != null && y >= TOP && x >= 12 && x < listRight() - 12) {
            keyboardRow = scroll + (y - TOP) / rowHeight();
            openDependency(keyboardRow);
            return;
        }
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
        if (focusKey(key)) return;
        if (key == org.lwjgl.input.Keyboard.KEY_B) {
            if (selected != null)
                mc.displayGuiScreen(new InformationScreen(this, "Recipe status", new ArrayList<>(explanations)));
            else mc.displayGuiScreen(new GroupScreen(parent));
            return;
        }
        if (key == org.lwjgl.input.Keyboard.KEY_I) {
            showItemInformation();
            return;
        }
        if (key >= org.lwjgl.input.Keyboard.KEY_1 && key <= org.lwjgl.input.Keyboard.KEY_3) {
            selected = null;
            keyboardRow = -1;
            actionPerformed(new GuiButton(10 + key - org.lwjgl.input.Keyboard.KEY_1, 0, 0, ""));
            return;
        }
        if (key == org.lwjgl.input.Keyboard.KEY_UP || key == org.lwjgl.input.Keyboard.KEY_DOWN) {
            focusedButton = -1;
            int direction = key == org.lwjgl.input.Keyboard.KEY_DOWN ? 1 : -1;
            if (rowCount() > 0) {
                keyboardRow = Math.max(0, Math.min(rowCount() - 1, keyboardRow < 0 ? scroll : keyboardRow + direction));
                if (keyboardRow < scroll) scroll = keyboardRow;
                if (keyboardRow >= scroll + visibleRows()) scroll = keyboardRow - visibleRows() + 1;
            } else {
                scroll += direction;
                clampScroll();
            }
            return;
        }
        if (key == org.lwjgl.input.Keyboard.KEY_RETURN && selected != null) {
            openDependency(keyboardRow < 0 ? scroll : keyboardRow);
            return;
        }
        if (key == org.lwjgl.input.Keyboard.KEY_RETURN && selected == null && !showMissing && !steps.isEmpty()) {
            selected = steps.get(Math.max(0, Math.min(steps.size() - 1, keyboardRow < 0 ? scroll : keyboardRow)));
            scroll = 0;
            buttons();
            return;
        }
        if ((key == org.lwjgl.input.Keyboard.KEY_F || key == org.lwjgl.input.Keyboard.KEY_G) && selected != null
            && !org.lwjgl.input.Keyboard.isRepeatEvent()) {
            actionPerformed(new GuiButton(key == org.lwjgl.input.Keyboard.KEY_F ? 7 : 8, 0, 0, ""));
            return;
        }
        if (key == org.lwjgl.input.Keyboard.KEY_N && selected != null) {
            actionPerformed(new GuiButton(4, 0, 0, ""));
            return;
        }
        if (key == org.lwjgl.input.Keyboard.KEY_R && selected == null) {
            actionPerformed(new GuiButton(2, 0, 0, ""));
            return;
        }
        if (key == org.lwjgl.input.Keyboard.KEY_M && selected == null) {
            if (splitPane()) {
                List<String> missing = new ArrayList<>();
                for (BookmarkItem item : plan.missingMaterials) missing.add(quantity(item, item.amount));
                if (missing.isEmpty()) missing.add("No missing external inputs or tools.");
                mc.displayGuiScreen(new InformationScreen(this, "Overall missing inputs", missing));
            } else actionPerformed(new GuiButton(3, 0, 0, ""));
            return;
        }
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

    private WorklistPlan.Step dependency(codechicken.nei.recipe.Recipe.RecipeId id) {
        for (WorklistPlan.Step step : allSteps) if (step.id.equals(id)) return step;
        return null;
    }

    private String dependencyLabel(codechicken.nei.recipe.Recipe.RecipeId id) {
        WorklistPlan.Step step = dependency(id);
        return step == null ? itemName(id.getResult()) : step.machine + " / " + itemName(step.outputs.get(0).itemStack);
    }

    private void openDependency(int row) {
        if (selected == null) return;
        int index = row - explanations.size() - selected.outputs.size() - selected.inputs.size();
        if (index < 0 || index >= selected.dependencies.size()) return;
        WorklistPlan.Step next = dependency(selected.dependencies.get(index));
        if (next != null) {
            selected = next;
            scroll = 0;
            keyboardRow = -1;
            refresh();
            buttons();
        }
    }

    private void showItemInformation() {
        int index = keyboardRow < 0 ? scroll : keyboardRow;
        BookmarkItem item = null;
        if (selected != null) {
            index -= explanations.size();
            if (index >= 0 && index < selected.outputs.size()) item = selected.outputs.get(index);
            else if (index >= selected.outputs.size() && index < selected.outputs.size() + selected.inputs.size())
                item = selected.inputs.get(index - selected.outputs.size());
        } else if (showMissing && index < plan.missingMaterials.size()) item = plan.missingMaterials.get(index);
        else if (!showMissing && index < steps.size()) item = steps.get(index).outputs.get(0);
        if (item != null)
            mc.displayGuiScreen(new InformationScreen(this, "Item information", itemInformation(item.itemStack)));
        else if (selected != null)
            mc.displayGuiScreen(new InformationScreen(this, "Recipe status", new ArrayList<>(explanations)));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
