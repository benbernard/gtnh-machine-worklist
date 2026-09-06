package com.benbernard.machineworklist;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class BackpackLayoutTest {

    @Test
    public void gridIsBottomRightNineStorageSlots() {
        List<Integer> grid = new ArrayList<>();
        for (int i = 0; i < 100; i++) if (BackpackLayout.craftingStorage(i)) grid.add(i);
        assertEquals(Arrays.asList(65, 66, 67, 73, 74, 75, 81, 82, 83), grid);
    }

    @Test
    public void supplyExcludesPlayerDuplicatesUtilityMirrorsAndResult() {
        int count = 0;
        for (int i = 0; i < 100; i++) if (BackpackLayout.storage(i)) count++;
        assertEquals(48, count);
        for (int i : new int[] { -1, 0, 35, 84, 89, 90, 98, 99, 100 }) assertFalse(BackpackLayout.storage(i));
    }
}
