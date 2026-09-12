package com.benbernard.machineworklist;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import codechicken.nei.FastTransferManager;
import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.recipe.RecipeHandlerRef;

/** Plans overflow on copies, then uses ordinary container clicks to prepare NEI's player workspace. */
final class CraftingSpace {

    interface Transfer {

        boolean active();

        ItemStack cursor();

        void click(Slot slot);

        EntityPlayer player();
    }

    static final class Placement {

        final Slot slot;
        final ItemStack before;
        final int count;

        Placement(Slot slot, ItemStack before, int count) {
            this.slot = slot;
            this.before = copy(before);
            this.count = count;
        }
    }

    static final class Move {

        final Slot source;
        final ItemStack stack;
        final List<Placement> placements = new ArrayList<>();

        Move(Slot source, ItemStack stack) {
            this.source = source;
            this.stack = copy(stack);
        }
    }

    static final class Plan {

        long batches;
        final List<Move> moves = new ArrayList<>();

        boolean execute(Transfer transfer) {
            if (!transfer.active() || transfer.cursor() != null) return false;
            for (Move move : moves) {
                if (!transfer.active() || !same(move.source.getStack(), move.stack)
                    || !move.source.canTakeStack(transfer.player())) return false;
                // Check all destinations before picking up anything.
                for (Placement place : move.placements) if (!valid(place, move.stack, transfer)) return false;
                transfer.click(move.source);
                if (!same(transfer.cursor(), move.stack) || move.source.getStack() != null) {
                    restore(move.source, transfer);
                    return false;
                }
                for (Placement place : move.placements) {
                    if (!valid(place, move.stack, transfer)) {
                        restore(move.source, transfer);
                        return false;
                    }
                    ItemStack cursor = copy(transfer.cursor());
                    transfer.click(place.slot);
                    ItemStack expected = copy(move.stack);
                    expected.stackSize = (place.before == null ? 0 : place.before.stackSize) + place.count;
                    cursor.stackSize -= place.count;
                    if (!same(place.slot.getStack(), expected)
                        || !same(transfer.cursor(), cursor.stackSize == 0 ? null : cursor)) {
                        restore(move.source, transfer);
                        return false;
                    }
                }
                if (transfer.cursor() != null) {
                    restore(move.source, transfer);
                    return false;
                }
            }
            return transfer.active() && transfer.cursor() == null;
        }
    }

    private static boolean valid(Placement place, ItemStack item, Transfer transfer) {
        return transfer.active() && same(place.slot.getStack(), place.before)
            && place.slot.isItemValid(item)
            && place.slot.canTakeStack(transfer.player())
            && Math.min(place.slot.getSlotStackLimit(), item.getMaxStackSize())
                >= (place.before == null ? 0 : place.before.stackSize) + place.count;
    }

    private static void restore(Slot source, Transfer transfer) {
        ItemStack cursor = transfer.cursor();
        // Never send clicks into a replacement/closed container, swap unrelated items, or drop a stack.
        if (transfer.active() && cursor != null
            && source.getStack() == null
            && source.isItemValid(cursor)
            && cursor.stackSize <= Math.min(source.getSlotStackLimit(), cursor.getMaxStackSize()))
            transfer.click(source);
    }

    static boolean execute(Plan plan, final GuiContainer gui) {
        return plan.execute(new Transfer() {

            public EntityPlayer player() {
                return Minecraft.getMinecraft().thePlayer;
            }

            public boolean active() {
                Minecraft mc = Minecraft.getMinecraft();
                return mc.thePlayer != null && !mc.thePlayer.isDead
                    && mc.thePlayer.getHealth() > 0
                    && mc.currentScreen == gui
                    && mc.thePlayer.openContainer == gui.inventorySlots;
            }

            public ItemStack cursor() {
                return player().inventory.getItemStack();
            }

            public void click(Slot slot) {
                FastTransferManager.clickSlot(gui, slot.slotNumber, 0, 0);
            }
        });
    }

    static int returnedSlots(RecipeHandlerRef handler) {
        int slots = 0;
        if (handler != null) for (codechicken.nei.PositionedStack ingredient : handler.handler
            .getIngredientStacks(handler.recipeIndex)) {
                for (ItemStack option : ingredient.items) if (option.getItem()
                    .hasContainerItem(option) || option.stackSize == 0) {
                        slots++;
                        break;
                    }
            }
        return slots;
    }

