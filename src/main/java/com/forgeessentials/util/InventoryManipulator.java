package com.forgeessentials.util;

import com.sun.istack.internal.NotNull;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.util.*;
import java.util.function.Consumer;

public class InventoryManipulator extends InventoryDecorator implements Iterable<ItemStack>
{
    public InventoryManipulator(IInventory inventory) {
        super(inventory);
    }

    /**
     * @inheritDocs
     */
    @Override
    public Iterator<ItemStack> iterator() {
        return new Itr();
    }

    public int getFreeSlots()
    {
        int total = inventory.getSizeInventory();

        for (ItemStack stack : this)
        {
            if (stack != null)
                total -= 1;
        }

        return total;
    }

    public int getFreeSpaceForItem(ItemStack targetStack)
    {
        int stackLimit = getStackLimit(targetStack);
        int total = inventory.getSizeInventory() * stackLimit;

        for (ItemStack stack : this)
        {
            if (stack != null)
            {
                total -= stack.isItemEqual(targetStack)
                        ? stack.stackSize
                        : stackLimit;
            }
        }

        return total;
    }

    public boolean isFit(ItemStack stack)
    {
        return getFreeSpaceForItem(stack) >= stack.stackSize;
    }

    public boolean pushItemStack(ItemStack pushStack)
    {
        if (!isFit(pushStack))
            return false;

        int stackLimit = getStackLimit(pushStack);

        int i = 0;
        for (ItemStack stack : this)
        {
            if (stack == null)
            {
                inventory.setInventorySlotContents(i, pushStack);
                break;
            }
            else
            {
                if (stack.isItemEqual(pushStack))
                {
                    int amount = Math.min(stackLimit - stack.stackSize, pushStack.stackSize);
                    inventory.setInventorySlotContents(i, pushStack.splitStack(amount));
                }
            }
            ++i;
        }

        return true;
    }

    public int countItems(@NotNull ItemStack searchItemStack)
    {
        int count = 0;

        for (ItemStack slotStack : this)
        {
            if (slotStack != null && slotStack.isItemEqual(searchItemStack))
            {
                count += slotStack.stackSize;
            }
        }

        return count;
    }

    public int getStackLimit(@NotNull ItemStack stack)
    {
        return Math.min(inventory.getInventoryStackLimit(), stack.getMaxStackSize());
    }

    /**
     * An iterator over inventory.
     * Helper class for more convenient iterating over inventory slots.
     *
     * @see Iterator
     * @see IInventory
     */
    protected class Itr implements Iterator<ItemStack>
    {
        private int index = -1;

        /**
         * @inheritDoc
         */
        @Override
        public boolean hasNext()
        {
            return index != inventory.getSizeInventory();
        }

        /**
         * {@inheritDoc}
         * If filter is present
         */
        @Override
        public ItemStack next()
        {
            try
            {
                return inventory.getStackInSlot(++index);
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                throw new NoSuchElementException();
            }
        }

        /**
         * @inheritDoc
         */
        @Override
        public void remove()
        {
            requireIndexLegal();
            inventory.setInventorySlotContents(index, null);
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
         * @return Current index
         */
        public int getIndex()
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
            inventory.setInventorySlotContents(index, itemStack);
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
            if (index == -1)
                throw new IllegalStateException();
        }
    }

}
