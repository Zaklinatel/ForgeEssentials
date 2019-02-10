package com.forgeessentials.economy.shop;

import com.forgeessentials.util.InventoryManipulator;
import com.forgeessentials.util.ItemUtil;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.item.ItemStack;

public class ShopStockChest extends InventoryManipulator
{
    private ItemStack shopItemStack;

    public ShopStockChest(TileEntityChest inventory, ItemStack shopItemStack)
    {
        super(inventory);
        this.shopItemStack = shopItemStack.copy();
    }

    public ShopStockChest(InventoryLargeChest inventory, ItemStack shopItemStack)
    {
        super(inventory);
        this.shopItemStack = shopItemStack.copy();
    }

    public int getAmount()
    {
        return countItems(shopItemStack);
    }

    public boolean add(int amount)
    {
        ItemStack stack = shopItemStack.copy();
        stack.stackSize = amount;

        return putItemStack(stack);
    }

    public boolean remove(int amount)
    {
        if (getAmount() < amount)
            return false;

        int slot = 0;
        for (ItemStack slotStack : this)
        {
            if (slotStack != null && ItemUtil.isStackDataEquals(slotStack, shopItemStack))
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
