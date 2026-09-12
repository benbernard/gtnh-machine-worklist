package com.benbernard.machineworklist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import codechicken.nei.ItemPanels;
import codechicken.nei.bookmark.BookmarkGrid;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerInputHandler;
import cpw.mods.fml.client.registry.ClientRegistry;

public class ClientProxy extends CommonProxy implements IContainerInputHandler {

    static net.minecraft.client.gui.GuiScreen pendingScreen;
    private boolean pendingOpen;
    private boolean pendingChoose;
    private GuiContainer pendingGui;
    private int pendingTicks;

    private static final KeyBinding open = new KeyBinding(
        "key.machineworklist.open",
        Keyboard.KEY_F10,
        "GTNH Machine Worklist");

    @Override
    public void init() {
        ClientRegistry.registerKeyBinding(open);
        GuiContainerManager.addInputHandler(this);
        GuiContainerManager.addDrawHandler(new EntryFeedback());
        net.minecraftforge.client.ClientCommandHandler.instance.registerCommand(new WorklistCommand());
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @cpw.mods.fml.common.eventhandler.SubscribeEvent
    public void tick(cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        if (event.phase != cpw.mods.fml.common.gameevent.TickEvent.Phase.END) return;
        CraftingSession.tick();
        if (pendingOpen) {
            pendingOpen = false;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen == pendingGui) open(pendingGui, pendingChoose);
            else pendingTicks = 0;
        }
        if (pendingScreen == null) return;
        net.minecraft.client.gui.GuiScreen screen = pendingScreen;
        pendingScreen = null;
        if (Minecraft.getMinecraft().thePlayer != null) Minecraft.getMinecraft()
            .displayGuiScreen(screen);
    }

    @Override
    public boolean lastKeyTyped(GuiContainer gui, char character, int key) {
        if (Keyboard.isRepeatEvent() || !isOpenKey(key)) return false;
        if (Minecraft.getMinecraft().currentScreen != gui) return false;
        open(gui, net.minecraft.client.gui.GuiScreen.isShiftKeyDown());
        return true;
    }

    static boolean isOpenKey(int key) {
        return key == open.getKeyCode();
    }

    static java.nio.file.Path navigationFile() {
        String world = codechicken.nei.NEIClientConfig.getWorldPath();
        return world == null ? null
            : WorklistPosition.file(
                Minecraft.getMinecraft().mcDataDir.toPath()
                    .resolve("config/machineworklist/navigation"),
                world);
    }