    static Plan plan(GuiContainer gui, WorklistPlan.Step step, long maximum) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        boolean backpack = CraftingInventory.backpack(gui);
        List<Slot> storage = backpack || CraftingInventory.station(gui)
            ? ContainerStorage.outputSlots(gui.inventorySlots, backpack)
            : new ArrayList<>();
        List<Slot> playerSlots = new ArrayList<>();
        boolean[] seen = new boolean[36];
        for (Slot slot : gui.inventorySlots.inventorySlots) {
            int index = slot.getSlotIndex();
            if (slot.inventory == player.inventory && index >= 0 && index < 36 && !seen[index]) {
                seen[index] = true;
                playerSlots.add(slot);
            }
        }
        return plan(
            player.inventory.mainInventory,
            playerSlots,
            storage,
            step.outputs,
            Math.min(maximum, CraftingBurst.MAX_BATCHES),
            returnedSlots(RecipeHandlerRef.of(step.id)),
            player.inventory.currentItem,
            player);
    }

    static Plan plan(ItemStack[] inventory, List<Slot> playerSlots, List<Slot> storage, List<BookmarkItem> outputs,
        long maximum, int returnedSlots, int held, EntityPlayer player) {
        Plan result = new Plan();
        ItemStack[] stock = new ItemStack[inventory.length];
        for (int i = 0; i < stock.length; i++) stock[i] = copy(inventory[i]);
        result.batches = capacity(stock, outputs, maximum, returnedSlots);
        if (result.batches >= maximum) return result;
        List<Slot> candidates = new ArrayList<>(playerSlots);
        // Stash crafted outputs first, then main inventory; never move the held backpack/item.
        candidates.sort(java.util.Comparator.comparingInt(slot -> priority(slot, outputs)));
        List<ItemStack> stored = new ArrayList<>();
        for (Slot slot : storage) stored.add(copy(slot.getStack()));
        for (Slot source : candidates) {
            int index = source.getSlotIndex();
            if (index == held || index < 0
                || index >= stock.length
                || stock[index] == null
                || !source.canTakeStack(player)) continue;
            Move move = new Move(source, stock[index]);
            int remaining = move.stack.stackSize;
            // Existing compatible stacks first, then empty slots. Commit only whole-stack moves.
            for (int pass = 0; pass < 2; pass++) for (int i = 0; i < storage.size() && remaining > 0; i++) {
                Slot target = storage.get(i);
                ItemStack before = stored.get(i);
                if ((before == null) != (pass == 1) || before != null && !matches(before, move.stack)
                    || !target.isItemValid(move.stack)
                    || !target.canTakeStack(player)) continue;
                int count = Math.min(
                    remaining,
                    Math.max(
                        0,
                        Math.min(target.getSlotStackLimit(), move.stack.getMaxStackSize())
                            - (before == null ? 0 : before.stackSize)));
                if (count > 0) {
                    move.placements.add(new Placement(target, before, count));
                    remaining -= count;
                }
            }
            if (remaining != 0) continue;
            for (Placement place : move.placements) {
                ItemStack after = copy(move.stack);
                after.stackSize = (place.before == null ? 0 : place.before.stackSize) + place.count;
                stored.set(storage.indexOf(place.slot), after);
            }
            stock[index] = null;
            result.moves.add(move);
            result.batches = capacity(stock, outputs, maximum, returnedSlots);
            if (result.batches >= maximum) break;
        }
        if (result.batches == 0) result.moves.clear();
        return result;
    }

    private static int priority(Slot slot, List<BookmarkItem> outputs) {
        ItemStack item = slot.getStack();
        for (BookmarkItem output : outputs) if (matches(item, output.itemStack)) return 0;
        return slot.getSlotIndex() >= 9 ? 1 : 2;
    }

    private static long capacity(ItemStack[] inventory, List<BookmarkItem> outputs, long maximum, int returnedSlots) {
        for (ItemStack item : inventory)
            if (item == null) return CraftingAvailability.outputCapacity(inventory, outputs, maximum, returnedSlots);
        return 0;
    }

    private static ItemStack copy(ItemStack item) {
        return item == null ? null : item.copy();
    }

    private static boolean matches(ItemStack a, ItemStack b) {
        return a != null && b != null && a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    private static boolean same(ItemStack a, ItemStack b) {
        return ItemStack.areItemStacksEqual(a, b);
    }
}
