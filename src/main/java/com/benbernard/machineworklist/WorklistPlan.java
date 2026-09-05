package com.benbernard.machineworklist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import codechicken.nei.bookmark.BookmarkGrid;
import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.recipe.Recipe.RecipeId;
import codechicken.nei.recipe.RecipeHandlerRef;
import codechicken.nei.recipe.StackInfo;
import codechicken.nei.recipe.chain.RecipeChainMath;

/** A detached snapshot: calculation never edits the user's NEI bookmarks or real inventory. */
public final class WorklistPlan {

    public final int groupId;
    private final List<BookmarkItem> source;
    public final List<BookmarkItem> missingMaterials = new ArrayList<>();

    WorklistPlan(int groupId, List<BookmarkItem> source) {
        this.groupId = groupId;
        this.source = new ArrayList<>();
        for (BookmarkItem item : source) {
            this.source.add(new MatchingBookmarkItem(item));
        }
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

    RecipeChainMath remainingChain(ItemStack[] inventory) {
        // Collapsing is a display preference, not permission to stop traversing a chain.
        RecipeChainMath math = RecipeChainMath.of(source, Collections.emptySet());
        // Plain bookmarks are not proof of ownership. Supply comes only from actual inventory.
        math.initialItems.clear();
        Map<BookmarkItem, BookmarkItem> supplies = new LinkedHashMap<>();
        for (ItemStack stack : inventory) {
            if (stack == null || stack.stackSize <= 0) continue;
            BookmarkItem item = new MatchingBookmarkItem(BookmarkItem.of(groupId, stack.copy()));
            BookmarkItem existing = supplies.get(item);
            if (existing == null) supplies.put(item, item);
            else existing.amount = Math.addExact(existing.amount, item.amount);
        }
        math.initialItems.addAll(supplies.values());
        // NEI's tool/container recycling can treat drained fluid containers as reusable supply.
        // Account in mB using inert, distinct internal tokens; retain NEI's fluid permutations.
        // Tokens never leave the calculation and never enter the player's inventory.
        Map<BookmarkItem, ItemStack> originals = new IdentityHashMap<>();
        normalizeFluids(math.initialItems, originals);
        normalizeFluids(math.recipeIngredients, originals);
        normalizeFluids(math.recipeResults, originals);
        // Different container items may normalize to the same fluid accounting identity.
        Map<BookmarkItem, BookmarkItem> normalizedSupplies = new LinkedHashMap<>();
        for (BookmarkItem item : math.initialItems) {
            BookmarkItem existing = normalizedSupplies.get(item);
            if (existing == null) normalizedSupplies.put(item, item);
            else existing.amount = Math.addExact(existing.amount, item.amount);
        }
        math.initialItems.clear();
        math.initialItems.addAll(normalizedSupplies.values());
        boolean paused = codechicken.nei.recipe.StackInfo.isPausedItemDamageSound();
        try {
            math.refresh();
        } finally {
            codechicken.nei.recipe.StackInfo.pauseItemDamageSound(paused);
            originals.forEach((item, original) -> item.itemStack = original);
        }
        return math;
    }

    private static void normalizeFluids(List<BookmarkItem> items, Map<BookmarkItem, ItemStack> originals) {
        for (BookmarkItem item : items) {
            FluidStack fluid = StackInfo.getFluid(item.itemStack);
            if (fluid == null) continue;
            originals.put(item, item.itemStack);
            ItemStack token = new ItemStack(Items.paper);
            NBTTagCompound identity = new NBTTagCompound();
            identity.setString(
                "machineworklistFluid",
                fluid.getFluid()
                    .getName());
            if (fluid.tag != null) identity.setTag("fluidTag", fluid.tag.copy());
            token.setTagCompound(identity);
            item.itemStack = token;
        }
    }

    public List<Step> calculate(ItemStack[] inventory) {
        RecipeChainMath math = remainingChain(inventory);
        missingMaterials.clear();
        missingMaterials.addAll(missingInputs(math, inventory));
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
            if (producer == null && input.factor > 0) {
                for (BookmarkItem candidate : math.recipeResults) {
                    if (candidate.factor > 0 && candidate.containsItems(input)) {
                        step.notes.add(
                            "External supply needed for " + input.itemStack.getDisplayName()
                                + "; NEI left a matching recipe unlinked (cycle or competing recipe).");
                        break;
                    }
                }
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
                    .filter(index -> matchesInput(input, stockItems.get(index)))
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

    List<BookmarkItem> missingInputs(RecipeChainMath math, ItemStack[] inventory) {
        java.util.Set<RecipeId> active = new java.util.HashSet<>();
        for (BookmarkItem output : math.recipeResults)
            if (output.factor > 0 && output.amount > 0) active.add(output.recipeId);
        Map<String, BookmarkItem> shortages = new LinkedHashMap<>();
        for (BookmarkItem input : math.recipeIngredients) {
            if (!active.contains(input.recipeId)) continue;
            if (input.factor == 0) {
                boolean owned = false;
                for (ItemStack stack : inventory) {
                    if (stack != null && stack.stackSize > 0
                        && matchesInput(input, BookmarkItem.of(groupId, stack.copy()))) {
                        owned = true;
                        break;
                    }
                }
                if (!owned) shortages
                    .putIfAbsent("tool:" + StackInfo.getItemStackGUID(input.itemStack), input.copyWithAmount(1));
                continue;
            }
            if (math.preferredItems.containsKey(input)) continue;
            long missing = math.requiredAmount.getOrDefault(input, 0L);
            if (missing <= 0) continue;
            String key = codechicken.nei.recipe.StackInfo.getItemStackGUID(input.itemStack);
            BookmarkItem existing = shortages.get(key);
            if (existing == null) shortages.put(key, input.copyWithAmount(missing));
            else existing.amount = Math.addExact(existing.amount, missing);
        }
        return new ArrayList<>(shortages.values());
    }

    static boolean matchesInput(BookmarkItem requirement, BookmarkItem supply) {
        return MatchingBookmarkItem.matches(requirement, supply);
    }

    public static final class Step {

        public final RecipeId id;
        public final String machine;
        public final boolean crafting;
        public final boolean recipeAvailable;
        public final List<BookmarkItem> inputs = new ArrayList<>();
        public final List<BookmarkItem> outputs = new ArrayList<>();
        public final List<RecipeId> dependencies = new ArrayList<>();
        public final List<String> notes = new ArrayList<>();
        public long runs;
        public long readyRuns;

        private Step(RecipeId id) {
            this.id = id;
            RecipeHandlerRef reference = RecipeHandlerRef.of(id);
            recipeAvailable = reference != null;
            machine = reference == null ? id.getHandleName() : reference.handler.getRecipeName();
            String handler = reference == null ? ""
                : reference.handler.getClass()
                    .getSimpleName();
            crafting = handler.equals("ShapedRecipeHandler") || handler.equals("ShapelessRecipeHandler");
            if (reference != null) {
                noteChance(reference.handler.getResultStack(reference.recipeIndex));
                for (codechicken.nei.PositionedStack output : reference.handler.getOtherStacks(reference.recipeIndex))
                    noteChance(output);
            }
        }

        private void noteChance(codechicken.nei.PositionedStack stack) {
            if (stack == null || !stack.getClass()
                .getName()
                .equals("gregtech.nei.GTNEIDefaultHandler$FixedPositionedStack")) return;
            // This field is part of the exact GTNH 2.8.4 handler. Keep GT classes optional
            // for the calculation tests and avoid loading the machine registry here.
            try {
                int chance = stack.getClass()
                    .getField("mChance")
                    .getInt(stack);
                if (chance > 0 && chance < 10000) notes.add(
                    "Chance output: " + stack.item.getDisplayName()
                        + " ("
                        + (chance / 100.0)
                        + "%). Counts assume successful outputs; repeats may be needed.");
            } catch (ReflectiveOperationException unavailable) {
                notes.add("Output probability unavailable; check this recipe in NEI.");
            }
        }
    }
}
