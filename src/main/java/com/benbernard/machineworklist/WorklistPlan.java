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
    private Map<RecipeId, Long> requestedRoots;
    final Map<String, Long> completed = new LinkedHashMap<>();
    String progressWarning;
    private java.nio.file.Path progressFile;
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
        WorklistPlan plan = new WorklistPlan(groupId, source);
        try {
            plan.loadProgress();
        } catch (RuntimeException exception) {
            plan.completed.clear();
            plan.progressWarning = exception.getMessage();
        }
        return plan;
    }

    static WorklistPlan example(ItemStack target, int batches) {
        for (codechicken.nei.recipe.ICraftingHandler handler : codechicken.nei.recipe.GuiCraftingRecipe
            .getCraftingHandlers("item", target)) {
            String name = handler.getClass()
                .getSimpleName();
            if (!name.equals("ShapedRecipeHandler") && !name.equals("ShapelessRecipeHandler")) continue;
            for (int index = 0; index < handler.numRecipes(); index++) {
                codechicken.nei.recipe.Recipe recipe = codechicken.nei.recipe.Recipe.of(handler, index);
                if (recipe.getResult() == null || !recipe.getResult()
                    .isItemEqual(target)) continue;
                List<BookmarkItem> items = new ArrayList<>();
                for (codechicken.nei.recipe.Recipe.RecipeIngredient input : recipe.getIngredients()) items.add(
                    BookmarkItem
                        .of(
                            -1,
                            input.getItemStack(),
                            input.getAmount(),
                            recipe.getRecipeId(),
                            BookmarkItem.BookmarkItemType.INGREDIENT)
                        .copyWithAmount((long) input.getAmount() * batches));
                for (codechicken.nei.recipe.Recipe.RecipeIngredient output : recipe.getResults()) items.add(
                    BookmarkItem
                        .of(
                            -1,
                            output.getItemStack(),
                            output.getAmount(),
                            recipe.getRecipeId(),
                            BookmarkItem.BookmarkItemType.RESULT)
                        .copyWithAmount((long) output.getAmount() * batches));
                WorklistPlan plan = new WorklistPlan(-1, items);
                plan.loadProgress();
                return plan;
            }
        }
        throw new IllegalStateException("No matching crafting recipe found.");
    }

    static String outputKey(BookmarkItem item) {
        FluidStack fluid = StackInfo.getFluid(item.itemStack);
        return fluid == null ? StackInfo.getItemStackGUID(item.itemStack)
            : "fluid:" + fluid.getFluid()
                .getName() + ":" + fluid.tag;
    }

    List<BookmarkItem> progressOutputs() {
        Map<String, BookmarkItem> outputs = new LinkedHashMap<>();
        for (BookmarkItem item : source) if (item.type == BookmarkItem.BookmarkItemType.RESULT && item.factor > 0)
            outputs.putIfAbsent(outputKey(item), item);
        return new ArrayList<>(outputs.values());
    }

    long completed(BookmarkItem output) {
        return completed.getOrDefault(outputKey(output), 0L);
    }

    long completeHalf(BookmarkItem output, ItemStack[] inventory) {
        long remaining = 0;
        for (BookmarkItem result : remainingChain(inventory).recipeResults)
            if (outputKey(result).equals(outputKey(output))) remaining = Math.addExact(remaining, result.amount);
        long batches = remaining / output.factor + (remaining % output.factor == 0 ? 0 : 1);
        long half = Math.multiplyExact(batches / 2 + batches % 2, output.factor);
        long visible = 0;
        for (ItemStack stack : inventory) if (stack != null && stack.stackSize > 0) {
            BookmarkItem stock = BookmarkItem.of(groupId, stack);
            if (outputKey(stock).equals(outputKey(output))) visible = Math.addExact(visible, stock.amount);
        }
        long amount = Math.addExact(Math.max(visible, completed(output)), half);
        setCompleted(output, amount);
        return amount;
    }

    void setCompleted(BookmarkItem output, long amount) {
        if (amount < 0) throw new IllegalArgumentException("Enter a non-negative whole quantity.");
        String key = outputKey(output);
        Long previous = completed.get(key);
        if (amount == 0) completed.remove(key);
        else completed.put(key, amount);
        try {
            saveProgress();
            progressWarning = null;
        } catch (RuntimeException exception) {
            if (previous == null) completed.remove(key);
            else completed.put(key, previous);
            throw exception;
        }
    }

    /** Maintain existing total-stock records only for observed worklist crafting changes. */
    void recordCraftedInventory(ItemStack[] before, ItemStack[] after) {
        if (completed.isEmpty()) return;
        Map<String, Long> old = new LinkedHashMap<>(completed);
        Map<String, Long> previous = inventoryTotals(before), current = inventoryTotals(after);
        try {
            for (Map.Entry<String, Long> entry : old.entrySet()) {
                String key = entry.getKey();
                long visible = previous.getOrDefault(key, 0L);
                long delta = current.getOrDefault(key, 0L) - visible;
                if (delta == 0) continue;
                long amount = Math.addExact(Math.max(entry.getValue(), visible), delta);
                if (amount == 0) completed.remove(key);
                else completed.put(key, amount);
            }
            if (!completed.equals(old)) saveProgress();
        } catch (RuntimeException failure) {
            completed.clear();
            completed.putAll(old);
            throw failure;
        }
    }

    private static Map<String, Long> inventoryTotals(ItemStack[] inventory) {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (ItemStack stack : inventory) if (stack != null) {
            BookmarkItem item = BookmarkItem.of(0, stack);
            totals.merge(outputKey(item), item.amount, Math::addExact);
        }
        return totals;
    }

    private void loadProgress() {
        String world = codechicken.nei.NEIClientConfig.getWorldPath();
        if (world == null) return;
        progressFile = net.minecraft.client.Minecraft.getMinecraft().mcDataDir.toPath()
            .resolve("config/machineworklist/progress")
            .resolve(identity(world) + ".properties");
        loadProgress(progressFile);
    }

    String snapshotKey() {
        return identity("");
    }

    private String identity(String world) {
        // A changed group gets a new record. World/server names are hashed, never written into the file.
        StringBuilder identity = new StringBuilder(world).append(':')
            .append(groupId);
        for (BookmarkItem item : source) identity.append('|')
            .append(outputKey(item))
            .append(':')
            .append(item.amount)
            .append(':')
            .append(item.factor)
            .append(':')
            .append(item.type)
            .append(':')
            .append(
                item.recipeId == null ? ""
                    : item.recipeId.toJsonObject()
                        .toString());
        return java.util.UUID.nameUUIDFromBytes(
            identity.toString()
                .getBytes(java.nio.charset.StandardCharsets.UTF_8))
            .toString();
    }

    void loadProgress(java.nio.file.Path file) {
        progressFile = file;
        completed.clear();
        if (!java.nio.file.Files.exists(progressFile)) return;
        java.util.Properties values = new java.util.Properties();
        try (java.io.InputStream input = java.nio.file.Files.newInputStream(progressFile)) {
            values.load(input);
            for (BookmarkItem output : progressOutputs()) {
                String key = outputKey(output);
                long amount = Long.parseLong(values.getProperty(key, "0"));
                if (amount < 0) throw new IllegalArgumentException("Negative saved completion quantity");
                if (amount > 0) completed.put(key, amount);
            }
        } catch (java.io.IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not read manual progress: " + exception.getMessage(), exception);
        }
    }

    private void saveProgress() {
        if (progressFile == null) return;
        java.util.Properties values = new java.util.Properties();
        completed.forEach((key, amount) -> values.setProperty(key, amount.toString()));
        try {
            java.nio.file.Files.createDirectories(progressFile.getParent());
            java.nio.file.Path temporary = java.nio.file.Files
                .createTempFile(progressFile.getParent(), "progress-", ".tmp");
            try {
                try (java.io.OutputStream output = java.nio.file.Files.newOutputStream(temporary)) {
                    values.store(
                        output,
                        "Completed outputs still available for this crafting group; includes inventory copies.");
                }
                java.nio.file.Files.move(temporary, progressFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } finally {
                java.nio.file.Files.deleteIfExists(temporary);
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save manual progress: " + exception.getMessage(), exception);
        }
    }

    RecipeChainMath remainingChain(ItemStack[] inventory) {
        // Collapsing is a display preference, not permission to stop traversing a chain.
        RecipeChainMath math = RecipeChainMath.of(source, Collections.emptySet());
        if (requestedRoots != null) {
            math.outputRecipes.clear();
            math.outputRecipes.putAll(requestedRoots);
        }
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
        // Manual counts include inventory copies: add only the amount not already visible.
        // Keep these separate until fluid normalization so cells and buckets share mB accounting.
        for (BookmarkItem output : progressOutputs()) {
            long declared = completed(output);
            if (declared == 0) continue;
            long visible = 0;
            for (BookmarkItem supply : supplies.values())
                if (outputKey(output).equals(outputKey(supply))) visible = Math.addExact(visible, supply.amount);
            if (declared > visible) {
                BookmarkItem credit = new MatchingBookmarkItem(output).copyWithAmount(declared - visible);
                credit.type = BookmarkItem.BookmarkItemType.ITEM;
                credit.recipeId = null;
                math.initialItems.add(credit);
            }
        }
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

    /** Freeze the selected recipe's remaining demand and existing NEI dependency choices. */
    WorklistPlan chainRequest(RecipeId target, ItemStack[] inventory) {
        WorklistPlan request;
        if (target == null) {
            request = new WorklistPlan(groupId, source);
        } else {
            RecipeChainMath current = remainingChain(inventory);
            java.util.Set<RecipeId> included = new java.util.LinkedHashSet<>();
            included.add(target);
            boolean changed;
            do {
                changed = false;
                for (Map.Entry<BookmarkItem, BookmarkItem> link : current.preferredItems.entrySet())
                    if (included.contains(link.getKey().recipeId)) changed |= included.add(link.getValue().recipeId);
            } while (changed);
            List<BookmarkItem> selected = new ArrayList<>();
            for (BookmarkItem item : source) if (included.contains(item.recipeId)) selected.add(item);
            request = new WorklistPlan(groupId, selected);
            long remaining = 0, creditedBatches = Long.MAX_VALUE;
            for (BookmarkItem output : current.recipeResults) if (target.equals(output.recipeId) && output.factor > 0) {
                remaining = Math.max(remaining, output.amount / output.factor);
                long credit = 0;
                for (BookmarkItem stock : current.initialItems)
                    if (outputKey(output).equals(outputKey(stock))) credit = Math.addExact(credit, stock.amount);
                creditedBatches = Math.min(creditedBatches, credit / output.factor);
            }
            request.requestedRoots = Collections.singletonMap(
                target,
                Math.addExact(remaining, creditedBatches == Long.MAX_VALUE ? 0 : creditedBatches));
        }
        request.completed.putAll(completed);
        return request;
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
            machine = reference == null ? "Unavailable recipe" : reference.handler.getRecipeName();
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
