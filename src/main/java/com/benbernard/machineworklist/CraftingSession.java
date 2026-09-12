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
    private long completed;

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

    private void finish(String reason, boolean restore) {
        active = null;
        String message = "Crafted " + completed + " of " + requested + " requested batches. " + reason;
        if (restore) owner.craftingFinished(message);
        else Minecraft.getMinecraft().ingameGUI.func_110326_a(message, false);
    }

    private void advance() {
        CraftingBurst.run(this::advanceBatch, System::nanoTime);
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
        if (completed >= requested) {
            finish("Request complete.", true);
            return false;
        }
        try {
            WorklistPlan.Step current = null;
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
            int batches = (int) Math.min(maximumBatches, Math.min(requested - completed, available.batches));
            if (batches < 1) {
                finish("No more batches are ready. Check the inputs and available output space.", true);
                return false;
            }
            long before = visibleOutput(current);
            boolean crafted = CraftingInventory.craft(RecipeHandlerRef.of(recipe), container, batches);
            int verified = CraftingBurst
                .verifiedBatches(before, visibleOutput(current), current.outputs.get(0).factor, batches);
            if (verified < 0) {
                finish(
                    "The output change did not match the requested batches. Check the inventory before retrying.",
                    true);
                return false;
            }
            completed += verified;
            if (!crafted || verified != batches) {
                finish("NEI stopped before completing the transfer. Check the grid, inputs and output space.", true);
                return false;
            }
            if (completed >= requested) {
                finish("Request complete.", true);
                return false;
            }
            mc.ingameGUI.func_110326_a("Crafting " + completed + "/" + requested + " batches. Esc: stop.", false);
            return true;
        } catch (RuntimeException failure) {
            finish(
                "Crafting stopped. Check the container before retrying: " + failure.getClass()
                    .getSimpleName(),
                true);
            return false;
        }
    }

    private long visibleOutput(WorklistPlan.Step step) {
        String key = WorklistPlan.outputKey(step.outputs.get(0));
        long count = 0;
        for (net.minecraft.item.ItemStack stack : CraftingInventory.snapshot(container)) if (stack != null) {
            codechicken.nei.bookmark.BookmarkItem item = codechicken.nei.bookmark.BookmarkItem.of(0, stack);
            if (WorklistPlan.outputKey(item)
                .equals(key)) count += item.amount;
        }
        return count;
    }
}
