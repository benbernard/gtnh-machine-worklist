package com.benbernard.machineworklist;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import codechicken.nei.bookmark.BookmarkItem;

/** GTNH's zero-count programmed circuit describes a ghost machine setting, not owned stock. */
final class VirtualInputs {

    private VirtualInputs() {}

    static boolean isCircuitSetting(BookmarkItem input) {
        return input.type == BookmarkItem.BookmarkItemType.INGREDIENT && input.factor == 0
            && isProgrammedCircuit(input.itemStack);
    }

    static boolean isProgrammedCircuit(ItemStack stack) {
        // Exact GTNH 2.8.4 registry identity; keep GregTech's machine registry optional in tests.
        return stack != null
            && "gregtech:gt.integrated_circuit".equals(Item.itemRegistry.getNameForObject(stack.getItem()));
    }
}
