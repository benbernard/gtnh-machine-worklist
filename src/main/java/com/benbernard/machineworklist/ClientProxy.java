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

    private final KeyBinding open = new KeyBinding(
        "key.machineworklist.open",
        Keyboard.KEY_F10,
        "GTNH Machine Worklist");

    @Override
    public void init() {
        ClientRegistry.registerKeyBinding(open);
        GuiContainerManager.addInputHandler(this);
        net.minecraftforge.client.ClientCommandHandler.instance.registerCommand(new WorklistCommand());
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @cpw.mods.fml.common.eventhandler.SubscribeEvent
    public void tick(cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        if (event.phase != cpw.mods.fml.common.gameevent.TickEvent.Phase.END) return;
        CraftingSession.tick();
        if (pendingScreen == null) return;
        net.minecraft.client.gui.GuiScreen screen = pendingScreen;
        pendingScreen = null;
        if (Minecraft.getMinecraft().thePlayer != null) Minecraft.getMinecraft()
            .displayGuiScreen(screen);
    }

    @Override
    public boolean lastKeyTyped(GuiContainer gui, char character, int key) {
        if (Keyboard.isRepeatEvent() || key != open.getKeyCode()) return false;
        if (ItemPanels.bookmarkPanel == null) {
            Minecraft.getMinecraft()
                .displayGuiScreen(new GroupScreen(gui));
            return true;
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
        Minecraft mc = Minecraft.getMinecraft();
        if (group < 0 || !grid.isCraftingMode(group)) {
            mc.displayGuiScreen(new GroupScreen(gui));
            return true;
        }
        mc.displayGuiScreen(new WorklistScreen(gui, WorklistPlan.capture(grid, group)));
        return true;
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
