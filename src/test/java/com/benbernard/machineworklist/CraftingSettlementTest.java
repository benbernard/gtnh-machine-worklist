package com.benbernard.machineworklist;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CraftingSettlementTest {

    @Test
    public void acknowledgedCleanTransferContinuesImmediately() {
        assertEquals(CraftingSettlement.Action.READY, new CraftingSettlement().tick(false, false));
    }

    @Test
    public void refreshedEmptyMirrorNeedsNoRecovery() {
        assertEquals(CraftingSettlement.Action.READY, new CraftingSettlement().tick(false, true));
    }

    @Test
    public void acknowledgedReturnedToolRecoversWithoutAdditionalQuietTicks() {
        CraftingSettlement settlement = new CraftingSettlement();
        assertEquals(CraftingSettlement.Action.RECOVER, settlement.tick(true, true));
        // The caller waits for this recovery's acknowledgments before observing the grid again.
        assertEquals(CraftingSettlement.Action.READY, settlement.tick(false, false));
    }

    @Test
    public void unknownItemsNeverTriggerRecoveryOrAnotherCraft() {
        CraftingSettlement settlement = new CraftingSettlement();
        for (int i = 0; i < CraftingSettlement.MAX_TICKS; i++)
            assertEquals(CraftingSettlement.Action.WAIT, settlement.tick(true, false));
        assertEquals(CraftingSettlement.Action.STOP, settlement.tick(true, false));
    }

    @Test
    public void lateRecognizedToolDoesNotRestartTheDeadline() {
        CraftingSettlement settlement = new CraftingSettlement();
        for (int i = 1; i < CraftingSettlement.MAX_TICKS; i++)
            assertEquals(CraftingSettlement.Action.WAIT, settlement.tick(true, false));
        assertEquals(CraftingSettlement.Action.RECOVER, settlement.tick(true, true));
        assertEquals(CraftingSettlement.Action.STOP, settlement.tick(true, true));
    }

    @Test
    public void recoveryAttemptsAreBoundedEvenWhenTheToolStaysVisible() {
        CraftingSettlement settlement = new CraftingSettlement();
        int recovered = 0;
        for (int i = 0; i < CraftingSettlement.MAX_TICKS; i++) {
            CraftingSettlement.Action action = settlement.tick(true, true);
            if (action == CraftingSettlement.Action.RECOVER) recovered++;
        }
        assertEquals(2, recovered);
        assertEquals(CraftingSettlement.Action.STOP, settlement.tick(true, true));
    }

    @Test
    public void exhaustedRecoveryStillAcceptsAnEventuallyClearedGrid() {
        CraftingSettlement settlement = new CraftingSettlement();
        assertEquals(CraftingSettlement.Action.RECOVER, settlement.tick(true, true));
        assertEquals(CraftingSettlement.Action.RECOVER, settlement.tick(true, true));
        assertEquals(CraftingSettlement.Action.WAIT, settlement.tick(true, true));
        assertEquals(CraftingSettlement.Action.READY, settlement.tick(false, false));
    }
}
