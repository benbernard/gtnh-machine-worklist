package com.benbernard.machineworklist;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CraftingSettlementTest {

    @Test
    public void cleanTransferContinuesOnItsFirstObservationAtAnyLatency() {
        for (int ping : new int[] { 0, 100, 500, Integer.MAX_VALUE }) {
            CraftingSettlement settlement = new CraftingSettlement(ping);
            assertEquals(CraftingSettlement.Action.READY, settlement.tick(true, false, false));
        }
    }

    @Test
    public void lateToolMirrorResetsTheQuietPeriod() {
        CraftingSettlement settlement = new CraftingSettlement(0);
        settlement.tick(true, true, true);
        settlement.tick(false, true, true);
        assertEquals(CraftingSettlement.Action.WAIT, settlement.tick(true, true, true));
        settlement.tick(false, true, true);
        assertEquals(CraftingSettlement.Action.READY, settlement.tick(true, false, false));
    }

    @Test
    public void returnedToolRecoversThenContinuesWhenTheGridClears() {
        CraftingSettlement settlement = new CraftingSettlement(0);
        settlement.tick(false, true, true);
        settlement.tick(false, true, true);
        assertEquals(CraftingSettlement.Action.RECOVER, settlement.tick(false, true, true));
        assertEquals(CraftingSettlement.Action.READY, settlement.tick(true, false, false));
    }

    @Test
    public void unknownItemsNeverTriggerRecoveryOrAnotherCraft() {
        CraftingSettlement settlement = new CraftingSettlement(0);
        for (int i = 0; i < CraftingSettlement.MAX_TICKS; i++)
            assertEquals(CraftingSettlement.Action.WAIT, settlement.tick(false, true, false));
        assertEquals(CraftingSettlement.Action.STOP, settlement.tick(false, true, false));
    }

    @Test
    public void repeatedInventoryChangesCannotExtendTheDeadline() {
        CraftingSettlement settlement = new CraftingSettlement(0);
        for (int i = 0; i < CraftingSettlement.MAX_TICKS; i++)
            assertEquals(CraftingSettlement.Action.WAIT, settlement.tick(true, true, true));
        assertEquals(CraftingSettlement.Action.STOP, settlement.tick(true, true, true));
    }

    @Test
    public void recoveryAttemptsAreBoundedEvenWhenTheToolStaysVisible() {
        CraftingSettlement settlement = new CraftingSettlement(0);
        int recovered = 0;
        for (int i = 0; i < CraftingSettlement.MAX_TICKS; i++) {
            CraftingSettlement.Action action = settlement.tick(false, true, true);
            if (action == CraftingSettlement.Action.RECOVER) recovered++;
        }
        assertEquals(2, recovered);
        assertEquals(CraftingSettlement.Action.STOP, settlement.tick(false, true, true));
    }

    @Test
    public void latencyAddsWaitButCannotOverflowOrMakeItUnbounded() {
        for (int ping : new int[] { 0, 100, 500, Integer.MAX_VALUE }) {
            CraftingSettlement settlement = new CraftingSettlement(ping);
            int expected = ping == 0 ? 3 : ping == 100 ? 6 : ping == 500 ? 22 : 40;
            for (int i = 1; i < expected; i++)
                assertEquals(CraftingSettlement.Action.WAIT, settlement.tick(false, true, true));
            assertEquals(CraftingSettlement.Action.RECOVER, settlement.tick(false, true, true));
        }
    }
}
