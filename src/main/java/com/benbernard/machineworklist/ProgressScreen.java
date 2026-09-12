package com.benbernard.machineworklist;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;

import org.lwjgl.input.Mouse;

import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.recipe.StackInfo;
import codechicken.nei.recipe.chain.RecipeChainMath;

/** Editable manual output stock, including recipes removed from the remaining queue. */
final class ProgressScreen extends WorklistGui {

    private final WorklistScreen parent;
    private final WorklistPlan plan;
    private final List<BookmarkItem> outputs;
    private int index;
    private GuiTextField quantity;
    private String error;
    private long remaining;
    private long visible;
    private String saved;

    ProgressScreen(WorklistScreen parent, WorklistPlan plan, BookmarkItem output) {
        this.parent = parent;
        this.plan = plan;
        outputs = plan.progressOutputs();
        if (output != null) for (int i = 0; i < outputs.size(); i++) if (WorklistPlan.outputKey(outputs.get(i))
            .equals(WorklistPlan.outputKey(output))) index = i;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width - 68, 10, 56, 20, "Back"));
        if (outputs.isEmpty()) return;
        buttonList.add(new GuiButton(1, 12, 62, 70, 20, "Previous"));
        buttonList.add(new GuiButton(2, 86, 62, 70, 20, "Next"));
        quantity = new GuiTextField(fontRendererObj, 12, 133, 140, 18);
        quantity.setMaxStringLength(19);
        quantity.setFocused(true);
        quantity.setText(Long.toString(plan.completed(output())));
        buttonList.add(new GuiButton(3, 160, 132, 140, 20, "Save available total"));
        buttonList.add(new GuiButton(4, 12, 178, 140, 20, "Add half remaining [H]"));
        buttonList.add(new GuiButton(5, 160, 178, 140, 20, "Clear record [Delete]"));
        buttonList.add(new GuiButton(6, width - 130, 62, 118, 20, "How stock works [?]"));
        recalculate();
    }

    private BookmarkItem output() {
        return outputs.get(index);
    }

    private void recalculate() {
        remaining = 0;
        visible = 0;
        try {
            for (net.minecraft.item.ItemStack stack : parent.availableInventory()) if (stack != null) {
                BookmarkItem item = BookmarkItem.of(0, stack);
                if (WorklistPlan.outputKey(item)
                    .equals(WorklistPlan.outputKey(output()))) visible = Math.addExact(visible, item.amount);
            }
            RecipeChainMath math = plan.remainingChain(parent.availableInventory());
            for (BookmarkItem result : math.recipeResults) if (WorklistPlan.outputKey(result)
                .equals(WorklistPlan.outputKey(output()))) remaining = Math.addExact(remaining, result.amount);
            error = plan.progressWarning;
        } catch (RuntimeException exception) {
            error = exception.getMessage();
        }
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        drawRect(0, 0, width, height, 0xf5101723);
        drawString(fontRendererObj, "AVAILABLE OUTPUT STOCK", 12, 14, 0x67dbc4);
        if (outputs.isEmpty()) {
            drawString(fontRendererObj, "No recipe outputs in this group.", 12, 42, 0xffffff);
        } else {
            String unit = StackInfo.getFluid(output().itemStack) == null ? "items" : "mB";
            drawString(
                fontRendererObj,
                fontRendererObj.trimStringToWidth(
                    (index + 1) + "/" + outputs.size() + "  " + itemName(output().itemStack),
                    width - 24),
                12,
                40,
                0xffffff);
            drawString(
                fontRendererObj,
                remaining + " " + unit + " remaining / " + output().factor + " per batch",
                12,
                92,
                0x67dbc4);
            quantity.drawTextBox();
            drawString(
                fontRendererObj,
                "Recorded: " + plan.completed(output())
                    + " / Inventory: "
                    + visible
                    + " / Credited: "
                    + Math.max(visible, plan.completed(output())),
                12,
                107,
                0xa9b7cb);
            drawString(
                fontRendererObj,
                "Total still available, including inventory (" + unit + "):",
                12,
                120,
                0xffffff);
            if (error != null) fontRendererObj.drawSplitString(error, 12, 157, width - 24, 0xff8989);
            else if (saved != null) drawString(fontRendererObj, saved, 12, 157, 0x67dbc4);
            drawString(fontRendererObj, "Reduce this total after consuming the recorded items.", 12, 204, 0xe9bd72);
            drawString(
                fontRendererObj,
                "Tab: field/buttons; Enter: save; PgUp/PgDn: output; Esc: back.",
                12,
                height - 18,
                0xa9b7cb);
        }
        super.drawScreen(x, y, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id == 1 || button.id == 2) {
            index = (index + outputs.size() + (button.id == 1 ? -1 : 1)) % outputs.size();
            initGui();
            return;
        }
        if (button.id == 6) {
            mc.displayGuiScreen(
                new InformationScreen(
                    this,
                    "How available stock works",
                    java.util.Arrays.asList(
                        "Record the total output still available to this chain, including copies in inventory. This replaces the saved total; it does not add to it.",
                        "Recorded 64 and inventory 64 credits 64, not 128. If 64 are in a chest and 32 in inventory, record 96.",
                        "Verified worklist crafting updates existing records for consumed inputs and new outputs. Update them yourself after other consumption or production. This is available stock, not lifetime production. Actual crafting always requires physical inputs.",
                        "Add half remaining rounds up to whole recipe batches. Other outputs must be recorded separately. Records are local to this world/server and group snapshot.")));
            return;
        }
        try {
            long amount = 0;
            if (button.id == 3) amount = Long.parseLong(
                quantity.getText()
                    .trim());
            if (button.id == 4) {
                amount = plan.completeHalf(output(), parent.availableInventory());
            }
            if (button.id != 4) plan.setCompleted(output(), amount);
            quantity.setText(Long.toString(amount));
            recalculate();
            saved = "Saved " + amount + ". Credited stock: " + Math.max(visible, amount) + ".";
        } catch (NumberFormatException exception) {
            error = "Enter a non-negative whole quantity (items or mB).";
        } catch (RuntimeException exception) {
            error = "Could not update progress: " + exception.getMessage();
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        if (quantity != null) quantity.mouseClicked(x, y, button);
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == org.lwjgl.input.Keyboard.KEY_TAB && quantity != null) {
            if (quantity.isFocused()) {
                quantity.setFocused(false);
                focusedButton = isShiftKeyDown() ? 6 : 0;
            } else {
                int boundary = isShiftKeyDown() ? 0 : 6;
                if (focusedButton == boundary) {
                    focusedButton = -1;
                    quantity.setFocused(true);
                } else focusKey(key);
            }
            return;
        }
        if (quantity == null || !quantity.isFocused()) if (focusKey(key)) return;
        if (key == 1) mc.displayGuiScreen(parent);
        else if (key == org.lwjgl.input.Keyboard.KEY_H && !org.lwjgl.input.Keyboard.isRepeatEvent())
            actionPerformed(new GuiButton(4, 0, 0, ""));
        else if (key == org.lwjgl.input.Keyboard.KEY_DELETE && quantity != null && !quantity.isFocused())
            actionPerformed(new GuiButton(5, 0, 0, ""));
        else if (character == '?') actionPerformed(new GuiButton(6, 0, 0, ""));
        else if (key == org.lwjgl.input.Keyboard.KEY_RETURN && quantity != null)
            actionPerformed(new GuiButton(3, 0, 0, ""));
        else if (key == org.lwjgl.input.Keyboard.KEY_NEXT && quantity != null)
            actionPerformed(new GuiButton(2, 0, 0, ""));
        else if (key == org.lwjgl.input.Keyboard.KEY_PRIOR && quantity != null)
            actionPerformed(new GuiButton(1, 0, 0, ""));
        else if (quantity != null) quantity.textboxKeyTyped(character, key);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta != 0 && !outputs.isEmpty()) {
            index = (index + outputs.size() + (delta < 0 ? 1 : -1)) % outputs.size();
            initGui();
        }
    }

    @Override
    public void updateScreen() {
        if (mc.thePlayer == null || mc.thePlayer.isDead || mc.thePlayer.getHealth() <= 0) mc.displayGuiScreen(null);
        else if (quantity != null) quantity.updateCursorCounter();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
