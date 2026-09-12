package com.benbernard.machineworklist;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Scrollable visible explanation, also usable without hovering. */
final class InformationScreen extends WorklistGui {

    private final GuiScreen parent;
    private final String title;
    private final List<String> text;
    private final List<String> lines = new ArrayList<>();
    private int scroll;

    InformationScreen(GuiScreen parent, String title, List<String> text) {
        this.parent = parent;
        this.title = title;
        this.text = text;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width - 68, 10, 56, 20, "Back"));
        lines.clear();
        for (String line : text) {
            lines.addAll(fontRendererObj.listFormattedStringToWidth(line, width - 32));
            lines.add("");
        }
    }

    private int visible() {
        return Math.max(1, (height - 76) / 12);
    }

    private void move(int delta) {
        scroll = Math.max(0, Math.min(scroll + delta, lines.size() - visible()));
    }

    @Override
    public void drawScreen(int x, int y, float ticks) {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        drawRect(0, 0, width, height, 0xff101723);
        drawString(fontRendererObj, fontRendererObj.trimStringToWidth(title, width - 90), 12, 15, 0x67dbc4);
        for (int i = scroll; i < Math.min(lines.size(), scroll + visible()); i++)
            drawString(fontRendererObj, lines.get(i), 16, 48 + (i - scroll) * 12, 0xffffff);
        drawString(fontRendererObj, "Arrows / wheel: scroll. Esc: back.", 12, height - 18, 0xa9b7cb);
        super.drawScreen(x, y, ticks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        mc.displayGuiScreen(parent);
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (focusKey(key)) return;
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
        if (key == Keyboard.KEY_DOWN) move(1);
        if (key == Keyboard.KEY_UP) move(-1);
        if (key == Keyboard.KEY_NEXT) move(visible());
        if (key == Keyboard.KEY_PRIOR) move(-visible());
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) move(wheel < 0 ? 1 : -1);
    }
}
