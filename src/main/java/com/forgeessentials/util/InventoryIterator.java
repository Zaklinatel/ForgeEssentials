package com.forgeessentials.util;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * An iterator over inventory.
 * Helper class for a more convenient iterating of inventory slots and finding
 * item stacks in it by a filter.
 *
 * @author Ivan Sennov @Zaklinatel
 * @see Iterator
 * @see IInventory
 */
public class InventoryIterator implements Iterator<ItemStack>
{
    /**
     * Iterable inventory
     */
    private final IInventory inventory;

    /**
     * Condition used to filter {@link #next} call result. If predicate returns
     * false, {@code next} will continue to iterate over inventory's slots.
     */
    private final Predicate<? super ItemStack> filter;
    private int index = 0;

    /**
     * Last returned slot index
     */
    private int lastRet = -1;

    /**
     * Creates a simple iterator which iterate over whole inventory.
     * @param inventory Inventory to iterate
     */
    public InventoryIterator(IInventory inventory)
    {
        this.inventory = inventory;
        this.filter = null;
    }

    /**
     * Creates iterator with a filter. Calling {@code next} returns only stacks
     * passed through {@code filter.test}. If predicate test returns a false,
     * @param inventory Inventory to iterate
     * @param filter Filter predicate. On each {@code next} call if
     *               {@code Predicate#test} returns a false,
     */
    public InventoryIterator(IInventory inventory, Predicate<? super ItemStack> filter)
    {
        this.inventory = inventory;
        this.filter = filter;
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean hasNext()
    {
        return index != inventory.getSizeInventory();
    }

    /**
     * @inheritDoc
     * If filter is present
     */
    @Override
    public ItemStack next()
    {
        while (hasNext())
        {
            lastRet = index;
            ItemStack stack = inventory.getStackInSlot(index++);

            if (filter == null || filter.test(stack))
            {
                return stack;
            }
        }

        if (filter == null)
        {
            throw new NoSuchElementException();
        }

        return null;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void remove()
    {
        requireIndexLegal();
        inventory.setInventorySlotContents(lastRet, null);
        lastRet = -1;
    }

    /**
     * Returns the index of the element that would be returned by a
     * subsequent call to {@link #next}. (Returns list size if the list
     * iterator is at the end of the list.)
     *
     * @return the index of the element that would be returned by a
     *         subsequent call to {@code next}, or list size if the list
     *         iterator is at the end of the list
     */
    public int nextIndex()
    {
        return index;
    }

    /**
     * Replaces the last element returned by {@link #next} with the specified
     * element (optional operation).
     * This call can be made only if {@link #remove} have not been called
     * after the last call to {@code next}.
     *
     * @param itemStack the item stack with which to replace the last element
     *                  returned by {@code next}
     *
     * @throws IllegalStateException if {@code next} have not been called, or
     *         {@code remove} have been called after the last call to {@code next}
     */
    public void set(ItemStack itemStack)
    {
        requireIndexLegal();
        inventory.setInventorySlotContents(lastRet, itemStack);
    }

    /**
     * Returns a filter condition predicate wrapped in {@link Optional} since the
     * predicate may be null.
     *
     * @return Optional contains {@code predicate} or {@code null}
     *
     * @see Optional
     * @see Predicate
     */
    public Optional<Predicate<? super ItemStack>> getFilter()
    {
        return Optional.ofNullable(filter);
    }

    /**
     * Requires {@code next} have been called or {@code remove} have not been
     * called after last call to {@code next}.
     *
     * @throws IllegalStateException if {@code next} have not been called, or
     *         {@code remove} have been called after the last call to {@code next}
     */
    private void requireIndexLegal()
    {
        if (this.lastRet == -1)
            throw new IllegalStateException();
    }
}
