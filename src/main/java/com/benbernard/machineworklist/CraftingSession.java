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
    private int cleanupWait;

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
        CraftingBurst.run(this::advanceBatch, System::nanoTime);
    }

    private boolean waitForCleanup() {
        if (completed == 0 || EntryFeedback.reasons(container)
            .isEmpty()) return false;
        if (++cleanupWait > 10) finish(
            "The cursor or crafting grid did not clear after the transfer. Inspect its contents before retrying.",
            true);
        return true;
    }

    private boolean advanceBatch(int maximumBatches) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.isDead || mc.thePlayer.getHealth() <= 0) {
            active = null;
            return false;
        }
        if (mc.currentScreen != container || mc.thePlayer.openContainer != container.inventorySlots) {
            finish("Stopped because the crafting container changed or closed.", false);
            return false;
        }
        // Server updates can briefly expose a transfer's intermediate cursor/grid state.
        // Yield without clicks; persistent leftovers require the user's attention.
        if (waitForCleanup()) return false;
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
                    if (waitForCleanup()) return false;
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
                    if (waitForCleanup()) return false;
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
            net.minecraft.item.ItemStack[] beforeInventory = CraftingInventory.snapshot(container);
            long before = visibleOutput(current, beforeInventory);
            boolean crafted = CraftingInventory.craft(RecipeHandlerRef.of(current.id), container, batches);
            net.minecraft.item.ItemStack[] afterInventory = CraftingInventory.snapshot(container);
            int verified = CraftingBurst.verifiedBatches(
                before,
                visibleOutput(current, afterInventory),
                current.outputs.get(0).factor,
                batches);
            if (verified < 0) {
                finish(
                    "The output change did not match the requested batches. Check the inventory before retrying.",
                    true);
                return false;
            }
            completed += verified;
            if (chain != null) chain.record(current.id, verified);
            if (verified > 0) owner.recordCraftedInventory(beforeInventory, afterInventory, plan);
            cleanupWait = 0;
            if (!crafted || verified != batches) {
                finish("NEI stopped before completing the transfer. Check the grid, inputs and output space.", true);
                return false;
            }
            if (chain == null && completed >= requested) {
                finish("Request complete.", true);
                return false;
            }
            mc.ingameGUI.func_110326_a(
                chain == null ? "Crafting " + completed + "/" + requested + " batches. Esc: stop."
                    : "Chain: " + chain.completed + " batches / " + chain.craftedRecipes() + " recipes. Esc: stop.",
                false);
            return true;
        } catch (RuntimeException failure) {
            finish(
                "Crafting stopped. Check the container before retrying: " + failure.getClass()
                    .getSimpleName(),
                true);
            return false;
        }
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
