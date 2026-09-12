package com.benbernard.machineworklist;

import java.util.function.LongSupplier;

/** Yield between bounded NEI transfers so input and container changes can be processed. */
final class CraftingBurst {

    static final int MAX_BATCHES = 64;
    static final int MAX_TRANSFERS = 8;
    static final long BUDGET_NANOS = 8_000_000L;

    interface Transfer {

        boolean craftNext(int maximumBatches);
    }

    static void run(Transfer transfer, LongSupplier clock) {
        long started = clock.getAsLong();
        for (int i = 0; i < MAX_TRANSFERS; i++) {
            if (!transfer.craftNext(MAX_BATCHES)) return;
            // A single NEI call cannot be interrupted; the time budget applies between calls.
            if (clock.getAsLong() - started >= BUDGET_NANOS) return;
        }
    }

    static int verifiedBatches(long before, long after, long outputPerBatch, int requested) {
        if (before < 0 || after < before || outputPerBatch <= 0 || requested < 1) return -1;
        long produced = after - before;
        if (produced % outputPerBatch != 0 || produced / outputPerBatch > requested) return -1;
        return (int) (produced / outputPerBatch);
    }

    private CraftingBurst() {}
}
