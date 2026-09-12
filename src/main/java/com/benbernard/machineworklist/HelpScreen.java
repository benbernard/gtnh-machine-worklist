package com.benbernard.machineworklist;

import java.util.Arrays;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;

/** Short, task-based help available without leaving the current worklist. */
final class HelpScreen extends WorklistGui {

    private static final String[] TOPICS = { "Getting started", "Reading the queue", "Crafting a chain",
        "Available stock", "Fixing blocked work", "Containers and controls" };
    private static final String[][] TEXT = { {
        "FIRST TRY: Press F10 (or your rebound Worklist key). It resumes your last unchanged chain on the current NEI page, including its recipe, tab, filter and scroll position. Shift + the Worklist key opens the group picker; B also chooses a group from the queue. In the picker, E opens an eight-table example using this pack's real recipe. It gives no items and changes no NEI bookmarks.",
        "YOUR OWN PLAN: In NEI, press R on the target, then choose the recipe you want. Hover its heart (Favorite) control and press Shift + your Bookmark key (A by default). The tooltip calls this Save Recipe Tree. It uses the ingredient recipes you have chosen as favorites.",
        "Saved recipe trees use Crafting Chain mode. For ordinary bookmark groups, right-click the group bracket to toggle that mode; for the ungrouped page, right-click the page header. Ctrl + mouse wheel over the target changes its quantity.",
        "With no saved position, F10 imports the hovered crafting group, opens the only group directly, or shows the picker. Positions save per world/server and survive restarts. A changed or missing group opens the picker; a completed recipe returns to its queue. After changing recipes, quantities or NEI pages, choose the group again. You choose recipes; this mod does not choose replacements." },
        { "The queue shows remaining work after inventory and recorded stock are credited. A run is one execution of a recipe; outputs show how many items or mB those runs produce. The target is a total to have available, not an extra amount to craft.",
            "Machines (1), Crafting (2) and All (3) filter the rows. R toggles ready-only rows. These filters do not limit Craft group or change the overall missing-input totals.",
            "READY means physical recipe inputs are present. It does not check machine tier, power or contents, and crafting may still need a larger grid or more output space. WAITING means check the inputs or make the selected upstream recipes first.",
            "M shows overall missing external inputs and reusable tools. At wide GUI sizes this is a separate pane; scroll each pane independently. Open a recipe row with a click or arrows + Enter. In details, B explains its status, N opens the exact NEI recipe, and I opens the selected item's full information.",
            "Detail rows include outputs, inputs, reusable tools and upstream recipes. Click an upstream row, or select it with arrows and Enter, to inspect it. Scroll to read every explanation and recipe note. Escape returns to the queue." },
        { "F / Craft 1 batch runs the selected recipe exactly once, using physical ingredients already available. One batch may produce several items. It does not make missing ingredients first.",
            "G / Craft chain in recipe details includes that recipe and its chosen upstream dependencies. It makes ready intermediates and then downstream outputs toward the remaining target. Unrelated targets are excluded.",
            "G / Craft group from the queue advances all chosen crafting recipes in this group, including rows hidden by the current tab or ready filter. Both modes reuse NEI's bulk transfer support, recheck inventory between transfers, and continue with another ready recipe. They never choose new recipes.",
            "Machines remain manual. A chain may craft their ingredients, run independent ready branches, then pause. Read the result for completed batches and blockers; supply the missing items or machine outputs, reopen the appropriate container and start again.",
            "Escape while transfers are running closes the container and stops further crafting. Finished transfers remain in inventory. Reopening and pressing G calculates the remaining work from current stock. Large requests may pause for output space; move outputs to storage and update Available stock before resuming." },
        { "C / Available stock records outputs still available elsewhere, including copies in inventory. Enter an exact total and press Enter or Save available total. This replaces the record; it does not add to it. Quantities are items, or mB for fluids.",
            "Example: record 96 if 64 are in a chest and 32 in inventory. The credited quantity is the larger of inventory and the recorded total. Recorded 64 plus inventory 64 credits 64, not 128.",
            "Verified crafting in this worklist updates existing records for consumed inputs and new outputs. After consuming or producing items elsewhere, update their records yourself. This is available stock, not lifetime production. Recorded items reduce planned work but cannot supply a crafting grid: bring physical ingredients into inventory or the open supported backpack.",
            "H in the stock editor adds half the remaining output, rounded up to whole recipe batches. Clear record removes only the manual record; actual inventory still counts. Other outputs are recorded separately. Previous/Next or the wheel changes outputs; save edits before switching.",
            "Records are saved locally per world/server and group snapshot. Reopening an unchanged group restores them. Changed recipe selections or targets create a different snapshot. You can review and clear completed outputs even after they disappear from the queue." },
        { "A disabled F or G button means the request cannot start now. Press its shortcut for a full explanation, or Tab to that button and press Enter. In recipe details B also shows status. A downstream recipe can be WAITING while G is available to make its ingredients.",
            "MISSING INPUTS: Read the named items, tool configurations and amounts. Supply them, craft chosen upstream recipes, or perform the machine operation shown in NEI. Machines, chests, nearby storage and worn/closed backpacks are not scanned.",
            "GRID OR CURSOR OCCUPIED: Put the cursor item in a slot and empty the current container's crafting grid and result slot before F10. The warning identifies the backpack when you opened from its grid. Items in a closed backpack do not block a table. Stored supplies in an open supported backpack, or an attached Tinkers' chest, can stay.",
            "GRID TOO SMALL: Close the worklist, open a real 3x3 table or supported backpack, then press F10 again. INVENTORY SPACE: Free player-inventory slots for outputs and returned tools. Even when an output stack has room, NEI transfers require a free slot.",
            "NEI OFF OR BUSY: Enable Autocrafting in NEI Options > Inventory, or wait for NEI's current request to finish. RECIPE UNAVAILABLE: Select it again in NEI and reopen the group. CONTAINER CLOSED: Reopen the actual container and then the worklist.",
            "PAUSED OR TRANSFER STOPPED: Read the result, inspect actual inventory/grid contents, correct the blocker and start again. Completed items are kept. No missing recipes are invented. Closing the container, death or disconnection stops further transfers." },
        { "Open the worklist from survival inventory for 2x2 recipes, from a crafting table for 3x3, or from the supported Adventure Backpack 1.3.13-GTNH GUI. Supplies come from player inventory/hotbar plus ordinary storage in that open backpack. Its bottom-right 3x3 crafting slots must be empty. Hidden mirror, tool and tank slots are not supplies.",
            "The queue header names the active crafting grid. Open a Tinkers' Crafting Station before F10 to use its 3x3 grid. Attached chest storage can stay occupied, but worklist crafting takes supplies from player inventory: move needed ingredients there first. Reopening remembers the chain and position, while using the container you just opened. A closed backpack is never selected instead of that table.",
            "F10 opens/resumes the worklist, and closes the whole worklist from details, stock, help or status without backing through screens. Close in the queue or recipe header does the same. Escape goes back one screen. Closing from stock does not save an unfinished quantity edit: press Enter to save first. Reopening restores the underlying queue/recipe position, not a help page or active crafting request.",
            "F10 is rebindable under Options > Controls > GTNH Machine Worklist. From the world, it opens a fresh player 2x2 inventory. Open a table/backpack first for its 3x3 grid and storage. Shift + the Worklist key chooses another group. /machineworklist opens the picker after NEI initializes; /machineworklist <group number> opens that group using the player grid.",
            "QUEUE: 1/2/3 tabs, R ready filter, M missing inputs, G craft whole group, C stock, B choose group, H help. DETAILS: F one batch, G selected chain, N NEI recipe, B status, I item information, C stock, H help.",
            "NAVIGATION: Click rows or use arrows + Enter. Tab / Shift-Tab selects buttons, including disabled buttons whose Enter action explains blockers. Wheel scrolls lists and text. Escape goes back; during active crafting it closes the container and cancels further transfers.",
            "HELP: Press 1-6 here to choose a topic. Scroll a topic with arrows, wheel or Page Up/Down; Home/End jump to its ends. Escape returns to this topic list, then to the screen you came from. Worklist screens do not pause the world." } };

