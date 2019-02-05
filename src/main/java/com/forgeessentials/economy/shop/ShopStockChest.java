package com.forgeessentials.economy.shop;

import com.forgeessentials.util.InventoryManipulator;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.item.ItemStack;

public class ShopStockChest extends InventoryManipulator
{
    private ItemStack shopItemStack;

    public ShopStockChest(TileEntityChest inventory, ItemStack shopItemStack)
    {
        super(inventory);
        this.shopItemStack = shopItemStack;
    }

    public ShopStockChest(InventoryLargeChest inventory, ItemStack shopItemStack)
    {
        super(inventory);
        this.shopItemStack = shopItemStack;
    }

    public int getAmount()
    {
        return countItems(shopItemStack);
    }

    public boolean add(int amount)
    {
        ItemStack stack = shopItemStack.copy();
        stack.stackSize = amount;

        return putItemStack(shopItemStack);
    }

    public boolean remove(int amount)
    {
        if (getAmount() < amount)
            return false;

        int slot = 0;
        for (ItemStack slotStack : this)
        {
            if (slotStack != null && slotStack.isItemEqual(shopItemStack))
            {
                int decreaseAmount = Math.min(slotStack.stackSize, amount);
                decrStackSize(slot, decreaseAmount);
                amount -= decreaseAmount;
            }

            slot++;
        }

        return true;
    }
}
