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
        assertTrue(batch.finished());
        assertFalse(batch.ready());
        assertTrue(batch.requestSnapshot());
        batch.contentsApplied();
        assertFalse(batch.ready());
        batch.cursorApplied();
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
        assertTrue(batch.requestSnapshot());
        batch.contentsApplied();
        batch.cursorApplied();
        assertTrue(batch.ready());
    }

    @Test
    public void unrelatedSnapshotsAndCursorBeforeRequestDoNotCompleteBatch() {
        CraftingTransactions.Batch batch = new CraftingTransactions.Batch();
        batch.contentsApplied();
        batch.cursorApplied();
        batch.seal();
        assertTrue(batch.requestSnapshot());
        assertFalse(batch.ready());
        batch.cursorApplied();
        assertFalse(batch.ready());
        batch.contentsApplied();
        batch.cursorApplied();
        assertTrue(batch.ready());
        assertFalse(batch.requestSnapshot());
    }

    @Test
    public void lateClickCancelsAccountingAndNeverRequestsAnotherSnapshot() {
        CraftingTransactions.Batch batch = new CraftingTransactions.Batch();
        batch.seal();
        batch.sent((short) 99);
        assertTrue(batch.rejected());
        batch.confirmed((short) 99, true);
        assertFalse(batch.requestSnapshot());
        assertFalse(batch.ready());
    }
}
