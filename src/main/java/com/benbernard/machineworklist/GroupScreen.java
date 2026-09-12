package com.benbernard.machineworklist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import codechicken.nei.ItemPanels;
import codechicken.nei.bookmark.BookmarkGrid;
import codechicken.nei.bookmark.BookmarkItem;

/** Keyboard-accessible selection and first-run help on the current screen. */
final class GroupScreen extends WorklistGui {

    private final GuiScreen parent;
    private final String message;
    private final List<Integer> groups = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private int row;
    private int scroll;

    @Override
    protected GuiScreen parentScreen() {
        return parent;
    }

    GroupScreen(GuiScreen parent) {
        this(parent, "Autocrafting groups on the current NEI page");
    }

    GroupScreen(GuiScreen parent, String message) {
        this.parent = parent;
        this.message = message;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width - 68, 10, 56, 20, "Back"));
        addCloseButton();
        buttonList.add(new GuiButton(1, 12, 42, 140, 20, "Worklist help [H]"));
        buttonList.add(new GuiButton(2, 158, 42, 142, 20, "Example: 8 tables [E]"));
        groups.clear();
        labels.clear();
        if (ItemPanels.bookmarkPanel == null) return;
        BookmarkGrid grid = ItemPanels.bookmarkPanel.getGrid();
        Set<Integer> ids = new TreeSet<>();
        for (int i = 0; i < grid.size(); i++) {
            int id = grid.getBookmarkItem(i).groupId;
            if (grid.isCraftingMode(id)) ids.add(id);
        }
        for (int id : ids) {
            List<String> names = new ArrayList<>();
            for (int i = 0; i < grid.size(); i++) {
                BookmarkItem item = grid.getBookmarkItem(i);
                if (item.groupId == id && item.type == BookmarkItem.BookmarkItemType.RESULT && item.amount > 0)
                    names.add(item.amount + " x " + itemName(item.itemStack));
            }
            groups.add(id);
            labels.add("Group " + id + ": " + (names.isEmpty() ? "Open planned recipes" : String.join(", ", names)));
        }
    }

    private int visible() {
        return Math.max(1, (height - 126) / 32);
    }

    private void move(int delta) {
        focusedButton = -1;
        row = Math.max(0, Math.min(groups.size() - 1, row + delta));
        if (row < scroll) scroll = row;
        if (row >= scroll + visible()) scroll = row - visible() + 1;
    }

    @Override
    public void drawScreen(int x, int y, float ticks) {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        drawRect(0, 0, width, height, 0xff101723);
        drawString(fontRendererObj, "CHOOSE A WORKLIST", 12, 15, 0x67dbc4);
        drawString(fontRendererObj, fontRendererObj.trimStringToWidth(message, width - 24), 12, 72, 0xa9b7cb);
        if (groups.isEmpty()) fontRendererObj.drawSplitString(
            "No autocrafting groups found on this page. Press H for setup, or E to try the example without changing bookmarks.",
            12,
            96,
            width - 24,
            0xe9bd72);
        for (int i = scroll; i < Math.min(groups.size(), scroll + visible()); i++) {
            int top = 94 + (i - scroll) * 32;
            drawRect(12, top, width - 12, top + 28, i == row ? 0xff36516b : 0xff202d40);
            drawString(
                fontRendererObj,
                fontRendererObj.trimStringToWidth(labels.get(i), width - 40),
                20,
                top + 9,
                0xffffff);
        }
        drawString(
            fontRendererObj,
            "Arrows + Enter: open. Tab: buttons. H: help. Esc: back.",
            12,
            height - 18,
            0xa9b7cb);
        super.drawScreen(x, y, ticks);
    }

    private void open() {
        if (groups.isEmpty() || ItemPanels.bookmarkPanel == null) return;
        try {
            mc.displayGuiScreen(
                new WorklistScreen(parent, WorklistPlan.capture(ItemPanels.bookmarkPanel.getGrid(), groups.get(row))));
        } catch (RuntimeException failure) {
            mc.displayGuiScreen(
                new InformationScreen(
                    this,
                    "Cannot open this group",
                    Arrays.asList(
                        "The selected group could not be imported. Reopen inventory and check its chosen recipes in NEI.",
                        failure.getClass()
                            .getSimpleName() + ": "
                            + failure.getMessage())));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) returnTo(parent);
        if (button.id == 1) mc.displayGuiScreen(new HelpScreen(this));
        if (button.id == 2) {
            try {
                mc.displayGuiScreen(
                    new WorklistScreen(parent, WorklistPlan.example(new ItemStack(Blocks.crafting_table), 8)));
            } catch (RuntimeException failure) {
                mc.displayGuiScreen(
                    new InformationScreen(
                        this,
                        "Example unavailable",
                        Arrays.asList(
                            "NEI could not resolve the crafting-table example in this pack. Choose its recipe manually and save it as an autocrafting group.",
                            failure.getMessage())));
            }
        }
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (focusKey(key)) return;
        if (key == Keyboard.KEY_ESCAPE) returnTo(parent);
        if (key == Keyboard.KEY_H) actionPerformed(new GuiButton(1, 0, 0, ""));
        if (key == Keyboard.KEY_E) actionPerformed(new GuiButton(2, 0, 0, ""));
        if (key == Keyboard.KEY_DOWN) move(1);
        if (key == Keyboard.KEY_UP) move(-1);
        if (key == Keyboard.KEY_RETURN) open();
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        if (button == 0 && x >= 12 && x < width - 12 && y >= 94 && y < 94 + visible() * 32) {
            int candidate = scroll + (y - 94) / 32;
            if (candidate < groups.size()) {
                row = candidate;
                open();
            }
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) move(wheel < 0 ? 1 : -1);
    }
}
