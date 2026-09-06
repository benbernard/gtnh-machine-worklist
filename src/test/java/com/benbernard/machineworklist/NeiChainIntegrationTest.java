package com.benbernard.machineworklist;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.bookmark.BookmarkItem.BookmarkItemType;
import codechicken.nei.recipe.Recipe.RecipeId;
import codechicken.nei.recipe.chain.RecipeChainMath;

/** Exercises the actual pinned NEI engine, using vanilla items as deterministic recipe fixtures. */
@RunWith(Parameterized.class)
public class NeiChainIntegrationTest {

    private final String fixtureName;

    public NeiChainIntegrationTest(String fixtureName) {
        this.fixtureName = fixtureName;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<String> fixtures() {
        return Arrays.asList(
            "existingIntermediatesReduceUpstreamMachineBatches",
            "finishedTargetEliminatesItsEntireChain",
            "sharedBranchesSpendInventoryOnce",
            "plainBookmarksDoNotInventStock",
            "repeatedCalculationsDoNotMutateInventoryOrTargets",
            "coproductsComeFromTheSameBatches",
            "multipleTargetsShareBatchSurplus",
            "fluidContainersUseMillibucketsAndPreserveRealInventory",
            "multipleInventoryStacksAreAllAvailable",
            "deepChainStopsAtOwnedIntermediate",
            "missingReusableToolsAreCountedOnceAndDisappearWhenOwned",
            "cyclicRecipesLeaveAFiniteExternalSeedRequirement",
            "reusableConfigurationMustMatchMetadataAndNbt",
            "consumedConfigurationCannotBeReplacedByUntaggedStock",
            "manualCompletionCutsUpstreamWithoutDoubleCountingInventory",
            "manualHalfCompletionRoundsBatchesAndCanBeUndone",
            "manualProgressSurvivesReloadAndCanBeCleared",
            "manualFluidProgressUsesMillibuckets",
            "manualStockIsSharedAcrossBranches",
            "backpackCleanupExcludesStorageAndSupplyExcludesMirrors");
    }

    @Test
    public void runInForgeClassLoader() throws Exception {
        List<java.net.URL> urls = new ArrayList<>();
        for (String path : System.getProperty("worklist.testClasspath")
            .split(java.io.File.pathSeparator)) {
            urls.add(
                new java.io.File(path).toURI()
                    .toURL());
        }
        try (net.minecraft.launchwrapper.LaunchClassLoader loader = new net.minecraft.launchwrapper.LaunchClassLoader(
            urls.toArray(new java.net.URL[0]))) {
            Class<?> sideClass = loader.loadClass("cpw.mods.fml.relauncher.Side");
            java.lang.reflect.Field side = loader.loadClass("cpw.mods.fml.relauncher.FMLRelaunchLog")
                .getDeclaredField("side");
            side.setAccessible(true);
            side.set(
                null,
                sideClass.getField("CLIENT")
                    .get(null));
            loader.registerTransformer("com.benbernard.machineworklist.NeiTestAccessTransformer");
            Class<?> forgeLoader = loader.loadClass("cpw.mods.fml.common.Loader");
            forgeLoader.getMethod("injectData", Object[].class)
                .invoke(
                    null,
                    new Object[] { new Object[] { "7", "99", "40", "1614", "1.7.10", "9.05",
                        new java.io.File("build/test-game"), Collections.emptyList() } });
            Class<?> fixture = loader.loadClass(getClass().getName());
            fixture.getMethod("executeFixture", String.class)
                .invoke(null, fixtureName);
        }
    }

    public static void executeFixture(String fixtureName) throws Exception {
        Bootstrap.func_151354_b();
        NeiChainIntegrationTest fixture = new NeiChainIntegrationTest(fixtureName);
        NeiChainIntegrationTest.class.getMethod(fixtureName)
            .invoke(fixture);
    }

    public void backpackCleanupExcludesStorageAndSupplyExcludesMirrors() {
        List<net.minecraft.inventory.Slot> slots = new ArrayList<>();
        BackpackCraftingOverlay overlay = new BackpackCraftingOverlay();
        for (int i = 0; i < 100; i++) {
            net.minecraft.inventory.Slot slot = new net.minecraft.inventory.Slot(null, i, 0, 0);
            slot.slotNumber = i;
            slots.add(slot);
            assertEquals(i < 84, overlay.canMoveFrom(slot, null));
        }
        java.util.Set<net.minecraft.inventory.Slot> cleanup = BackpackCraftingOverlay.craftingSlots(slots);
        assertEquals(9, cleanup.size());
        for (int i = 0; i < 100; i++) {
            assertEquals(
                Arrays.asList(65, 66, 67, 73, 74, 75, 81, 82, 83)
                    .contains(i),
                cleanup.contains(slots.get(i)));
        }
    }

    public void existingIntermediatesReduceUpstreamMachineBatches() {
        List<BookmarkItem> chain = new ArrayList<>();
        RecipeId wire = recipe(chain, "wiremill", Items.gold_ingot, 2, 1, Items.iron_ingot, 1);
        RecipeId component = recipe(chain, "assembler", Items.redstone, 1, 1, Items.gold_ingot, 3);
        RecipeId target = recipe(chain, "target", Items.diamond, 1, 2, Items.redstone, 2);
        RecipeChainMath math = new WorklistPlan(1, chain).remainingChain(
            new ItemStack[] { new ItemStack(Items.gold_ingot, 5), new ItemStack(Items.redstone, 1),
                new ItemStack(Items.iron_ingot, 64) });
        assertEquals(2, runs(math, wire));
        assertEquals(3, runs(math, component));
        assertEquals(2, runs(math, target));
    }

    public void finishedTargetEliminatesItsEntireChain() {
        List<BookmarkItem> chain = new ArrayList<>();
        RecipeId wire = recipe(chain, "wiremill", Items.gold_ingot, 2, 1, Items.iron_ingot, 1);
        RecipeId target = recipe(chain, "target", Items.diamond, 1, 2, Items.gold_ingot, 3);
        RecipeChainMath math = new WorklistPlan(1, chain)
            .remainingChain(new ItemStack[] { new ItemStack(Items.diamond, 2) });
        assertEquals(0, runs(math, wire));
        assertEquals(0, runs(math, target));
    }

    public void sharedBranchesSpendInventoryOnce() {
        List<BookmarkItem> chain = new ArrayList<>();
        RecipeId wire = recipe(chain, "wiremill", Items.gold_ingot, 2, 1, Items.iron_ingot, 1);
        RecipeId left = recipe(chain, "left", Items.diamond, 1, 1, Items.gold_ingot, 2);
        RecipeId right = recipe(chain, "right", Items.redstone, 1, 1, Items.gold_ingot, 3);
        recipe(chain, "target", Items.emerald, 1, 1, new ItemStack(Items.diamond, 2), new ItemStack(Items.redstone, 2));
        RecipeChainMath math = new WorklistPlan(1, chain)
            .remainingChain(new ItemStack[] { new ItemStack(Items.gold_ingot, 4) });
        assertEquals(3, runs(math, wire));
        assertEquals(2, runs(math, left));
        assertEquals(2, runs(math, right));
    }

    public void plainBookmarksDoNotInventStock() {
        List<BookmarkItem> chain = new ArrayList<>();
        RecipeId wire = recipe(chain, "wiremill", Items.gold_ingot, 2, 1, Items.iron_ingot, 1);
        recipe(chain, "target", Items.diamond, 1, 2, Items.gold_ingot, 3);
        chain.add(BookmarkItem.of(1, new ItemStack(Items.gold_ingot, 64)));
        assertEquals(3, runs(new WorklistPlan(1, chain).remainingChain(new ItemStack[0]), wire));
    }

    public void repeatedCalculationsDoNotMutateInventoryOrTargets() {
        List<BookmarkItem> chain = new ArrayList<>();
        RecipeId target = recipe(chain, "target", Items.diamond, 1, 10, Items.gold_ingot, 3);
        WorklistPlan plan = new WorklistPlan(1, chain);
        ItemStack[] inventory = { new ItemStack(Items.diamond, 3) };
        assertEquals(7, runs(plan.remainingChain(inventory), target));
        assertEquals(7, runs(plan.remainingChain(inventory), target));
        assertEquals(10, runs(plan.remainingChain(new ItemStack[0]), target));
        assertEquals(3, inventory[0].stackSize);
        assertEquals(10, chain.get(0).amount);
    }

    public void coproductsComeFromTheSameBatches() {
        List<BookmarkItem> chain = new ArrayList<>();
        RecipeId separator = recipe(chain, "separator", Items.gold_ingot, 2, 1, Items.iron_ingot, 1);
        ItemStack byproduct = new ItemStack(Items.redstone, 3);
        chain.add(
            BookmarkItem.of(
                1,
                byproduct,
                3,
                separator,
                BookmarkItemType.RESULT,
                BookmarkItem.generatePermutations(byproduct, (codechicken.nei.recipe.Recipe) null)));
        recipe(
            chain,
            "target",
            Items.diamond,
            1,
            1,
            new ItemStack(Items.gold_ingot, 4),
            new ItemStack(Items.redstone, 6));
        RecipeChainMath math = new WorklistPlan(1, chain).remainingChain(new ItemStack[0]);
        assertEquals(2, runs(math, separator));
    }

    public void multipleTargetsShareBatchSurplus() {
        List<BookmarkItem> chain = new ArrayList<>();
        RecipeId wire = recipe(chain, "wiremill", Items.gold_ingot, 4, 1, Items.iron_ingot, 1);
        recipe(chain, "target-one", Items.diamond, 1, 2, Items.gold_ingot, 1);
        recipe(chain, "target-two", Items.emerald, 1, 3, Items.gold_ingot, 1);
        RecipeChainMath math = new WorklistPlan(1, chain).remainingChain(new ItemStack[0]);
        assertEquals(2, runs(math, wire));
    }

    public void fluidContainersUseMillibucketsAndPreserveRealInventory() {
        List<BookmarkItem> chain = new ArrayList<>();
        recipe(chain, "fluid-consumer", Items.diamond, 1, 3, Items.water_bucket, 2);
        ItemStack[] inventory = { new ItemStack(Items.water_bucket, 2) };
        RecipeChainMath math = new WorklistPlan(1, chain).remainingChain(inventory);
        BookmarkItem water = math.recipeIngredients.get(0);
        assertEquals(2000, water.factor);
        assertEquals(6000, water.amount);
        assertEquals(Long.valueOf(4000), math.requiredAmount.get(water));
        assertEquals(2, inventory[0].stackSize);
        RecipeChainMath mixedContainers = new WorklistPlan(1, chain).remainingChain(
            new ItemStack[] { new ItemStack(Items.water_bucket, 1), new ItemStack(Items.potionitem, 1) });
        assertEquals(Long.valueOf(4000), mixedContainers.requiredAmount.get(mixedContainers.recipeIngredients.get(0)));
    }

    public void multipleInventoryStacksAreAllAvailable() {
        List<BookmarkItem> chain = new ArrayList<>();
        RecipeId target = recipe(chain, "target", Items.diamond, 1, 100, Items.gold_ingot, 1);
        ItemStack[] inventory = { new ItemStack(Items.diamond, 64), new ItemStack(Items.diamond, 36) };
        assertEquals(0, runs(new WorklistPlan(1, chain).remainingChain(inventory), target));
        assertEquals(64, inventory[0].stackSize);
        assertEquals(36, inventory[1].stackSize);
    }

    public void deepChainStopsAtOwnedIntermediate() {
        List<BookmarkItem> chain = new ArrayList<>();
        List<RecipeId> ids = new ArrayList<>();
        for (int index = 1; index <= 80; index++) {
            ItemStack output = new ItemStack(Items.dye, index == 80 ? 17 : 1, index);
            ItemStack input = new ItemStack(Items.dye, 1, index - 1);
            RecipeId id = RecipeId.of(output, "depth-" + index, Arrays.asList(input));
            ids.add(id);
            chain.add(
                BookmarkItem.of(
                    1,
                    output,
                    1,
                    id,
                    BookmarkItemType.RESULT,
                    BookmarkItem.generatePermutations(output, (codechicken.nei.recipe.Recipe) null)));
            chain.add(
                BookmarkItem.of(
                    1,
                    input,
                    1,
                    id,
                    BookmarkItemType.INGREDIENT,
                    BookmarkItem.generatePermutations(input, (codechicken.nei.recipe.Recipe) null)));
        }
        RecipeChainMath math = new WorklistPlan(1, chain)
            .remainingChain(new ItemStack[] { new ItemStack(Items.dye, 17, 40) });
        for (int index = 0; index < 80; index++) assertEquals(index < 40 ? 0 : 17, runs(math, ids.get(index)));
    }

    public void missingReusableToolsAreCountedOnceAndDisappearWhenOwned() {
        List<BookmarkItem> chain = new ArrayList<>();
        recipe(
            chain,
            "press-one",
            Items.diamond,
            1,
            10,
            new ItemStack(Items.iron_ingot, 1),
            new ItemStack(Items.stick, 0));
        recipe(
            chain,
            "press-two",
            Items.emerald,
            1,
            12,
            new ItemStack(Items.iron_ingot, 1),
            new ItemStack(Items.stick, 0));
        WorklistPlan plan = new WorklistPlan(1, chain);
        ItemStack[] rawOnly = { new ItemStack(Items.iron_ingot, 22) };
        List<BookmarkItem> missing = plan.missingInputs(plan.remainingChain(rawOnly), rawOnly);
        assertEquals(1, missing.size());
        assertEquals(Items.stick, missing.get(0).itemStack.getItem());
        assertEquals(1, missing.get(0).amount);
        assertEquals(0, missing.get(0).factor);
        ItemStack[] stocked = { new ItemStack(Items.iron_ingot, 22), new ItemStack(Items.stick, 1) };
        assertEquals(
            0,
            plan.missingInputs(plan.remainingChain(stocked), stocked)
                .size());
        ItemStack[] finished = { new ItemStack(Items.diamond, 10), new ItemStack(Items.emerald, 12) };
        assertEquals(
            0,
            plan.missingInputs(plan.remainingChain(finished), finished)
                .size());
    }

    public void cyclicRecipesLeaveAFiniteExternalSeedRequirement() {
        List<BookmarkItem> chain = new ArrayList<>();
        recipe(chain, "cycle-a", Items.gold_ingot, 2, 1, Items.redstone, 1);
        recipe(chain, "cycle-b", Items.redstone, 1, 1, Items.gold_ingot, 1);
        RecipeId target = recipe(chain, "target", Items.diamond, 1, 8, Items.gold_ingot, 1);
        WorklistPlan plan = new WorklistPlan(1, chain);
        ItemStack[] empty = new ItemStack[0];
        RecipeChainMath math = plan.remainingChain(empty);
        assertEquals(8, runs(math, target));
        List<BookmarkItem> missing = plan.missingInputs(math, empty);
        org.junit.Assert.assertFalse("A cycle cannot bootstrap with no stock", missing.isEmpty());
        for (BookmarkItem item : missing) org.junit.Assert.assertTrue(item.amount > 0 && item.amount <= 8);
        ItemStack[] finished = { new ItemStack(Items.diamond, 8) };
        assertEquals(
            0,
            plan.missingInputs(plan.remainingChain(finished), finished)
                .size());
    }

    public void reusableConfigurationMustMatchMetadataAndNbt() {
        List<BookmarkItem> chain = new ArrayList<>();
        ItemStack configured = new ItemStack(Items.dye, 0, 1);
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        tag.setString("configuration", "required");
        configured.setTagCompound(tag);
        recipe(chain, "configured-machine", Items.diamond, 1, 5, new ItemStack(Items.iron_ingot, 1), configured);
        WorklistPlan plan = new WorklistPlan(1, chain);
        ItemStack[] stock = { new ItemStack(Items.iron_ingot, 5), new ItemStack(Items.dye, 1, 2) };
        assertEquals(
            1,
            plan.missingInputs(plan.remainingChain(stock), stock)
                .size());
        stock[1] = new ItemStack(Items.dye, 1, 1);
        assertEquals(
            1,
            plan.missingInputs(plan.remainingChain(stock), stock)
                .size());
        stock[1] = configured.copy();
        stock[1].stackSize = 1;
        stock[1].getTagCompound()
            .setString("configuration", "wrong");
        assertEquals(
            1,
            plan.missingInputs(plan.remainingChain(stock), stock)
                .size());
        stock[1] = configured.copy();
        stock[1].stackSize = 1;
        stock[1].getTagCompound()
            .setString("additionalInformation", "allowed");
        assertEquals(
            0,
            plan.missingInputs(plan.remainingChain(stock), stock)
                .size());
        assertEquals(
            "required",
            stock[1].getTagCompound()
                .getString("configuration"));
    }

    public void consumedConfigurationCannotBeReplacedByUntaggedStock() {
        List<BookmarkItem> chain = new ArrayList<>();
        ItemStack required = new ItemStack(Items.gold_ingot, 1);
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        tag.setString("configuration", "required");
        required.setTagCompound(tag);
        recipe(chain, "configured-consumer", Items.diamond, 1, 5, required);
        WorklistPlan plan = new WorklistPlan(1, chain);
        ItemStack[] stock = { new ItemStack(Items.gold_ingot, 5) };
        List<BookmarkItem> missing = plan.missingInputs(plan.remainingChain(stock), stock);
        assertEquals(1, missing.size());
        assertEquals(5, missing.get(0).amount);
        stock[0] = required.copy();
        stock[0].stackSize = 5;
        assertEquals(
            0,
            plan.missingInputs(plan.remainingChain(stock), stock)
                .size());
    }

    public void manualCompletionCutsUpstreamWithoutDoubleCountingInventory() {
        List<BookmarkItem> chain = new ArrayList<>();
        RecipeId wire = recipe(chain, "wiremill", Items.gold_ingot, 2, 1, Items.iron_ingot, 1);
        RecipeId component = recipe(chain, "assembler", Items.redstone, 1, 1, Items.gold_ingot, 3);
        RecipeId target = recipe(chain, "target", Items.diamond, 1, 4, Items.redstone, 2);
        WorklistPlan plan = new WorklistPlan(1, chain);
        BookmarkItem output = plan.progressOutputs()
            .get(1);
        plan.setCompleted(output, 4);
        RecipeChainMath math = plan.remainingChain(new ItemStack[0]);
        assertEquals(4, runs(math, component));
        assertEquals(6, runs(math, wire));
        assertEquals(4, runs(math, target));
        ItemStack[] inventory = { new ItemStack(Items.redstone, 2), new ItemStack(Items.redstone, 2) };
        assertEquals(4, runs(plan.remainingChain(inventory), component));
        inventory[0].stackSize = 3;
        assertEquals(3, runs(plan.remainingChain(inventory), component));
        assertEquals(3, inventory[0].stackSize);
        plan.setCompleted(output, 0);
        assertEquals(8, runs(plan.remainingChain(new ItemStack[0]), component));
    }

    public void manualHalfCompletionRoundsBatchesAndCanBeUndone() {
        List<BookmarkItem> chain = new ArrayList<>();
        RecipeId target = recipe(chain, "target", Items.diamond, 3, 5, Items.gold_ingot, 2);
        WorklistPlan plan = new WorklistPlan(1, chain);
        BookmarkItem output = plan.progressOutputs()
            .get(0);
        assertEquals(9, plan.completeHalf(output, new ItemStack[0]));
        assertEquals(2, runs(plan.remainingChain(new ItemStack[0]), target));
        plan.setCompleted(output, 4);
        assertEquals(4, runs(plan.remainingChain(new ItemStack[0]), target));
        plan.setCompleted(output, 15);
        assertEquals(0, runs(plan.remainingChain(new ItemStack[0]), target));
        assertEquals(
            1,
            plan.progressOutputs()
                .size());
        plan.setCompleted(output, 0);
        assertEquals(5, runs(plan.remainingChain(new ItemStack[0]), target));
        assertEquals(9, plan.completeHalf(output, new ItemStack[] { new ItemStack(Items.diamond, 3) }));
        assertEquals(2, runs(plan.remainingChain(new ItemStack[] { new ItemStack(Items.diamond, 3) }), target));
        try {
            plan.setCompleted(output, -1);
            org.junit.Assert.fail("Negative completion must be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals(9, plan.completed(output));
        }
    }

    public void manualFluidProgressUsesMillibuckets() {
        List<BookmarkItem> chain = new ArrayList<>();
        RecipeId producer = recipe(chain, "fluid-maker", Items.water_bucket, 1, 1, Items.iron_ingot, 1);
        recipe(chain, "fluid-consumer", Items.diamond, 1, 3, Items.water_bucket, 2);
        WorklistPlan plan = new WorklistPlan(1, chain);
        plan.setCompleted(
            plan.progressOutputs()
                .get(0),
            2500);
        assertEquals(4, runs(plan.remainingChain(new ItemStack[0]), producer));
        assertEquals(4, runs(plan.remainingChain(new ItemStack[] { new ItemStack(Items.water_bucket, 2) }), producer));
        assertEquals(3, runs(plan.remainingChain(new ItemStack[] { new ItemStack(Items.water_bucket, 3) }), producer));
    }

    public void manualStockIsSharedAcrossBranches() {
        List<BookmarkItem> chain = new ArrayList<>();
        RecipeId wire = recipe(chain, "wiremill", Items.gold_ingot, 2, 1, Items.iron_ingot, 1);
        recipe(chain, "left", Items.diamond, 1, 2, Items.gold_ingot, 2);
        recipe(chain, "right", Items.redstone, 1, 2, Items.gold_ingot, 3);
        WorklistPlan plan = new WorklistPlan(1, chain);
        plan.setCompleted(
            plan.progressOutputs()
                .get(0),
            4);
        assertEquals(3, runs(plan.remainingChain(new ItemStack[0]), wire));
        assertEquals(3, runs(plan.remainingChain(new ItemStack[] { new ItemStack(Items.gold_ingot, 4) }), wire));
    }

    public void manualProgressSurvivesReloadAndCanBeCleared() throws Exception {
        List<BookmarkItem> chain = new ArrayList<>();
        RecipeId target = recipe(chain, "target", Items.diamond, 1, 10, Items.gold_ingot, 2);
        java.nio.file.Path directory = java.nio.file.Files.createTempDirectory("worklist-progress-test");
        java.nio.file.Path file = directory.resolve("progress.properties");
        try {
            WorklistPlan plan = new WorklistPlan(1, chain);
            plan.loadProgress(file);
            plan.setCompleted(
                plan.progressOutputs()
                    .get(0),
                7);
            WorklistPlan reopened = new WorklistPlan(1, chain);
            reopened.loadProgress(file);
            assertEquals(3, runs(reopened.remainingChain(new ItemStack[0]), target));
            reopened.setCompleted(
                reopened.progressOutputs()
                    .get(0),
                0);
            plan.loadProgress(file);
            assertEquals(10, runs(plan.remainingChain(new ItemStack[0]), target));
        } finally {
            java.nio.file.Files.deleteIfExists(file);
            java.nio.file.Files.deleteIfExists(directory);
        }
    }

    private static RecipeId recipe(List<BookmarkItem> chain, String name, Item output, int yield, int runs, Item input,
        int count) {
        return recipe(chain, name, output, yield, runs, new ItemStack(input, count));
    }

    private static RecipeId recipe(List<BookmarkItem> chain, String name, Item output, int yield, int runs,
        ItemStack... inputs) {
        ItemStack out = new ItemStack(output, yield);
        RecipeId id = RecipeId.of(out, name, Arrays.asList(inputs));
        chain.add(
            BookmarkItem.of(
                1,
                new ItemStack(output, yield * runs),
                yield,
                id,
                BookmarkItemType.RESULT,
                BookmarkItem.generatePermutations(out, (codechicken.nei.recipe.Recipe) null)));
        for (ItemStack in : inputs) chain.add(
            BookmarkItem.of(
                1,
                in,
                in.stackSize,
                id,
                BookmarkItemType.INGREDIENT,
                BookmarkItem.generatePermutations(in, (codechicken.nei.recipe.Recipe) null)));
        return id;
    }

    private static long runs(RecipeChainMath math, RecipeId id) {
        return math.recipeResults.stream()
            .filter(item -> item.recipeId.equals(id))
            .mapToLong(item -> item.amount / item.factor)
            .max()
            .orElse(0);
    }
}
