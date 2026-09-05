package com.benbernard.machineworklist;

import java.util.LinkedHashMap;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import codechicken.nei.NEIServerUtils;
import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.recipe.StackInfo;
import codechicken.nei.util.NBTHelper;

/** Detached NEI node with directional recipe-template matching, independent of the fuzzy GUID cache. */
final class MatchingBookmarkItem extends BookmarkItem {

    private final ItemStack matchingStack;

    MatchingBookmarkItem(BookmarkItem item) {
        super(
            item.groupId,
            item.amount,
            item.fluidCellAmount,
            item.itemStack.copy(),
            new LinkedHashMap<>(),
            item.factor,
            item.recipeId,
            item.type);
        matchingStack = actualStack(item).copy();
        item.permutations.forEach((key, stack) -> permutations.put(key, stack.copy()));
    }

    @Override
    public BookmarkItem copyWithAmount(long amount) {
        MatchingBookmarkItem copy = new MatchingBookmarkItem(this);
        copy.amount = amount;
        return copy;
    }

    @Override
    public boolean containsItems(BookmarkItem other) {
        return type == BookmarkItemType.INGREDIENT ? matches(this, other) : matches(other, this);
    }

    static boolean matches(BookmarkItem requirement, BookmarkItem supply) {
        ItemStack actual = actualStack(supply);
        for (ItemStack template : requirement.permutations.values()) {
            FluidStack requiredFluid = StackInfo.getFluid(template);
            if (requiredFluid != null) {
                FluidStack availableFluid = StackInfo.getFluid(actual);
                if (availableFluid != null && requiredFluid.isFluidEqual(availableFluid)) return true;
            } else if (NEIServerUtils.areStacksSameTypeCrafting(template, actual)
                && NBTHelper.matchTag(template.getTagCompound(), actual.getTagCompound())) return true;
        }
        return false;
    }

    private static ItemStack actualStack(BookmarkItem item) {
        // Fluid accounting temporarily replaces itemStack with an inert token. Matching
        // must continue to use the real fluid/container identity during that calculation.
        return item instanceof MatchingBookmarkItem ? ((MatchingBookmarkItem) item).matchingStack : item.itemStack;
    }
}
