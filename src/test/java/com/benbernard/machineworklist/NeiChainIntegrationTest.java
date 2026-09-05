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

import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.bookmark.BookmarkItem.BookmarkItemType;
import codechicken.nei.recipe.Recipe.RecipeId;
import codechicken.nei.recipe.chain.RecipeChainMath;

/** Exercises the actual pinned NEI engine, using vanilla items as deterministic recipe fixtures. */
public class NeiChainIntegrationTest {

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
            fixture.getMethod("executeFixtures")
                .invoke(null);
        }
    }

    public static void executeFixtures() {
        Bootstrap.func_151354_b();
        NeiChainIntegrationTest fixture = new NeiChainIntegrationTest();
        fixture.existingIntermediatesReduceUpstreamMachineBatches();
        fixture.finishedTargetEliminatesItsEntireChain();
        fixture.sharedBranchesSpendInventoryOnce();
        fixture.plainBookmarksDoNotInventStock();
        fixture.repeatedCalculationsDoNotMutateInventoryOrTargets();
        fixture.coproductsComeFromTheSameBatches();
        fixture.multipleTargetsShareBatchSurplus();
        fixture.fluidContainersUseMillibucketsAndPreserveRealInventory();
        fixture.multipleInventoryStacksAreAllAvailable();
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
    }

    public void multipleInventoryStacksAreAllAvailable() {
        List<BookmarkItem> chain = new ArrayList<>();
        RecipeId target = recipe(chain, "target", Items.diamond, 1, 100, Items.gold_ingot, 1);
        ItemStack[] inventory = { new ItemStack(Items.diamond, 64), new ItemStack(Items.diamond, 36) };
        assertEquals(0, runs(new WorklistPlan(1, chain).remainingChain(inventory), target));
        assertEquals(64, inventory[0].stackSize);
        assertEquals(36, inventory[1].stackSize);
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
