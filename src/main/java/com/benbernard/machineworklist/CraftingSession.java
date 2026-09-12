package com.benbernard.machineworklist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;

import codechicken.nei.recipe.Recipe.RecipeId;
import codechicken.nei.recipe.RecipeHandlerRef;

/** Checked NEI bulk transfers in short client-tick bursts; closing the container cancels further work. */
final class CraftingSession {

    private static CraftingSession active;
    private final WorklistScreen owner;
    private final GuiContainer container;
    private final WorklistPlan plan;
    private final RecipeId recipe;
    private final long requested;
    private CraftingChain chain;
    private long completed;
    private PendingTransfer pending;

    private static final class PendingTransfer {

        final WorklistPlan.Step step;
        final net.minecraft.item.ItemStack[] before;
        final int batches;
        final boolean crafted;
        final CraftingReturns returns;
        final CraftingSettlement settlement;
        net.minecraft.item.ItemStack[] observed;

        PendingTransfer(WorklistPlan.Step step, net.minecraft.item.ItemStack[] before, int batches, boolean crafted,
            CraftingReturns returns, int ping) {
            this.step = step;
            this.before = before;
            this.batches = batches;
            this.crafted = crafted;
            this.returns = returns;
            settlement = new CraftingSettlement(ping);
        }
    }

    private CraftingSession(WorklistScreen owner, GuiContainer container, WorklistPlan plan, RecipeId recipe,
        long count) {
        this.owner = owner;
        this.container = container;
        this.plan = plan;
        this.recipe = recipe;
        requested = count;
    }

    static boolean running() {
        return active != null;
    }

    static void start(WorklistScreen owner, GuiContainer container, WorklistPlan plan, RecipeId recipe, long count) {
        if (active != null || count < 1) return;
        active = new CraftingSession(owner, container, plan, recipe, count);
        Minecraft.getMinecraft()
            .displayGuiScreen(container);
    }

    static void tick() {
        if (active != null) active.advance();
    }

    static void startChain(WorklistScreen owner, GuiContainer container, CraftingChain chain) {
        if (active != null) return;
        active = new CraftingSession(owner, container, chain.plan, null, 0);
        active.chain = chain;
        Minecraft.getMinecraft()
            .displayGuiScreen(container);
    }

