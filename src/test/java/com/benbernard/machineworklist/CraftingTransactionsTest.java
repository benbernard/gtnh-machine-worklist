package com.benbernard.machineworklist;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CraftingTransactionsTest {

    @Test
    public void cleanPredictionMustWaitForEveryClickIncludingCleanup() {
        CraftingTransactions.Batch batch = new CraftingTransactions.Batch();
        batch.sent((short) 24);
        batch.sent((short) 25);
        batch.confirmed((short) 24, true);
        batch.seal();
        assertFalse(batch.finished());
        batch.confirmed((short) 25, true);
        assertTrue(batch.ready());
    }

    @Test
    public void rejectedClickFinishesWithoutWaitingForClicksTheServerDiscarded() {
        CraftingTransactions.Batch batch = new CraftingTransactions.Batch();
        batch.sent((short) 67);
        batch.sent((short) 68);
        batch.seal();
        batch.confirmed((short) 67, false);
        assertTrue(batch.finished());
        assertTrue(batch.rejected());
        assertFalse(batch.ready());
    }

    @Test
    public void unrelatedAcknowledgmentAndUnflushedBatchCannotCompleteTransfer() {
        CraftingTransactions.Batch batch = new CraftingTransactions.Batch();
        batch.sent(Short.MIN_VALUE);
        batch.confirmed((short) 6, false);
        assertFalse(batch.rejected());
        batch.confirmed(Short.MIN_VALUE, true);
        assertFalse(batch.ready());
        batch.seal();
        assertTrue(batch.ready());
    }
}
