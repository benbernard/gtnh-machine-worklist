package com.benbernard.machineworklist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;

import codechicken.nei.recipe.Recipe.RecipeId;
import codechicken.nei.recipe.RecipeHandlerRef;

/** One checked batch per client tick; closing the container cancels further work. */
final class CraftingSession {

    private static CraftingSession active;
    private final WorklistScreen owner;
    private final GuiContainer container;
    private final WorklistPlan plan;
    private final RecipeId recipe;
    private final long requested;
    private long completed;
    private int delay = 2;

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
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.isDead || mc.thePlayer.getHealth() <= 0) {
            active = null;
            return;
        }
        if (mc.currentScreen != container || mc.thePlayer.openContainer != container.inventorySlots) {
            finish("Stopped because the crafting container changed or closed.", false);
            return;
        }
        if (--delay > 0) return;
        delay = 2;
        if (completed >= requested) {
            finish("Request complete.", true);
            return;
        }
        try {
            WorklistPlan.Step current = null;
            for (WorklistPlan.Step step : plan.calculate(CraftingInventory.snapshot(container)))
                if (step.id.equals(recipe)) current = step;
            if (current == null) {
                finish("No work remains for this recipe.", true);
                return;
            }
            CraftingAvailability available = CraftingAvailability.inspect(container, current);
            if (!available.reasons.isEmpty()) {
                finish(available.reasons.get(0), true);
                return;
            }
            long before = visibleOutput(current);
            if (!CraftingInventory.craft(RecipeHandlerRef.of(recipe), container)) {
                finish("NEI could not complete the next batch. Check the grid and recipe inputs.", true);
                return;
            }
            if (visibleOutput(current) - before != current.outputs.get(0).factor) {
                finish("The output change did not match one batch. Check the inventory before retrying.", true);
                return;
            }
            completed++;
            mc.ingameGUI.func_110326_a("Crafting " + completed + "/" + requested + " batches. Esc: stop.", false);
        } catch (RuntimeException failure) {
            finish(
                "Crafting stopped. Check the container before retrying: " + failure.getClass()
                    .getSimpleName(),
                true);
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
