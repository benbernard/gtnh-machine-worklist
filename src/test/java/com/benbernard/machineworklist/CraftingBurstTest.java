package com.benbernard.machineworklist;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class CraftingBurstTest {

    @Test
    public void yieldsAfterASlowTransferWithoutSubmittingAnother() {
        AtomicLong clock = new AtomicLong();
        AtomicInteger calls = new AtomicInteger();
        CraftingBurst.run(maximum -> {
            calls.incrementAndGet();
            clock.addAndGet(CraftingBurst.BUDGET_NANOS + 1);
            return true;
        }, clock::get);
        assertEquals(1, calls.get());
    }

    @Test
    public void limitsWorkEvenWhenTheClockDoesNotAdvance() {
        AtomicInteger submitted = new AtomicInteger();
        CraftingBurst.run(maximum -> {
            submitted.addAndGet(maximum);
            return true;
        }, () -> 0);
        assertEquals(512, submitted.get());
    }

    @Test
    public void stopsImmediatelyWhenTheSessionFinishesOrCancels() {
        AtomicInteger calls = new AtomicInteger();
        CraftingBurst.run(maximum -> calls.incrementAndGet() < 2, () -> 0);
        assertEquals(2, calls.get());
    }

    @Test
    public void countsOnlyObservedWholeBatchesIncludingPartialTransfers() {
        assertEquals(64, CraftingBurst.verifiedBatches(6, 134, 2, 64));
        assertEquals(29, CraftingBurst.verifiedBatches(640, 698, 2, 29));
        assertEquals(3, CraftingBurst.verifiedBatches(6, 12, 2, 64));
        assertEquals(0, CraftingBurst.verifiedBatches(6, 6, 2, 64));
    }

    @Test
    public void rejectsOverproductionFractionalResultsAndInvalidCounts() {
        assertEquals(-1, CraftingBurst.verifiedBatches(6, 136, 2, 64));
        assertEquals(-1, CraftingBurst.verifiedBatches(6, 9, 2, 64));
        assertEquals(-1, CraftingBurst.verifiedBatches(6, 4, 2, 64));
        assertEquals(-1, CraftingBurst.verifiedBatches(6, 10, 0, 64));
        assertEquals(-1, CraftingBurst.verifiedBatches(0, Long.MAX_VALUE, 1, 64));
    }
}
