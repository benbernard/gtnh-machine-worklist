package com.benbernard.machineworklist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.item.ItemStack;

import codechicken.nei.recipe.Recipe.RecipeId;

/** Recalculate from real stock after every transfer, within the original finite request. */
final class CraftingChain {

    final WorklistPlan plan;
    private final Map<RecipeId, Long> remainingBudget = new LinkedHashMap<>();
    private final Set<RecipeId> craftedRecipes = new LinkedHashSet<>();
    long completed;

    CraftingChain(WorklistPlan plan, ItemStack[] inventory) {
        this.plan = plan;
        for (WorklistPlan.Step step : plan.calculate(inventory))
            if (step.crafting) remainingBudget.put(step.id, step.runs);
    }

    Selection inspect(ItemStack[] inventory, Function<WorklistPlan.Step, CraftingAvailability> inspect) {
        List<WorklistPlan.Step> pending = plan.calculate(inventory);
        Selection result = new Selection();
        result.remainingRecipes = pending.size();
        for (WorklistPlan.Step step : pending) if (step.crafting) result.craftingRecipes++;
        for (WorklistPlan.Step step : consumersFirst(pending)) {
            if (!step.crafting) {
                result.reason("Manual operation remains: " + step.machine + " / " + name(step) + ".");
                continue;
            }
            long budget = remainingBudget.getOrDefault(step.id, 0L);
            if (budget == 0) {
                result.reason(
                    "The original execution limit was reached for " + name(step)
                        + ". Check cycles or changed stock before starting a new request.");
                continue;
            }
            CraftingAvailability availability = inspect.apply(step);
            if (availability.batches > 0) {
                result.next = step;
                result.batches = Math.min(budget, availability.batches);
                return result;
            }
            for (String reason : availability.reasons) result.reason(name(step) + ": " + reason);
        }
        return result;
    }

    void record(RecipeId recipe, int batches) {
        long budget = remainingBudget.getOrDefault(recipe, 0L);
        if (batches < 0 || batches > budget) throw new IllegalArgumentException("Transfer exceeds the chain request");
        remainingBudget.put(recipe, budget - batches);
        completed = Math.addExact(completed, batches);
        if (batches > 0) craftedRecipes.add(recipe);
    }

    int craftedRecipes() {
        return craftedRecipes.size();
    }

    static String name(WorklistPlan.Step step) {
        return step.outputs.get(0).itemStack.getDisplayName();
    }

    private static List<WorklistPlan.Step> consumersFirst(List<WorklistPlan.Step> steps) {
        Map<RecipeId, WorklistPlan.Step> remaining = new LinkedHashMap<>();
        Map<RecipeId, Integer> consumers = new LinkedHashMap<>();
        for (WorklistPlan.Step step : steps) {
            remaining.put(step.id, step);
            for (RecipeId dependency : step.dependencies) consumers.merge(dependency, 1, Integer::sum);
        }
        java.util.ArrayDeque<WorklistPlan.Step> ready = new java.util.ArrayDeque<>();
        for (WorklistPlan.Step step : steps) if (!consumers.containsKey(step.id)) ready.add(step);
        List<WorklistPlan.Step> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            WorklistPlan.Step step = ready.remove();
            remaining.remove(step.id);
            ordered.add(step);
            for (RecipeId dependency : step.dependencies)
                if (consumers.merge(dependency, -1, Integer::sum) == 0 && remaining.containsKey(dependency))
                    ready.add(remaining.get(dependency));
        }
        ordered.addAll(remaining.values());
        return ordered;
    }

    static final class Selection {

        WorklistPlan.Step next;
        long batches;
        int remainingRecipes;
        int craftingRecipes;
        final List<String> reasons = new ArrayList<>();

        private void reason(String reason) {
            if (reasons.size() < 8 && !reasons.contains(reason)) reasons.add(reason);
        }

        String stoppedReason() {
            if (remainingRecipes == 0) return "Chain complete.";
            return "Chain paused with " + remainingRecipes
                + " recipe operations remaining. "
                + (reasons.isEmpty() ? "No further transfers are ready." : String.join(" ", reasons));
        }
    }
}
