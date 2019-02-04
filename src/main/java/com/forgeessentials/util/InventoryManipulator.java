package com.forgeessentials.util;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.util.*;
import java.util.function.Consumer;

/**
 * Decorator that expands the possibilities for inventory operations and
 * implements {@link Iterable} of type {@link ItemStack} so object of this
 * type can be iterated:
 *
 * <pre>
 * TileEntityChest tileChest;
 * ...
 * InventoryManipulator chest = new InventoryManipulator(tileChest);
 * for (ItemStack stack : chest) {
 *     // Iterates over all slots in the inventory
 * }
 * </pre>
 *
 * @see InventoryDecorator
 * @see Iterable
 */
public class InventoryManipulator extends InventoryDecorator implements Iterable<ItemStack>
{
    /**
     * Basic constructor encapsulating the inventory object
     *
     * @param inventory target inventory
     */
    public InventoryManipulator(IInventory inventory) {
        super(inventory);
    }

    /**
     * Returns an iterator over inventory of type {@link ItemStack}
     *
     * @see Iterator
     */
    @Override
    public Iterator<ItemStack> iterator() {
        return new Itr();
    }

    /**
     * Returns a count of empty slots in the inventory
     *
     * @return free slots count
     */
    public int getEmptySlotsCount()
    {
        int total = getSizeInventory();

        for (ItemStack stack : this)
        {
            if (stack != null)
                total -= 1;
        }

        return total;
    }

    /**
     * Calculate how much items of given type can be putted in
     * the inventory. Counts free slots as max stack size
     * and adds a remaining space in not full stacks of the same type.
     *
     * @param targetStack ItemStack to put
     * @return count of items can be putted.
     */
    public int getFreeSpaceForItem(ItemStack targetStack)
    {
        int stackLimit = getStackLimit(targetStack);
        int total = getSizeInventory() * stackLimit;

        for (ItemStack slotStack : this)
        {
            if (slotStack != null)
            {
                total -= slotStack.isItemEqual(targetStack)
                        ? slotStack.stackSize
                        : stackLimit;
            }
        }

        return total;
    }

    /**
     * Checks whether inventory has enough space for target stack or not.
     * Counts free slots (adds max stack size per slot) and remaining space in
     * not full stacks of the same type.
     *
     * @param stack stack to check
     * @return {@code} true if the inventory has enough space.
     */
    public boolean isFit(ItemStack stack)
    {
        return getFreeSpaceForItem(stack) >= stack.stackSize;
    }

    /**
     * Puts item stack into the inventory in first a empty slot or first not
     * full stacks of the same type of item.
     *
     * @param stack stack to put
     * @return {@code true} if operation success or {@code false} if not
     *         enough free space.
     */
    public boolean putItemStack(ItemStack stack)
    {
        if (!isFit(stack))
            return false;

        int stackLimit = getStackLimit(stack);

        int i = 0;
        for (ItemStack slotStack : this)
        {
            if (slotStack == null)
            {
                setInventorySlotContents(i, stack);
                break;
            }
            else
            {
                if (slotStack.isItemEqual(stack))
                {
                    int amount = Math.min(stackLimit - slotStack.stackSize, stack.stackSize);
                    setInventorySlotContents(i, stack.splitStack(amount));
                }
            }
            ++i;
        }

        return true;
    }

    /**
     * Counts an items of given type in the inventory.
     *
     * @param stack Item stack to count (damage )
     * @return items count in all stacks in the inventory
     */
    public int countItems(ItemStack stack)
    {
        int count = 0;

        for (ItemStack slotStack : this)
        {
            if (slotStack != null && slotStack.isItemEqual(stack))
            {
                count += slotStack.stackSize;
            }
        }

        return count;
    }

    /**
     * Returns a minimum stack limit for a given stack in the current inventory.
     *
     * @param stack Target item stack
     * @return stack limit
     */
    public int getStackLimit(ItemStack stack)
    {
        return Math.min(getInventoryStackLimit(), stack.getMaxStackSize());
    }

    /**
     * An iterator over inventory.
     */
    protected class Itr implements Iterator<ItemStack>
    {
        /**
         * Current slot index
         */
        private int slotIndex = -1;

        @Override
        public boolean hasNext()
        {
            return slotIndex != getSizeInventory();
        }

        @Override
        public ItemStack next()
        {
            try
            {
                return getStackInSlot(++slotIndex);
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove()
        {
            requireIndexLegal();
            setInventorySlotContents(slotIndex, null);
        }

        /**
         * {@inheritDoc}
         * <p>This implementation also checks for a possible null returned from {@code next}</p>
         */
        @Override
        public void forEachRemaining(Consumer<? super ItemStack> action) {
            Objects.requireNonNull(action);
            while (hasNext())
            {
                ItemStack stack = next();

                if (stack != null) {
                    action.accept(next());
                }
            }
        }

        /**
         * Return inventory slot index on current iteration
         *
         * @return current slot index
         */
        public int getIndex()
        {
            return slotIndex;
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
            setInventorySlotContents(slotIndex, itemStack);
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
            if (slotIndex == -1)
                throw new IllegalStateException();
        }
    }

}