    private final GuiScreen parent;

    @Override
    protected GuiScreen parentScreen() {
        return parent;
    }

    HelpScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width - 68, 10, 56, 20, "Back"));
        addCloseButton();
        int topicWidth = Math.min(300, width - 24);
        for (int i = 0; i < TOPICS.length; i++) buttonList.add(
            new GuiButton(i + 1, (width - topicWidth) / 2, 46 + i * 25, topicWidth, 20, (i + 1) + ". " + TOPICS[i]));
    }

    @Override
    public void drawScreen(int x, int y, float ticks) {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        drawRect(0, 0, width, height, 0xff101723);
        drawString(fontRendererObj, "WORKLIST HELP", 12, 15, 0x67dbc4);
        drawString(fontRendererObj, "1-6: topic. Tab: buttons. Esc: back. F10: close.", 12, height - 18, 0xa9b7cb);
        super.drawScreen(x, y, ticks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) returnTo(parent);
        else mc
            .displayGuiScreen(new InformationScreen(this, TOPICS[button.id - 1], Arrays.asList(TEXT[button.id - 1])));
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (focusKey(key)) return;
        if (key == Keyboard.KEY_ESCAPE) returnTo(parent);
        if (key >= Keyboard.KEY_1 && key <= Keyboard.KEY_6)
            actionPerformed(new GuiButton(1 + key - Keyboard.KEY_1, 0, 0, ""));
    }
}
