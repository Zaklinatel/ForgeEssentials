package com.forgeessentials.util;
import com.sun.istack.internal.NotNull;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

public class InventoryManipulator implements Iterable<ItemStack> {
    private IInventory inventory;
    private InventoryIterator iterator;
    private Predicate<ItemStack> iteratorFilter;

    public InventoryManipulator(@NotNull IInventory inventory) {
        this.inventory = inventory;
    }

    public void setIteratorFilter(Predicate<ItemStack> filter) {
        iteratorFilter = filter;
    }

    /**
     * @inheritDocs
     */
    @Override
    public InventoryIterator iterator() {
        return iteratorFilter == null
                ? new InventoryIterator(inventory)
                : new InventoryIterator(inventory, iteratorFilter);
    }
}
