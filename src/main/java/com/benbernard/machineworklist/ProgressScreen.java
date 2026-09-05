package com.benbernard.machineworklist;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import org.lwjgl.input.Mouse;

import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.recipe.StackInfo;
import codechicken.nei.recipe.chain.RecipeChainMath;

/** Editable manual output stock, including recipes removed from the remaining queue. */
final class ProgressScreen extends GuiScreen {

    private final WorklistScreen parent;
    private final WorklistPlan plan;
    private final List<BookmarkItem> outputs;
    private int index;
    private GuiTextField quantity;
    private String error;
    private long remaining;

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
        quantity = new GuiTextField(fontRendererObj, 12, 111, 140, 18);
        quantity.setMaxStringLength(19);
        quantity.setFocused(true);
        quantity.setText(Long.toString(plan.completed(output())));
        buttonList.add(new GuiButton(3, 160, 110, 140, 20, "Set completed total"));
        buttonList.add(new GuiButton(4, 12, 136, 140, 20, "Complete half remaining"));
        buttonList.add(new GuiButton(5, 160, 136, 140, 20, "Clear this entry"));
        recalculate();
    }

    private BookmarkItem output() {
        return outputs.get(index);
    }

    private void recalculate() {
        remaining = 0;
        try {
            RecipeChainMath math = plan.remainingChain(mc.thePlayer.inventory.mainInventory);
            for (BookmarkItem result : math.recipeResults) if (WorklistPlan.outputKey(result)
                .equals(WorklistPlan.outputKey(output()))) remaining = Math.addExact(remaining, result.amount);
            error = plan.progressWarning;
        } catch (RuntimeException exception) {
            error = exception.getMessage();
        }
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        drawDefaultBackground();
        drawString(fontRendererObj, "MANUAL COMPLETION", 12, 14, 0x67dbc4);
        if (outputs.isEmpty()) {
            drawString(fontRendererObj, "No recipe outputs in this group.", 12, 42, 0xffffff);
        } else {
            String unit = StackInfo.getFluid(output().itemStack) == null ? "items" : "mB";
            drawString(
                fontRendererObj,
                fontRendererObj.trimStringToWidth(
                    (index + 1) + "/" + outputs.size() + "  " + output().itemStack.getDisplayName(),
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
            fontRendererObj.drawSplitString(
                "Enter the total completed output still available for this chain (" + unit
                    + "), including inventory copies. Inventory is not counted twice. Half rounds up to whole batches."
                    + " Update or clear this count when you use those outputs in later steps."
                    + " Other recipe outputs are recorded separately; ready counts use real inventory only.",
                12,
                166,
                width - 24,
                0xa9b7cb);
            if (error != null) fontRendererObj.drawSplitString(error, 12, height - 32, width - 24, 0xff8989);
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
        try {
            long amount = 0;
            if (button.id == 3) amount = Long.parseLong(
                quantity.getText()
                    .trim());
            if (button.id == 4) {
                amount = plan.completeHalf(output(), mc.thePlayer.inventory.mainInventory);
            }
            if (button.id != 4) plan.setCompleted(output(), amount);
            quantity.setText(Long.toString(amount));
            recalculate();
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
        if (key == 1) mc.displayGuiScreen(parent);
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
