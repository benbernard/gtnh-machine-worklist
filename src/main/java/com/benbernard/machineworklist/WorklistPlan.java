package com.benbernard.machineworklist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import codechicken.nei.bookmark.BookmarkGrid;
import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.recipe.Recipe.RecipeId;
import codechicken.nei.recipe.RecipeHandlerRef;
import codechicken.nei.recipe.chain.RecipeChainMath;

/** A detached snapshot: calculation never edits the user's NEI bookmarks or real inventory. */
public final class WorklistPlan {

    public final int groupId;
    private final List<BookmarkItem> source;

    private WorklistPlan(int groupId, List<BookmarkItem> source) {
        this.groupId = groupId;
        this.source = source;
    }

    public static WorklistPlan capture(BookmarkGrid grid, int groupId) {
        List<BookmarkItem> source = new ArrayList<>();
        grid.createChainItems(groupId)
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(
                entry -> source.add(
                    entry.getValue()
                        .copy()));
        return new WorklistPlan(groupId, source);
    }

    public List<Step> calculate(ItemStack[] inventory) {
        // Collapsing is a display preference, not permission to stop traversing a chain.
        RecipeChainMath math = RecipeChainMath.of(source, Collections.emptySet());
        // Plain bookmarks are not proof of ownership. Supply comes only from actual inventory.
        math.initialItems.clear();
        for (ItemStack stack : inventory) {
            if (stack != null && stack.stackSize > 0) math.initialItems.add(BookmarkItem.of(groupId, stack.copy()));
        }
        math.refresh();
        Map<RecipeId, Step> steps = new LinkedHashMap<>();
        for (BookmarkItem result : math.recipeResults) {
            if (result.factor <= 0 || result.amount <= 0) continue;
            Step step = steps.computeIfAbsent(result.recipeId, Step::new);
            step.runs = Math.max(step.runs, result.amount / result.factor);
            step.outputs.add(result);
        }
        for (BookmarkItem input : math.recipeIngredients) {
            Step step = steps.get(input.recipeId);
            if (step == null) continue;
            step.inputs.add(input);
            BookmarkItem producer = math.preferredItems.get(input);
            if (producer != null && steps.containsKey(producer.recipeId) && !producer.recipeId.equals(step.id)) {
                if (!step.dependencies.contains(producer.recipeId)) step.dependencies.add(producer.recipeId);
            }
        }
        List<Step> result = new ArrayList<>(steps.values());
        List<BookmarkItem> stockItems = new ArrayList<>();
        for (ItemStack stack : inventory) {
            if (stack != null && stack.stackSize > 0) stockItems.add(BookmarkItem.of(groupId, stack.copy()));
        }
        long[] stock = stockItems.stream()
            .mapToLong(item -> item.amount)
            .toArray();
        for (Step step : result) {
            List<BatchReadiness.Ingredient> requirements = new ArrayList<>();
            for (BookmarkItem input : step.inputs) {
                int[] matches = java.util.stream.IntStream.range(0, stockItems.size())
                    .filter(index -> input.containsItems(stockItems.get(index)))
                    .toArray();
                requirements.add(new BatchReadiness.Ingredient(Math.max(1, input.factor), input.factor == 0, matches));
            }
            step.readyRuns = BatchReadiness.maximumRuns(step.runs, stock, requirements);
        }
        result.sort(
            Comparator.comparing((Step step) -> step.readyRuns == 0)
                .thenComparing(step -> step.machine));
        return result;
    }

    public static final class Step {

        public final RecipeId id;
        public final String machine;
        public final boolean crafting;
        public final List<BookmarkItem> inputs = new ArrayList<>();
        public final List<BookmarkItem> outputs = new ArrayList<>();
        public final List<RecipeId> dependencies = new ArrayList<>();
        public long runs;
        public long readyRuns;

        private Step(RecipeId id) {
            this.id = id;
            RecipeHandlerRef reference = RecipeHandlerRef.of(id);
            machine = reference == null ? id.getHandleName() : reference.handler.getRecipeName();
            String handler = reference == null ? ""
                : reference.handler.getClass()
                    .getSimpleName();
            crafting = handler.equals("ShapedRecipeHandler") || handler.equals("ShapelessRecipeHandler");
        }
    }
}
