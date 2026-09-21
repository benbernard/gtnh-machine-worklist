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
    private final CraftingTransactions transactions;
    private final net.minecraft.client.entity.EntityClientPlayerMP player;
    private final net.minecraft.network.NetworkManager connection;
    private boolean waitingForInventory = true;
    private int confirmationTicks;
    private final long started = System.nanoTime();

    private static final class PendingTransfer {

        final WorklistPlan.Step step;
        final net.minecraft.item.ItemStack[] before;
        final int batches;
        final boolean crafted;

        PendingTransfer(WorklistPlan.Step step, net.minecraft.item.ItemStack[] before, int batches, boolean crafted) {
            this.step = step;
            this.before = before;
            this.batches = batches;
            this.crafted = crafted;
        }
    }

    private CraftingSession(WorklistScreen owner, GuiContainer container, WorklistPlan plan, RecipeId recipe,
        long count) {
        this.owner = owner;
        this.container = container;
        this.plan = plan;
        this.recipe = recipe;
        requested = count;
        player = Minecraft.getMinecraft().thePlayer;
        connection = player.sendQueue.getNetworkManager();
        transactions = new CraftingTransactions(connection, container.inventorySlots);
        // Refresh the real server window before planning the first transfer, including resumed worklists.
        try {
            transactions.begin();
            transactions.seal();
        } catch (RuntimeException failure) {
            transactions.close();
            throw failure;
        }
        CraftingDiagnostics.event(
            "Start " + (recipe == null ? "chain" : "recipe")
                + " in "
                + container.inventorySlots.getClass()
                    .getSimpleName());
    }

    static boolean running() {
        return active != null;
    }

    static boolean blocksInput(GuiContainer gui) {
        return active != null && active.container == gui;
    }

    private static boolean canStart(WorklistScreen owner, GuiContainer gui) {
        Minecraft mc = Minecraft.getMinecraft();
        return active == null && mc.currentScreen == owner
            && CraftingInventory.usableFrom(gui, mc.currentScreen, mc.thePlayer)
            && codechicken.nei.NEIClientConfig.hasSMPCounterPart()
            && !codechicken.nei.recipe.AutoCraftingManager.processing();
    }

    static void start(WorklistScreen owner, GuiContainer container, WorklistPlan plan, RecipeId recipe, long count) {
        if (count >= 1) open(owner, container, plan, recipe, count);
    }

    static void tick() {
        if (active != null) active.advance();
    }

    static void startChain(WorklistScreen owner, GuiContainer container, CraftingChain chain) {
        if (open(owner, container, chain.plan, null, 0)) active.chain = chain;
    }

    private static boolean open(WorklistScreen owner, GuiContainer container, WorklistPlan plan, RecipeId recipe,
        long count) {
        if (!canStart(owner, container)) return false;
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(container);
        if (mc.currentScreen != container || !CraftingInventory.usableFrom(container, container, mc.thePlayer))
            return false;
        try {
            active = new CraftingSession(owner, container, plan, recipe, count);
            return true;
        } catch (RuntimeException failure) {
            CraftingDiagnostics.event(
                "Could not start crafting: " + failure.getClass()
                    .getSimpleName());
            mc.thePlayer.addChatMessage(
                new net.minecraft.util.ChatComponentText(
                    "Could not start crafting. Reopen the container before retrying."));
            return false;
        }
    }

    private void finish(String reason) {
        active = null;
        transactions.close();
        CraftingDiagnostics.event(
            "Stop after " + completed
                + " verified batches / "
                + ((System.nanoTime() - started) / 1_000_000)
                + " ms: "
                + reason);
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
        // Like native NEI, leave the real container open. Never close it on a predicted empty cursor/grid.
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == player) player.addChatMessage(new net.minecraft.util.ChatComponentText(message));
        if (mc.ingameGUI != null) mc.ingameGUI.func_110326_a(message, false);
    }

    private void advance() {
        if (!validContainer()) return;
        try {
            if (waitingForInventory) settleTransfer();
            else CraftingBurst.run(this::advanceBatch, System::nanoTime);
        } catch (RuntimeException failure) {
            finish(
                "Crafting stopped while synchronizing inventory. Inspect the grid/cursor: " + failure.getClass()
                    .getSimpleName());
        }
    }

    private boolean validContainer() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != player || player.isDead
            || player.getHealth() <= 0
            || mc.getNetHandler() == null
            || mc.getNetHandler()
                .getNetworkManager() != connection) {
            active = null;
            transactions.close();
            return false;
        }
        if (mc.currentScreen != container || !CraftingInventory.usableFrom(container, mc.currentScreen, player)
            || codechicken.nei.recipe.AutoCraftingManager.processing()) {
            finish("Stopped because the crafting container changed or closed.");
            return false;
        }
        return true;
    }

    private void settleTransfer() {
        if (transactions.rejected()) {
            finish(
                "The server rejected an inventory transfer. Let the inventory refresh, then reopen the worklist "
                    + "to recalculate actual stock. This transfer's output is unconfirmed.");
            return;
        }
        if (!transactions.ready()) {
            if (++confirmationTicks > CraftingTransactions.MAX_TICKS) finish(
                "The server has not refreshed the inventory. Reopen the container before retrying. Output is unconfirmed.");
            return;
        }
        waitingForInventory = false;
        if (!EntryFeedback.reasons(container)
            .isEmpty()) {
            finish(
                "The refreshed inventory still has items on the cursor or crafting grid. Clear them manually, then press F10 to recalculate current stock.");
        } else {
            PendingTransfer transfer = pending;
            pending = null;
            if (transfer == null || acceptTransfer(transfer, CraftingInventory.snapshot(container)))
                CraftingBurst.run(this::advanceBatch, System::nanoTime);
        }
    }

    private boolean advanceBatch(int maximumBatches) {
        if (!validContainer()) return false;
        if (chain == null && completed >= requested) {
            finish("Request complete.");
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
                    finish(selection.stoppedReason());
                    return false;
                }
                current = selection.next;
                availableBatches = selection.batches;
            } else {
                for (WorklistPlan.Step step : plan.calculate(CraftingInventory.snapshot(container)))
                    if (step.id.equals(recipe)) current = step;
                if (current == null) {
                    finish("No work remains for this recipe.");
                    return false;
                }
                CraftingAvailability available = CraftingAvailability.inspect(container, current);
                if (!available.reasons.isEmpty()) {
                    finish(available.reasons.get(0));
                    return false;
                }
                availableBatches = Math.min(requested - completed, available.batches);
            }
            int batches = (int) Math.min(maximumBatches, availableBatches);
            if (batches < 1) {
                finish("No more batches are ready. Check the inputs and available output space.");
                return false;
            }
            CraftingSpace.Plan space = CraftingSpace.plan(container, current, batches);
            batches = (int) Math.min(batches, space.batches);
            if (batches < 1) {
                finish("Storage changed or refused a transfer. Check the cursor and available storage space.");
                return false;
            }
            transactions.begin();
            if (!space.moves.isEmpty()) {
                boolean moved = CraftingSpace.execute(space, container);
                transactions.seal();
                waitingForInventory = true;
                confirmationTicks = 0;
                if (!moved)
                    finish("Storage changed or refused a transfer. Inspect the real container before retrying.");
                // Never craft from a predicted storage move. Replan after the server's full refresh.
                return false;
            }
            net.minecraft.item.ItemStack[] beforeInventory = CraftingInventory.snapshot(container);
            boolean crafted = CraftingInventory.craft(RecipeHandlerRef.of(current.id), container, batches);
            transactions.seal();
            confirmationTicks = 0;
            CraftingDiagnostics.event(
                "Submitted " + batches
                    + " batches of "
                    + current.outputs.get(0).itemStack.getDisplayName()
                    + "; NEI success="
                    + crafted);
            PendingTransfer transfer = new PendingTransfer(current, beforeInventory, batches, crafted);
            // A clean prediction is not proof that the server has processed the bulk transfer.
            pending = transfer;
            waitingForInventory = true;
            return false;
        } catch (RuntimeException failure) {
            finish(
                "Crafting stopped. Check the container before retrying: " + failure.getClass()
                    .getSimpleName());
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
            finish("The output change did not match the requested batches. Check the inventory before retrying.");
            return false;
        }
        completed += verified;
        if (chain != null) chain.record(current.id, verified);
        if (verified > 0) owner.recordCraftedInventory(transfer.before, afterInventory, plan);
        if (!transfer.crafted || verified != transfer.batches) {
            finish("NEI stopped before completing the transfer. Check the grid, inputs and output space.");
            return false;
        }
        if (chain == null && completed >= requested) {
            finish("Request complete.");
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
