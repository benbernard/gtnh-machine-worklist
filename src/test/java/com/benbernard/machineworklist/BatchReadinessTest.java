package com.benbernard.machineworklist;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class BatchReadinessTest {

    @Test
    public void roundsToWholeRunsAndCapsAtOutstandingWork() {
        assertEquals(
            3,
            BatchReadiness.maximumRuns(
                20,
                new long[] { 11 },
                Collections.singletonList(new BatchReadiness.Ingredient(3, false, 0))));
        assertEquals(
            2,
            BatchReadiness.maximumRuns(
                2,
                new long[] { 11 },
                Collections.singletonList(new BatchReadiness.Ingredient(3, false, 0))));
    }

    @Test
    public void cannotSpendSameStockForTwoIngredientSlots() {
        assertEquals(
            2,
            BatchReadiness.maximumRuns(
                10,
                new long[] { 10 },
                Arrays.asList(new BatchReadiness.Ingredient(2, false, 0), new BatchReadiness.Ingredient(3, false, 0))));
    }

    @Test
    public void reroutesFlexibleIngredientToPreserveRestrictedIngredient() {
        assertEquals(
            4,
            BatchReadiness.maximumRuns(
                9,
                new long[] { 4, 4 },
                Arrays.asList(
                    new BatchReadiness.Ingredient(1, false, 0, 1),
                    new BatchReadiness.Ingredient(1, false, 0))));
    }

    @Test
    public void reusableMoldDoesNotScaleWithBatchCountButMustExist() {
        assertEquals(
            64,
            BatchReadiness.maximumRuns(
                64,
                new long[] { 64, 1 },
                Arrays.asList(new BatchReadiness.Ingredient(1, false, 0), new BatchReadiness.Ingredient(1, true, 1))));
        assertEquals(
            0,
            BatchReadiness.maximumRuns(
                64,
                new long[] { 64, 0 },
                Arrays.asList(new BatchReadiness.Ingredient(1, false, 0), new BatchReadiness.Ingredient(1, true, 1))));
    }

    @Test
    public void fluidQuantitiesAreNotLimitedToItemStackSizes() {
        assertEquals(
            10000,
            BatchReadiness.maximumRuns(
                20000,
                new long[] { 1440000 },
                Collections.singletonList(new BatchReadiness.Ingredient(144, false, 0))));
    }

    @Test
    public void hugeQuantitiesDoNotOverflowOrLoseDoublePrecision() {
        assertEquals(
            Long.MAX_VALUE / 3,
            BatchReadiness.maximumRuns(
                Long.MAX_VALUE,
                new long[] { Long.MAX_VALUE },
                Collections.singletonList(new BatchReadiness.Ingredient(3, false, 0))));
    }

    @Test
    public void noInventoryMeansBlockedAndZeroDemandMeansZeroWork() {
        assertEquals(
            0,
            BatchReadiness
                .maximumRuns(7, new long[0], Collections.singletonList(new BatchReadiness.Ingredient(1, false))));
        assertEquals(
            0,
            BatchReadiness.maximumRuns(
                0,
                new long[] { 10 },
                Collections.singletonList(new BatchReadiness.Ingredient(1, false, 0))));
    }
}
