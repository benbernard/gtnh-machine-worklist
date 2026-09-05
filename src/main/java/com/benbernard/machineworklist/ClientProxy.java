package com.benbernard.machineworklist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;

import org.lwjgl.input.Keyboard;

import codechicken.nei.ItemPanels;
import codechicken.nei.bookmark.BookmarkGrid;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerInputHandler;
import cpw.mods.fml.client.registry.ClientRegistry;

public class ClientProxy extends CommonProxy implements IContainerInputHandler {

    private final KeyBinding open = new KeyBinding("key.machineworklist.open", Keyboard.KEY_P, "GTNH Machine Worklist");

    @Override
    public void init() {
        ClientRegistry.registerKeyBinding(open);
        GuiContainerManager.addInputHandler(this);
        net.minecraftforge.client.ClientCommandHandler.instance.registerCommand(new WorklistCommand());
    }

    @Override
    public boolean lastKeyTyped(GuiContainer gui, char character, int key) {
        if (Keyboard.isRepeatEvent() || key != open.getKeyCode()) return false;
        if (ItemPanels.bookmarkPanel == null) return false;
        int group = ItemPanels.bookmarkPanel.getHoveredGroupId(false);
        if (group < 0) group = ItemPanels.bookmarkPanel.getHoveredGroupId(true);
        BookmarkGrid grid = ItemPanels.bookmarkPanel.getGrid();
        Minecraft mc = Minecraft.getMinecraft();
        if (group < 0 || !grid.isCraftingMode(group)) {
            mc.thePlayer.addChatMessage(
                new ChatComponentText("Hover an NEI autocrafting group and press P to open its machine work list."));
            return false;
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
