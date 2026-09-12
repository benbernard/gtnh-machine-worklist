package com.benbernard.machineworklist;

/** One observation per client tick; never count the calls inside a crafting burst as elapsed ticks. */
final class CraftingSettlement {

    enum Action {
        WAIT,
        RECOVER,
        READY,
        STOP
    }

    static final int MAX_TICKS = 200;
    static final int MAX_RECOVERIES = 2;
    private final int quietTicks;
    private int ticks;
    private int quiet;
    private int recoveries;

    CraftingSettlement(int pingMillis) {
        quietTicks = Math.max(3, Math.min(40, 2 + 2 * (int) ((Math.max(0L, pingMillis) + 49) / 50)));
    }

    Action tick(boolean changed, boolean blocked, boolean recoverable) {
        if (++ticks > MAX_TICKS) return Action.STOP;
        // A fresh client tick with an empty cursor/grid needs no artificial quiet period.
        // Keep the latency-aware delay only before recovering actual leftovers.
        if (!blocked) return Action.READY;
        quiet = changed ? 0 : quiet + 1;
        if (quiet < quietTicks) return Action.WAIT;
        if (recoverable && recoveries < MAX_RECOVERIES) {
            recoveries++;
            quiet = 0;
            return Action.RECOVER;
        }
        return Action.WAIT;
    }
}
