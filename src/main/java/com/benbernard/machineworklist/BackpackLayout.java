package com.benbernard.machineworklist;

/** Adventure Backpack 1.3.13-GTNH: 36 player slots, 48 storage, 6 utility, 9 mirrors, result. */
final class BackpackLayout {

    static boolean storage(int slot) {
        return slot >= 36 && slot < 84;
    }

    static boolean craftingStorage(int slot) {
        if (!storage(slot)) return false;
        int index = slot - 36;
        return index / 8 >= 3 && index % 8 >= 5;
    }

    private BackpackLayout() {}
}
