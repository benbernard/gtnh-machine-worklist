package com.benbernard.machineworklist;

import java.util.Set;
import java.util.TreeSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import codechicken.nei.ItemPanels;
import codechicken.nei.bookmark.BookmarkGrid;

/** Client command: never sends a command to the multiplayer server. */
public class WorklistCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "machineworklist";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/machineworklist [group number]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] arguments) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || ItemPanels.bookmarkPanel == null) return;
        BookmarkGrid grid = ItemPanels.bookmarkPanel.getGrid();
        if (grid.size() == 0) {
            sender.addChatMessage(
                new ChatComponentText(
                    "Open your inventory once to load NEI bookmarks, then select an autocrafting group."));
            return;
        }
        Set<Integer> groups = new TreeSet<>();
        for (int i = 0; i < grid.size(); i++) {
            int group = grid.getBookmarkItem(i).groupId;
            if (grid.isCraftingMode(group)) groups.add(group);
        }
        if (arguments.length == 0) {
            sender.addChatMessage(
                new ChatComponentText(
                    "Autocrafting groups on this NEI page: " + groups
                        + ". Use /machineworklist <number>, or hover a group and press the worklist key."));
            return;
        }
        int group;
        try {
            group = Integer.parseInt(arguments[0]);
        } catch (NumberFormatException invalid) {
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
            return;
        }
        if (!groups.contains(group)) {
            sender
                .addChatMessage(new ChatComponentText("No autocrafting group " + group + " on the current NEI page."));
            return;
        }
        // GuiChat closes itself after executing a command; open on the following client tick.
        GuiInventory inventory = new GuiInventory(mc.thePlayer);
        net.minecraft.client.gui.ScaledResolution resolution = new net.minecraft.client.gui.ScaledResolution(
            mc,
            mc.displayWidth,
            mc.displayHeight);
        // NEI checks the parent GUI's Minecraft reference when inspecting usable slots.
        // A command-created inventory has not yet been displayed/initialized by Minecraft.
        inventory.setWorldAndResolution(mc, resolution.getScaledWidth(), resolution.getScaledHeight());
        ClientProxy.pendingScreen = new WorklistScreen(inventory, WorklistPlan.capture(grid, group));
    }
}