    @cpw.mods.fml.common.eventhandler.SubscribeEvent
    public void keyInput(cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!Keyboard.getEventKeyState() || Keyboard.isRepeatEvent()
            || !isOpenKey(Keyboard.getEventKey())
            || mc.currentScreen != null
            || mc.thePlayer == null
            || mc.thePlayer.isDead) return;
        // Always create a fresh, real inventory GUI; a closed table/backpack cannot be reused.
        mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiInventory(mc.thePlayer));
        pendingOpen = true;
        pendingGui = (GuiContainer) mc.currentScreen;
        pendingTicks = 0;
        pendingChoose = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
    }

    private void open(GuiContainer gui, boolean chooseGroup) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        GuiContainer active = CraftingInventory.activeGui(gui, mc.thePlayer.openContainer);
        if (active == null) {
            mc.thePlayer.addChatMessage(
                new net.minecraft.util.ChatComponentText(
                    "That crafting container has closed. Open your inventory or crafting table, then press F10."));
            return;
        }
        gui = active;
        if (!EntryFeedback.allow(gui)) return;
        if (!bookmarksLoaded()) {
            if (++pendingTicks <= 300) {
                pendingOpen = true;
                pendingGui = gui;
                pendingChoose = chooseGroup;
            } else {
                pendingTicks = 0;
                mc.thePlayer.addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                        "NEI is still loading bookmarks. Keep inventory open, then press F10 again when loading finishes."));
            }
            return;
        }
        pendingTicks = 0;
        if (ItemPanels.bookmarkPanel != null) ItemPanels.bookmarkPanel.update();
        if (chooseGroup) {
            mc.displayGuiScreen(new GroupScreen(gui));
            return;
        }
        if (ItemPanels.bookmarkPanel == null) {
            Minecraft.getMinecraft()
                .displayGuiScreen(new GroupScreen(gui));
            return;
        }
        java.nio.file.Path file = navigationFile();
        WorklistPosition remembered = file == null ? null : WorklistPosition.load(file);
        if (remembered != null) {
            try {
                BookmarkGrid current = ItemPanels.bookmarkPanel.getGrid();
                WorklistPlan plan = remembered.group == -1
                    ? WorklistPlan
                        .example(new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.crafting_table), 8)
                    : current.isCraftingMode(remembered.group) ? WorklistPlan.capture(current, remembered.group) : null;
                if (plan != null && remembered.matches(plan.groupId, plan.snapshotKey())) {
                    mc.displayGuiScreen(new WorklistScreen(gui, plan, remembered));
                    return;
                }
            } catch (RuntimeException failure) {
                // A deleted/changed recipe must not make remembered navigation block the picker.
            }
            mc.displayGuiScreen(new GroupScreen(gui, "Saved chain unavailable here; choose a group."));
            return;
        }
        int group = ItemPanels.bookmarkPanel.getHoveredGroupId(false);
        if (group < 0) group = ItemPanels.bookmarkPanel.getHoveredGroupId(true);
        BookmarkGrid grid = ItemPanels.bookmarkPanel.getGrid();
        if (group < 0) {
            // A page with one crafting group has no ambiguous selection; also works
            // for keyboard users whose pointer is outside the bookmark panel.
            java.util.Set<Integer> craftingGroups = new java.util.HashSet<>();
            for (int i = 0; i < grid.size(); i++) {
                int candidate = grid.getBookmarkItem(i).groupId;
                if (grid.isCraftingMode(candidate)) craftingGroups.add(candidate);
            }
            if (craftingGroups.size() == 1) group = craftingGroups.iterator()
                .next();
        }
        if (group < 0 || !grid.isCraftingMode(group)) {
            mc.displayGuiScreen(new GroupScreen(gui));
            return;
        }
        try {
            mc.displayGuiScreen(new WorklistScreen(gui, WorklistPlan.capture(grid, group)));
        } catch (RuntimeException failure) {
            mc.displayGuiScreen(
                new InformationScreen(
                    gui,
                    "Cannot open this group",
                    java.util.Arrays.asList(
                        "Check the selected recipes in NEI and reopen the worklist. If this persists, include the following detail in a bug report.",
                        failure.getClass()
                            .getSimpleName() + ": "
                            + failure.getMessage())));
        }
    }

    private static boolean bookmarksLoaded() {
        if (!codechicken.nei.ItemList.loadFinished || ItemPanels.bookmarkPanel == null) return false;
        try {
            // In this NEI version loadFinished precedes bookmark loading; bookmarkFile is assigned last.
            java.lang.reflect.Field storageField = codechicken.nei.BookmarkPanel.class.getDeclaredField("storage");
            storageField.setAccessible(true);
            Object storage = storageField.get(ItemPanels.bookmarkPanel);
            java.lang.reflect.Field fileField = codechicken.nei.bookmark.BookmarkStorage.class
                .getDeclaredField("bookmarkFile");
            fileField.setAccessible(true);
            return fileField.get(storage) != null;
        } catch (ReflectiveOperationException failure) {
            return true; // Older NEI versions still retain the public item-loading gate.
        }
    }

    @Override
    public boolean keyTyped(GuiContainer gui, char character, int key) {
        return false;
    }

    @Override
    public void onKeyTyped(GuiContainer gui, char character, int key) {}

    @Override
    public boolean mouseClicked(GuiContainer gui, int x, int y, int button) {
        return false;
    }

    @Override
    public void onMouseClicked(GuiContainer gui, int x, int y, int button) {}

    @Override
    public void onMouseUp(GuiContainer gui, int x, int y, int button) {}

    @Override
    public boolean mouseScrolled(GuiContainer gui, int x, int y, int delta) {
        return false;
    }

    @Override
    public void onMouseScrolled(GuiContainer gui, int x, int y, int delta) {}

    @Override
    public void onMouseDragged(GuiContainer gui, int x, int y, int button, long time) {}
}
