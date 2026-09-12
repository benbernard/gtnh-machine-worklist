package com.benbernard.machineworklist;

import net.minecraft.item.ItemStack;

import codechicken.nei.recipe.StackInfo;

/** Conservative physical-tool reuse budget for one bounded NEI bulk transfer. */
final class CraftingToolUses {

    private CraftingToolUses() {}

    static int available(ItemStack original) {
        if (original.getMaxStackSize() != 1 || !original.getItem()
            .hasContainerItem(original)) return 0;
        boolean paused = StackInfo.isPausedItemDamageSound();
        StackInfo.pauseItemDamageSound(true);
        try {
            ItemStack current = original.copy();
            current.stackSize = 1;
            for (int uses = 1; uses <= CraftingBurst.MAX_BATCHES; uses++) {
                ItemStack returned = current.getItem()
                    .getContainerItem(current.copy());
                if (returned == null || returned.stackSize <= 0
                    || returned.isItemStackDamageable() && returned.getItemDamage() > returned.getMaxDamage())
                    return uses;
                if (returned.getItem() != original.getItem()
                    || original.getHasSubtypes() && returned.getItemDamage() != original.getItemDamage()) {
                    // A filled bucket becoming an empty bucket is consumption, not tool reuse.
                    return uses == 1 ? 0 : uses;
                }
                current = returned.copy();
            }
            return CraftingBurst.MAX_BATCHES;
        } finally {
            StackInfo.pauseItemDamageSound(paused);
        }
    }
}
