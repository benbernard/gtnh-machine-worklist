package com.benbernard.machineworklist;

/** Called only after all transfer clicks are acknowledged and the client has applied inventory updates. */
final class CraftingSettlement {

    enum Action {
        WAIT,
        RECOVER,
        READY,
        STOP
    }

    static final int MAX_TICKS = 200;
    static final int MAX_RECOVERIES = 2;
    private int ticks;
    private int recoveries;

    Action tick(boolean blocked, boolean recoverable) {
        if (++ticks > MAX_TICKS) return Action.STOP;
        if (!blocked) return Action.READY;
        // The transaction gate, not a ping estimate or unrelated inventory changes, guards recovery.
        if (recoverable && recoveries < MAX_RECOVERIES) {
            recoveries++;
            return Action.RECOVER;
        }
        return Action.WAIT;
    }
}