    private void finish(String reason, boolean restore) {
        active = null;
        String message = chain == null
            ? "Crafted " + completed
                + " of "
                + requested
                + (requested == 1 ? " requested batch. " : " requested batches. ")
                + reason
            : "Crafted " + chain.completed
                + (chain.completed == 1 ? " batch across " : " batches across ")
                + chain.craftedRecipes()
                + (chain.craftedRecipes() == 1 ? " recipe. " : " recipes. ")
                + reason;
        if (restore && !EntryFeedback.allow(container)) {
            // A failed transfer may leave a cursor/grid stack. Changing GUIs can drop it.
            Minecraft.getMinecraft().thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(message));
            Minecraft.getMinecraft().ingameGUI
                .func_110326_a("Crafting stopped. Clear the cursor/grid, then F10.", false);
        } else if (restore) owner.craftingFinished(message);
        else Minecraft.getMinecraft().ingameGUI.func_110326_a(message, false);
    }

    private void advance() {
        if (!validContainer()) return;
        try {
            if (pending != null) settleTransfer();
            else CraftingBurst.run(this::advanceBatch, System::nanoTime);
        } catch (RuntimeException failure) {
            finish(
                "Crafting stopped while returning tools. Inspect the grid/cursor: " + failure.getClass()
                    .getSimpleName(),
                true);
        }
    }

    private int pingMillis() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() != null)
            for (net.minecraft.client.gui.GuiPlayerInfo player : mc.getNetHandler().playerInfoList)
                if (player.name.equals(mc.thePlayer.getCommandSenderName())) return player.responseTime;
        return 100;
    }

    private boolean validContainer() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.isDead || mc.thePlayer.getHealth() <= 0) {
            active = null;
            return false;
        }
        if (mc.currentScreen != container || mc.thePlayer.openContainer != container.inventorySlots) {
            finish("Stopped because the crafting container changed or closed.", false);
            return false;
        }
        return true;
    }

    private void settleTransfer() {
        net.minecraft.item.ItemStack[] state = CraftingInventory.containerSnapshot(container);
        boolean changed = !CraftingInventory.sameSnapshot(pending.observed, state);
        pending.observed = state;
        boolean blocked = !EntryFeedback.reasons(container)
            .isEmpty();
        CraftingSettlement.Action action = pending.settlement
            .tick(changed, blocked, pending.returns.canRecover(container));
        if (action == CraftingSettlement.Action.STOP) {
            finish(
                "Tool return did not settle. Check the cursor, bottom-right crafting grid and inventory space; "
                    + "reopen the container if an item looks stuck. This transfer's output is unconfirmed; "
                    + "reopening recalculates actual stock.",
                true);
        } else if (action == CraftingSettlement.Action.RECOVER) {
            pending.returns.recover(container);
        } else if (action == CraftingSettlement.Action.READY) {
            PendingTransfer transfer = pending;
            pending = null;
            if (acceptTransfer(transfer, CraftingInventory.snapshot(container)))
                CraftingBurst.run(this::advanceBatch, System::nanoTime);
        } else {
            Minecraft.getMinecraft().ingameGUI
                .func_110326_a("Returning crafting tools; waiting for inventory updates. Esc: stop.", false);
        }
    }

    private boolean advanceBatch(int maximumBatches) {
        if (!validContainer()) return false;
        if (chain == null && completed >= requested) {
            finish("Request complete.", true);
            return false;
        }
        try {
            WorklistPlan.Step current = null;
            long availableBatches;
            if (chain != null) {
                CraftingChain.Selection selection = chain.inspect(
                    CraftingInventory.snapshot(container),
                    step -> CraftingAvailability.inspect(container, step));
                if (selection.next == null) {
                    finish(selection.stoppedReason(), true);
                    return false;
                }
                current = selection.next;
                availableBatches = selection.batches;
            } else {
                for (WorklistPlan.Step step : plan.calculate(CraftingInventory.snapshot(container)))
                    if (step.id.equals(recipe)) current = step;
                if (current == null) {
                    finish("No work remains for this recipe.", true);
                    return false;
                }
                CraftingAvailability available = CraftingAvailability.inspect(container, current);
                if (!available.reasons.isEmpty()) {
                    finish(available.reasons.get(0), true);
                    return false;
                }
                availableBatches = Math.min(requested - completed, available.batches);
            }
            int batches = (int) Math.min(maximumBatches, availableBatches);
            if (batches < 1) {
                finish("No more batches are ready. Check the inputs and available output space.", true);
                return false;
            }
            CraftingSpace.Plan space = CraftingSpace.plan(container, current, batches);
            batches = (int) Math.min(batches, space.batches);
            if (batches < 1 || !CraftingSpace.execute(space, container)) {
                finish("Storage changed or refused a transfer. Check the cursor and available storage space.", true);
                return false;
            }
            net.minecraft.item.ItemStack[] beforeInventory = CraftingInventory.snapshot(container);
            CraftingReturns returns = new CraftingReturns(current.inputs);
            boolean crafted = CraftingInventory.craft(RecipeHandlerRef.of(current.id), container, batches);
            PendingTransfer transfer = new PendingTransfer(
                current,
                beforeInventory,
                batches,
                crafted,
                returns,
                pingMillis());
            // Final and partially successful transfers need the same cleanup as intermediate ones.
            // Ordinary empty-grid recipes retain the existing fast burst path.
            if (returns.hasTools() || !EntryFeedback.reasons(container)
                .isEmpty()) {
                pending = transfer;
                return false;
            }
            return acceptTransfer(transfer, CraftingInventory.snapshot(container));
        } catch (RuntimeException failure) {
            finish(
                "Crafting stopped. Check the container before retrying: " + failure.getClass()
                    .getSimpleName(),
                true);
            return false;
        }
    }

    private boolean acceptTransfer(PendingTransfer transfer, net.minecraft.item.ItemStack[] afterInventory) {
        WorklistPlan.Step current = transfer.step;
        int verified = CraftingBurst.verifiedBatches(
            visibleOutput(current, transfer.before),
            visibleOutput(current, afterInventory),
            current.outputs.get(0).factor,
            transfer.batches);
        if (verified < 0) {
            finish("The output change did not match the requested batches. Check the inventory before retrying.", true);
            return false;
        }
        completed += verified;
        if (chain != null) chain.record(current.id, verified);
        if (verified > 0) owner.recordCraftedInventory(transfer.before, afterInventory, plan);
        if (!transfer.crafted || verified != transfer.batches) {
            finish("NEI stopped before completing the transfer. Check the grid, inputs and output space.", true);
            return false;
        }
        if (chain == null && completed >= requested) {
            finish("Request complete.", true);
            return false;
        }
        Minecraft.getMinecraft().ingameGUI.func_110326_a(
            chain == null ? "Crafting " + completed + "/" + requested + " batches. Esc: stop."
                : "Chain: " + chain.completed + " batches / " + chain.craftedRecipes() + " recipes. Esc: stop.",
            false);
        return true;
    }

    private long visibleOutput(WorklistPlan.Step step, net.minecraft.item.ItemStack[] inventory) {
        String key = WorklistPlan.outputKey(step.outputs.get(0));
        long count = 0;
        for (net.minecraft.item.ItemStack stack : inventory) if (stack != null) {
            codechicken.nei.bookmark.BookmarkItem item = codechicken.nei.bookmark.BookmarkItem.of(0, stack);
            if (WorklistPlan.outputKey(item)
                .equals(key)) count += item.amount;
        }
        return count;
    }
}
