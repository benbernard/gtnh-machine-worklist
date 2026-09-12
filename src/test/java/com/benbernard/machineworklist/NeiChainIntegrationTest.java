package com.benbernard.machineworklist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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
            "backpackCleanupExcludesStorageAndSupplyExcludesMirrors",
            "stationStorageDoesNotBlockButMatrixAndResultDo",
            "closedContainerCannotOverrideTheCurrentTable",
            "stationMappingTracksChestOffsetAndExcludesStorage",
            "navigationIdentityTracksRecipeChoicesAndQuantities",
            "navigationIdentityIgnoresInventoryAndRecordedProgress",
            "bulkOutputCapacityRespectsYieldAndRemainingDemand",
            "bulkOutputCapacitySharesSpaceAcrossCoproducts",
            "bulkOutputCapacityPreservesNbtAndRealInventory",
            "bulkOutputCapacityHandlesNonStackablesAndHugeCounts",
            "chainCraftsThreeStagesAndStopsAtExactTarget",
            "chainSharesIngredientsAndBatchSurplus",
            "selectedChainExcludesOtherTargets",
            "selectedChainPreservesOwnedPartialBatches",
            "selectedIntermediateUsesCurrentDownstreamDemand",
            "chainManualStockDoesNotInventPhysicalIngredients",
            "chainPreservesExternalTargetStockWhileCrafting",
            "verifiedCraftingUpdatesConsumedAndProducedStockWithoutInventingRecords",
            "verifiedCraftingStockPersistsAndRollsBackOnSaveFailure",
            "chainPausesAtMachinesAndResumesWithTheirOutputs",
            "chainSkipsBlockedBranchAndRechecksAfterProgress",
            "chainCannotReplenishConsumedOutputsWithoutANewRequest");
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

    public void stationStorageDoesNotBlockButMatrixAndResultDo() {
        TableFixture table = new TableFixture(122);
        FixtureGui gui = new FixtureGui(table);
        table.storage.setInventorySlotContents(0, new ItemStack(Items.diamond, 64));
        table.player.setInventorySlotContents(0, new ItemStack(Items.iron_ingot, 64));
        for (net.minecraft.inventory.Slot slot : table.inventorySlots)
            assertFalse(CraftingInventory.occupiedCraftingSlot(gui, slot, table));
        table.matrix.setInventorySlotContents(4, new ItemStack(Items.stick));
        assertTrue(CraftingInventory.occupiedCraftingSlot(gui, table.getSlot(5), table));
        table.matrix.setInventorySlotContents(4, null);
        table.result.setInventorySlotContents(0, new ItemStack(Items.gold_ingot));
        assertTrue(CraftingInventory.occupiedCraftingSlot(gui, table.getSlot(0), table));
        assertEquals(64, table.storage.getStackInSlot(0).stackSize);
    }

    public void closedContainerCannotOverrideTheCurrentTable() {
        TableFixture closed = new TableFixture(0);
        closed.matrix.setInventorySlotContents(0, new ItemStack(Items.stick));
        TableFixture current = new TableFixture(122);
        FixtureGui oldGui = new FixtureGui(closed), currentGui = new FixtureGui(current);
        assertNull(CraftingInventory.activeGui(oldGui, current));
        assertFalse(CraftingInventory.backpack(oldGui, current));
        assertSame(currentGui, CraftingInventory.activeGui(currentGui, current));
        assertNull(CraftingInventory.activeGui(currentGui, null));
        for (net.minecraft.inventory.Slot slot : current.inventorySlots)
            assertFalse(CraftingInventory.occupiedCraftingSlot(currentGui, slot, current));
        assertEquals(1, closed.matrix.getStackInSlot(0).stackSize);
    }

    public void stationMappingTracksChestOffsetAndExcludesStorage() {
        for (int offset : new int[] { 0, 122, 134 }) {
            TableFixture table = new TableFixture(offset);
            FixtureGui gui = new FixtureGui(table);
            StationCraftingOverlay overlay = new StationCraftingOverlay(table);
            List<codechicken.nei.PositionedStack> ingredients = new ArrayList<>();
            for (int i = 0; i < 9; i++) ingredients.add(
                new codechicken.nei.PositionedStack(new ItemStack(Items.iron_ingot), 25 + i % 3 * 18, 6 + i / 3 * 18));
            ShapedRecipeHandler handler = new ShapedRecipeHandler(new ItemStack(Items.gold_ingot)) {

                @Override
                public List<codechicken.nei.PositionedStack> getIngredientStacks(int recipe) {
                    return ingredients;
                }
            };
            assertTrue(overlay.canFillCraftingGrid(gui, handler, 0));
            net.minecraft.inventory.Slot[][] mapped = overlay.mapIngredSlots(gui, ingredients);
            for (int i = 0; i < 9; i++) {
                assertEquals(1, mapped[i].length);
                assertSame(table.getSlot(i + 1), mapped[i][0]);
            }
            assertEquals(
                9,
                overlay.getCraftMatrixSlots(gui, handler)
                    .size());
            assertFalse(overlay.canMoveFrom(table.getSlot(10), gui));
            assertTrue(overlay.canMoveFrom(table.getSlot(11), gui));
            table.storage.setInventorySlotContents(0, new ItemStack(Items.iron_ingot, 64));
            table.player.setInventorySlotContents(0, new ItemStack(Items.iron_ingot, 9));
            assertEquals(
                9,
                table.inventorySlots.stream()
                    .filter(slot -> slot.getHasStack() && overlay.canMoveFrom(slot, gui))
                    .mapToInt(slot -> slot.getStack().stackSize)
                    .sum());
            ingredients.add(new codechicken.nei.PositionedStack(new ItemStack(Items.stick), 79, 6));
            assertFalse(overlay.canFillCraftingGrid(gui, handler, 0));
            assertEquals(64, table.storage.getStackInSlot(0).stackSize);
            assertEquals(9, table.player.getStackInSlot(0).stackSize);
        }
    }

    public void navigationIdentityTracksRecipeChoicesAndQuantities() {
        List<BookmarkItem> source = new ArrayList<>();
        recipe(source, "wire", Items.gold_ingot, 1, 4, Items.iron_ingot, 2);
        String identity = new WorklistPlan(1, source).snapshotKey();
        assertEquals(identity, new WorklistPlan(1, source).snapshotKey());
        assertNotEquals(identity, new WorklistPlan(2, source).snapshotKey());
        List<BookmarkItem> changedRecipe = new ArrayList<>();
        recipe(changedRecipe, "other wire recipe", Items.gold_ingot, 1, 4, Items.stick, 2);
        assertNotEquals(identity, new WorklistPlan(1, changedRecipe).snapshotKey());
        List<BookmarkItem> changedQuantity = new ArrayList<>();
        recipe(changedQuantity, "wire", Items.gold_ingot, 1, 5, Items.iron_ingot, 2);
        assertNotEquals(identity, new WorklistPlan(1, changedQuantity).snapshotKey());
    }

    public void navigationIdentityIgnoresInventoryAndRecordedProgress() {
        List<BookmarkItem> source = new ArrayList<>();
        recipe(source, "wire", Items.gold_ingot, 1, 4, Items.iron_ingot, 2);
        WorklistPlan plan = new WorklistPlan(1, source);
        String identity = plan.snapshotKey();
        plan.remainingChain(new ItemStack[] { new ItemStack(Items.gold_ingot, 2) });
        plan.completed.put(WorklistPlan.outputKey(BookmarkItem.of(1, new ItemStack(Items.gold_ingot))), 3L);
        plan.remainingChain(new ItemStack[0]);
        assertEquals(identity, plan.snapshotKey());
    }

    /** Installed TConstruct 1.13.57 uses an InventoryCrafting subclass and SlotCrafting result. */
    private static final class TableFixture extends net.minecraft.inventory.Container {

        final net.minecraft.inventory.InventoryCrafting matrix = new net.minecraft.inventory.InventoryCrafting(
            this,
            3,
            3) {};
        final net.minecraft.inventory.InventoryCraftResult result = new net.minecraft.inventory.InventoryCraftResult();
        final net.minecraft.inventory.InventoryBasic storage = new net.minecraft.inventory.InventoryBasic(
            "Chest",
            false,
            1);
        final net.minecraft.entity.player.InventoryPlayer player = new net.minecraft.entity.player.InventoryPlayer(
            null);

        TableFixture(int offset) {
            addSlotToContainer(new net.minecraft.inventory.SlotCrafting(null, matrix, result, 0, 124 + offset, 35));
            for (int i = 0; i < 9; i++) addSlotToContainer(
                new net.minecraft.inventory.Slot(matrix, i, 30 + offset + i % 3 * 18, 17 + i / 3 * 18));
            addSlotToContainer(new net.minecraft.inventory.Slot(storage, 0, 30, 17));
            addSlotToContainer(new net.minecraft.inventory.Slot(player, 0, 8 + offset, 84));
        }

        @Override
        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {
            return true;
        }
    }

    private static final class FixtureGui extends net.minecraft.client.gui.inventory.GuiContainer {

        FixtureGui(net.minecraft.inventory.Container container) {
            super(container);
        }

        @Override
        protected void drawGuiContainerBackgroundLayer(float ticks, int x, int y) {}
    }

    private static BookmarkItem capacityOutput(ItemStack stack, int yield) {
        BookmarkItem output = BookmarkItem.of(0, stack);
        output.factor = yield;
        return output;
    }

    public void bulkOutputCapacityRespectsYieldAndRemainingDemand() {
        List<BookmarkItem> outputs = Arrays.asList(capacityOutput(new ItemStack(Items.gold_ingot), 4));
        ItemStack[] slots = { new ItemStack(Items.gold_ingot, 60), null };
        assertEquals(17, CraftingAvailability.outputCapacity(slots, outputs, 100));
        assertEquals(3, CraftingAvailability.outputCapacity(slots, outputs, 3));
        assertEquals(1, CraftingAvailability.outputCapacity(slots, outputs, 100, 1));
        assertEquals(0, CraftingAvailability.outputCapacity(slots, outputs, 100, 2));
        assertEquals(
            0,
            CraftingAvailability.outputCapacity(new ItemStack[] { new ItemStack(Items.gold_ingot, 62) }, outputs, 100));
    }

    public void bulkOutputCapacitySharesSpaceAcrossCoproducts() {
        List<BookmarkItem> outputs = Arrays.asList(
            capacityOutput(new ItemStack(Items.gold_ingot), 4),
            capacityOutput(new ItemStack(Items.diamond), 1));
        assertEquals(16, CraftingAvailability.outputCapacity(new ItemStack[2], outputs, 100));
        assertEquals(0, CraftingAvailability.outputCapacity(new ItemStack[1], outputs, 100));
    }

    public void bulkOutputCapacityPreservesNbtAndRealInventory() {
        ItemStack tagged = new ItemStack(Items.gold_ingot, 60);
        tagged.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        tagged.getTagCompound()
            .setInteger("configuration", 1);
        ItemStack[] slots = { tagged };
        assertEquals(
            0,
            CraftingAvailability
                .outputCapacity(slots, Arrays.asList(capacityOutput(new ItemStack(Items.gold_ingot), 1)), 100));
        assertEquals(
            4,
            CraftingAvailability.outputCapacity(slots, Arrays.asList(capacityOutput(tagged.copy(), 1)), 100));
        assertEquals(60, tagged.stackSize);
        assertEquals(
            1,
            tagged.getTagCompound()
                .getInteger("configuration"));
    }

    public void bulkOutputCapacityHandlesNonStackablesAndHugeCounts() {
        assertEquals(
            3,
            CraftingAvailability.outputCapacity(
                new ItemStack[3],
                Arrays.asList(capacityOutput(new ItemStack(Items.iron_pickaxe), 1)),
                Long.MAX_VALUE));
        assertEquals(
            16,
            CraftingAvailability.outputCapacity(
                new ItemStack[1],
                Arrays.asList(capacityOutput(new ItemStack(Items.gold_ingot), 4)),
                Long.MAX_VALUE));
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

    public void chainCraftsThreeStagesAndStopsAtExactTarget() throws Exception {
        List<BookmarkItem> recipes = new ArrayList<>();
        recipe(recipes, "planks", Items.gold_ingot, 2, 1, Items.iron_ingot, 1);
        recipe(recipes, "sticks", Items.redstone, 1, 1, Items.gold_ingot, 3);
        recipe(recipes, "target", Items.diamond, 1, 4, Items.redstone, 2);
        registerCrafting(recipes);
        ItemStack[] inventory = stock(new ItemStack(Items.iron_ingot, 12));
        CraftingChain chain = new CraftingChain(new WorklistPlan(1, recipes), inventory);
        assertEquals(0, drain(chain, inventory).remainingRecipes);
        assertEquals(4, count(inventory, Items.diamond));
        assertEquals(0, count(inventory, Items.iron_ingot));
        assertEquals(0, count(inventory, Items.gold_ingot));
        assertEquals(0, count(inventory, Items.redstone));
        assertEquals(24, chain.completed);
        assertEquals(3, chain.craftedRecipes());
    }

    public void chainSharesIngredientsAndBatchSurplus() throws Exception {
        List<BookmarkItem> recipes = new ArrayList<>();
        recipe(recipes, "shared", Items.gold_ingot, 2, 1, Items.iron_ingot, 1);
        recipe(recipes, "left", Items.diamond, 1, 1, Items.gold_ingot, 2);
        recipe(recipes, "right", Items.redstone, 1, 1, Items.gold_ingot, 3);
        recipe(
            recipes,
            "target",
            Items.emerald,
            1,
            1,
            new ItemStack(Items.diamond, 2),
            new ItemStack(Items.redstone, 2));
        registerCrafting(recipes);
        ItemStack[] inventory = stock(new ItemStack(Items.iron_ingot, 4), new ItemStack(Items.gold_ingot, 3));
        CraftingChain chain = new CraftingChain(new WorklistPlan(1, recipes), inventory);
        assertEquals(0, drain(chain, inventory).remainingRecipes);
        assertEquals(1, count(inventory, Items.emerald));
        assertEquals(1, count(inventory, Items.gold_ingot));
        assertEquals(0, count(inventory, Items.iron_ingot));
        assertEquals(9, chain.completed);
    }

    public void selectedChainExcludesOtherTargets() throws Exception {
        List<BookmarkItem> recipes = new ArrayList<>();
        recipe(recipes, "shared", Items.gold_ingot, 2, 1, Items.iron_ingot, 1);
        RecipeId left = recipe(recipes, "left", Items.diamond, 1, 2, Items.gold_ingot, 3);
        RecipeId right = recipe(recipes, "right", Items.redstone, 1, 20, Items.gold_ingot, 4);
        registerCrafting(recipes);
        ItemStack[] inventory = stock(new ItemStack(Items.iron_ingot, 10), new ItemStack(Items.gold_ingot, 2));
        WorklistPlan original = new WorklistPlan(1, recipes);
        WorklistPlan request = original.chainRequest(left, inventory);
        assertEquals(0, runs(request.remainingChain(inventory), right));
        CraftingChain chain = new CraftingChain(request, inventory);
        assertEquals(0, drain(chain, inventory).remainingRecipes);
        assertEquals(2, count(inventory, Items.diamond));
        assertEquals(0, count(inventory, Items.redstone));
        assertEquals(8, count(inventory, Items.iron_ingot));
        assertEquals(20, runs(original.remainingChain(inventory), right));
    }

    public void selectedChainPreservesOwnedPartialBatches() throws Exception {
        List<BookmarkItem> recipes = new ArrayList<>();
        recipe(recipes, "shared", Items.gold_ingot, 2, 1, Items.iron_ingot, 1);
        RecipeId target = recipe(recipes, "target", Items.diamond, 4, 3, Items.gold_ingot, 3);
        registerCrafting(recipes);
        ItemStack[] inventory = stock(new ItemStack(Items.iron_ingot, 10), new ItemStack(Items.diamond, 5));
        WorklistPlan plan = new WorklistPlan(1, recipes);
        WorklistPlan request = plan.chainRequest(target, inventory);
        assertEquals(runs(plan.remainingChain(inventory), target), runs(request.remainingChain(inventory), target));
        CraftingChain chain = new CraftingChain(request, inventory);
        assertEquals(0, drain(chain, inventory).remainingRecipes);
        assertEquals(13, count(inventory, Items.diamond));
        assertEquals(7, count(inventory, Items.iron_ingot));
        assertEquals(5, chain.completed);
    }

    public void selectedIntermediateUsesCurrentDownstreamDemand() throws Exception {
        List<BookmarkItem> recipes = new ArrayList<>();
        RecipeId intermediate = recipe(recipes, "intermediate", Items.gold_ingot, 1, 1, Items.iron_ingot, 1);
        recipe(recipes, "target", Items.diamond, 1, 10, Items.gold_ingot, 2);
        registerCrafting(recipes);
        ItemStack[] inventory = stock(
            new ItemStack(Items.iron_ingot, 10),
            new ItemStack(Items.diamond, 8),
            new ItemStack(Items.gold_ingot, 1));
        WorklistPlan request = new WorklistPlan(1, recipes).chainRequest(intermediate, inventory);
        CraftingChain chain = new CraftingChain(request, inventory);
        assertEquals(0, drain(chain, inventory).remainingRecipes);
        assertEquals(4, count(inventory, Items.gold_ingot));
        assertEquals(8, count(inventory, Items.diamond));
        assertEquals(7, count(inventory, Items.iron_ingot));
        assertEquals(3, chain.completed);
    }

    public void chainManualStockDoesNotInventPhysicalIngredients() throws Exception {
        List<BookmarkItem> recipes = new ArrayList<>();
        recipe(recipes, "intermediate", Items.redstone, 1, 1, Items.iron_ingot, 1);
        RecipeId target = recipe(recipes, "target", Items.diamond, 1, 2, Items.redstone, 1);
        registerCrafting(recipes);
        ItemStack[] inventory = stock(new ItemStack(Items.iron_ingot, 10));
        WorklistPlan plan = new WorklistPlan(1, recipes);
        plan.setCompleted(
            plan.progressOutputs()
                .get(0),
            2);
        CraftingChain chain = new CraftingChain(plan.chainRequest(target, inventory), inventory);
        org.junit.Assert.assertTrue(drain(chain, inventory).remainingRecipes > 0);
        assertEquals(0, chain.completed);
        assertEquals(10, count(inventory, Items.iron_ingot));
        assertEquals(0, count(inventory, Items.diamond));
    }

    public void chainPreservesExternalTargetStockWhileCrafting() throws Exception {
        List<BookmarkItem> recipes = new ArrayList<>();
        recipe(recipes, "target", Items.diamond, 1, 12, Items.iron_ingot, 1);
        registerCrafting(recipes);
        WorklistPlan plan = new WorklistPlan(1, recipes);
        BookmarkItem output = plan.progressOutputs()
            .get(0);
        plan.setCompleted(output, 10); // Eight held, two recorded elsewhere.
        ItemStack[] inventory = stock(new ItemStack(Items.diamond, 8), new ItemStack(Items.iron_ingot, 4));
        CraftingChain chain = new CraftingChain(plan.chainRequest(null, inventory), inventory);
        assertEquals(0, drain(chain, inventory).remainingRecipes);
        assertEquals(2, chain.completed);
        assertEquals(10, count(inventory, Items.diamond));
        assertEquals(2, count(inventory, Items.iron_ingot));
        assertEquals(12, chain.plan.completed(output));
    }

    public void verifiedCraftingUpdatesConsumedAndProducedStockWithoutInventingRecords() {
        List<BookmarkItem> recipes = new ArrayList<>();
        recipe(recipes, "intermediate", Items.redstone, 1, 1, Items.iron_ingot, 1);
        recipe(recipes, "target", Items.diamond, 1, 12, Items.redstone, 1);
        WorklistPlan plan = new WorklistPlan(1, recipes);
        BookmarkItem intermediate = plan.progressOutputs()
            .get(0),
            target = plan.progressOutputs()
                .get(1);
        plan.setCompleted(intermediate, 10);
        plan.setCompleted(target, 5);
        ItemStack[] before = stock(
            new ItemStack(Items.redstone, 4),
            new ItemStack(Items.diamond, 2),
            new ItemStack(Items.iron_ingot, 64));
        ItemStack[] after = stock(
            new ItemStack(Items.redstone, 2),
            new ItemStack(Items.diamond, 4),
            new ItemStack(Items.iron_ingot, 62));
        plan.recordCraftedInventory(before, after);
        assertEquals(8, plan.completed(intermediate));
        assertEquals(7, plan.completed(target));
        assertEquals(2, plan.completed.size());
        assertEquals(4, count(before, Items.redstone));
        assertEquals(2, count(after, Items.redstone));
        plan.setCompleted(intermediate, 1); // Visible stock already exceeds this older record.
        plan.recordCraftedInventory(before, after);
        assertEquals(2, plan.completed(intermediate));
    }

    public void verifiedCraftingStockPersistsAndRollsBackOnSaveFailure() throws Exception {
        List<BookmarkItem> recipes = new ArrayList<>();
        recipe(recipes, "target", Items.diamond, 1, 12, Items.iron_ingot, 1);
        WorklistPlan plan = new WorklistPlan(1, recipes);
        BookmarkItem output = plan.progressOutputs()
            .get(0);
        java.nio.file.Path directory = java.nio.file.Files.createTempDirectory("worklist-stock-test-");
        java.nio.file.Path file = directory.resolve("progress.properties");
        try {
            plan.loadProgress(file);
            plan.setCompleted(output, 10);
            plan.recordCraftedInventory(
                stock(new ItemStack(Items.diamond, 8)),
                stock(new ItemStack(Items.diamond, 10)));
            plan.loadProgress(file);
            assertEquals(12, plan.completed(output));
            java.nio.file.Files.delete(file);
            java.nio.file.Files.createDirectory(file);
            java.nio.file.Files.write(file.resolve("occupied"), new byte[] { 1 });
            try {
                plan.recordCraftedInventory(
                    stock(new ItemStack(Items.diamond, 10)),
                    stock(new ItemStack(Items.diamond, 11)));
                org.junit.Assert.fail("A failed save must not silently change credited stock");
            } catch (IllegalStateException expected) {
                assertEquals(12, plan.completed(output));
            }
            java.nio.file.Files.delete(file.resolve("occupied"));
        } finally {
            java.nio.file.Files.deleteIfExists(file);
            java.nio.file.Files.delete(directory);
        }
    }

    public void chainPausesAtMachinesAndResumesWithTheirOutputs() throws Exception {
        List<BookmarkItem> recipes = new ArrayList<>();
        recipe(recipes, "before", Items.gold_ingot, 1, 1, Items.iron_ingot, 1);
        RecipeId machine = recipe(recipes, "machine", Items.redstone, 1, 1, Items.gold_ingot, 1);
        recipe(recipes, "after", Items.diamond, 1, 3, Items.redstone, 1);
        registerCrafting(recipes);
        cacheHandler(machine, new MachineRecipeHandler(new ItemStack(Items.redstone)));
        ItemStack[] inventory = stock(new ItemStack(Items.iron_ingot, 3));
        WorklistPlan plan = new WorklistPlan(1, recipes);
        CraftingChain chain = new CraftingChain(plan, inventory);
        org.junit.Assert.assertTrue(
            drain(chain, inventory).stoppedReason()
                .contains("Manual operation"));
        assertEquals(3, count(inventory, Items.gold_ingot));
        assertEquals(0, count(inventory, Items.diamond));
        // The player supplies the machine's real outputs, consuming its inputs outside the worklist.
        inventory = stock(new ItemStack(Items.redstone, 3));
        chain = new CraftingChain(plan.chainRequest(null, inventory), inventory);
        assertEquals(0, drain(chain, inventory).remainingRecipes);
        assertEquals(3, count(inventory, Items.diamond));
        assertEquals(3, chain.completed);
    }

    public void chainSkipsBlockedBranchAndRechecksAfterProgress() throws Exception {
        List<BookmarkItem> recipes = new ArrayList<>();
        RecipeId left = recipe(recipes, "left", Items.diamond, 1, 2, Items.iron_ingot, 1);
        recipe(recipes, "right", Items.redstone, 1, 2, Items.gold_ingot, 1);
        registerCrafting(recipes);
        ItemStack[] inventory = stock(new ItemStack(Items.iron_ingot, 2), new ItemStack(Items.gold_ingot, 2));
        CraftingChain chain = new CraftingChain(new WorklistPlan(1, recipes), inventory);
        CraftingChain.Selection selection = chain.inspect(inventory, step -> {
            CraftingAvailability result = fixtureAvailability(step);
            if (step.id.equals(left)) {
                result.batches = 0;
                result.reasons.add("Grid does not fit");
            }
            return result;
        });
        assertEquals(Items.redstone, selection.next.outputs.get(0).itemStack.getItem());
        transfer(chain, selection, inventory);
        assertEquals(2, count(inventory, Items.redstone));
        assertEquals(0, drain(chain, inventory).remainingRecipes);
        assertEquals(2, count(inventory, Items.diamond));
        assertEquals(4, chain.completed);
    }

    public void chainCannotReplenishConsumedOutputsWithoutANewRequest() throws Exception {
        List<BookmarkItem> recipes = new ArrayList<>();
        recipe(recipes, "target", Items.gold_ingot, 1, 1, Items.iron_ingot, 1);
        registerCrafting(recipes);
        ItemStack[] inventory = stock(new ItemStack(Items.iron_ingot, 10));
        CraftingChain chain = new CraftingChain(new WorklistPlan(1, recipes), inventory);
        transfer(chain, chain.inspect(inventory, NeiChainIntegrationTest::fixtureAvailability), inventory);
        // Simulate the completed output being consumed outside the request before the next tick.
        inventory = stock(new ItemStack(Items.iron_ingot, 9));
        CraftingChain.Selection stopped = chain.inspect(inventory, NeiChainIntegrationTest::fixtureAvailability);
        assertEquals(null, stopped.next);
        org.junit.Assert.assertTrue(
            stopped.stoppedReason()
                .contains("execution limit"));
        assertEquals(1, chain.completed);
    }

    private static ItemStack[] stock(ItemStack... contents) {
        return Arrays.copyOf(contents, 36);
    }

    private static long count(ItemStack[] inventory, Item item) {
        long count = 0;
        for (ItemStack stack : inventory) if (stack != null && stack.getItem() == item) count += stack.stackSize;
        return count;
    }

    private static CraftingAvailability fixtureAvailability(WorklistPlan.Step step) {
        CraftingAvailability result = new CraftingAvailability();
        result.batches = step.readyRuns;
        if (result.batches == 0) result.reasons.add("Missing physical ingredients");
        return result;
    }

    private static CraftingChain.Selection drain(CraftingChain chain, ItemStack[] inventory) {
        for (int i = 0; i < 100; i++) {
            CraftingChain.Selection selection = chain.inspect(inventory, NeiChainIntegrationTest::fixtureAvailability);
            if (selection.next == null) return selection;
            transfer(chain, selection, inventory);
        }
        throw new AssertionError("Chain failed to terminate");
    }

    private static void transfer(CraftingChain chain, CraftingChain.Selection selection, ItemStack[] inventory) {
        ItemStack[] before = new ItemStack[inventory.length];
        for (int i = 0; i < inventory.length; i++) before[i] = inventory[i] == null ? null : inventory[i].copy();
        int batches = (int) Math.min(64, selection.batches);
        WorklistPlan.Step step = selection.next;
        for (BookmarkItem input : step.inputs) {
            if (input.factor == 0) continue;
            long needed = input.factor * batches;
            for (int i = 0; i < inventory.length; i++)
                if (inventory[i] != null && WorklistPlan.matchesInput(input, BookmarkItem.of(0, inventory[i]))) {
                    int amount = (int) Math.min(needed, inventory[i].stackSize);
                    needed -= amount;
                    inventory[i].stackSize -= amount;
                    if (inventory[i].stackSize == 0) inventory[i] = null;
                }
            assertEquals("A shared input was overspent", 0, needed);
        }
        for (BookmarkItem output : step.outputs) {
            long produced = output.factor * batches;
            for (int i = 0; i < inventory.length && produced > 0; i++) {
                if (inventory[i] == null) {
                    inventory[i] = output.itemStack.copy();
                    inventory[i].stackSize = 0;
                }
                if (!inventory[i].isItemEqual(output.itemStack)) continue;
                int amount = (int) Math.min(produced, 64 - inventory[i].stackSize);
                inventory[i].stackSize += amount;
                produced -= amount;
            }
            assertEquals("Fixture output space exhausted", 0, produced);
        }
        chain.record(step.id, batches);
        chain.plan.recordCraftedInventory(before, inventory);
    }

    private static void registerCrafting(List<BookmarkItem> recipes) throws Exception {
        for (BookmarkItem item : recipes) if (item.type == BookmarkItemType.RESULT)
            cacheHandler(item.recipeId, new ShapedRecipeHandler(item.itemStack));
    }

    @SuppressWarnings("unchecked")
    private static void cacheHandler(RecipeId recipe, ShapedRecipeHandler handler) throws Exception {
        java.lang.reflect.Field field = codechicken.nei.recipe.RecipeHandlerRef.class
            .getDeclaredField("recipeRefCache");
        field.setAccessible(true);
        ((java.util.Map<RecipeId, codechicken.nei.recipe.RecipeHandlerRef>) field.get(null))
            .put(recipe, codechicken.nei.recipe.RecipeHandlerRef.of(handler, 0));
    }

    /** Handler metadata without NEI's renderer initialization; chain math remains the real NEI engine. */
    public static class ShapedRecipeHandler implements codechicken.nei.recipe.IRecipeHandler {

        private final ItemStack output;

        ShapedRecipeHandler(ItemStack output) {
            this.output = output;
        }

        @Override
        public codechicken.nei.PositionedStack getResultStack(int recipe) {
            return new codechicken.nei.PositionedStack(output, 0, 0);
        }

        @Override
        public List<codechicken.nei.PositionedStack> getOtherStacks(int recipe) {
            return Collections.emptyList();
        }

        @Override
        public String getRecipeName() {
            return "Fixture crafting";
        }

        @Override
        public int numRecipes() {
            return 1;
        }

        @Override
        public void drawBackground(int recipe) {}

        @Override
        public void drawForeground(int recipe) {}

        @Override
        public void onUpdate() {}

        @Override
        public List<codechicken.nei.PositionedStack> getIngredientStacks(int recipe) {
            return Collections.emptyList();
        }

        @Override
        public boolean hasOverlay(net.minecraft.client.gui.inventory.GuiContainer gui,
            net.minecraft.inventory.Container container, int recipe) {
            return false;
        }

        @Override
        public codechicken.nei.api.IRecipeOverlayRenderer getOverlayRenderer(
            net.minecraft.client.gui.inventory.GuiContainer gui, int recipe) {
            return null;
        }

        @Override
        public codechicken.nei.api.IOverlayHandler getOverlayHandler(
            net.minecraft.client.gui.inventory.GuiContainer gui, int recipe) {
            return null;
        }

        @Override
        public List<String> handleTooltip(codechicken.nei.recipe.GuiRecipe<?> gui, List<String> tooltip, int recipe) {
            return tooltip;
        }

        @Override
        public List<String> handleItemTooltip(codechicken.nei.recipe.GuiRecipe<?> gui, ItemStack stack,
            List<String> tooltip, int recipe) {
            return tooltip;
        }

        @Override
        public boolean keyTyped(codechicken.nei.recipe.GuiRecipe<?> gui, char character, int key, int recipe) {
            return false;
        }

        @Override
        public boolean mouseClicked(codechicken.nei.recipe.GuiRecipe<?> gui, int button, int recipe) {
            return false;
        }
    }

    public static class MachineRecipeHandler extends ShapedRecipeHandler {

        MachineRecipeHandler(ItemStack output) {
            super(output);
        }

        @Override
        public String getRecipeName() {
            return "Fixture machine";
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
