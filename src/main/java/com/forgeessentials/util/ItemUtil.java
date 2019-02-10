package com.forgeessentials.util;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;

import com.forgeessentials.commons.selections.WorldPoint;
import com.forgeessentials.util.output.LoggingHandler;

import cpw.mods.fml.common.registry.GameData;

public final class ItemUtil
{

    public static int getItemDamage(ItemStack stack)
    {
        try
        {
            return stack.getItemDamage();
        }
        catch (Exception e)
        {
            if (stack.getItem() == null)
                LoggingHandler.felog.error("ItemStack item is null when checking getItemDamage");
            else
                LoggingHandler.felog.error(String.format("Item %s threw exception on getItemDamage", stack.getItem().getClass().getName()));
            return 0;
        }
    }

    public static String getItemIdentifier(ItemStack itemStack)
    {
        String id = GameData.getItemRegistry().getNameForObject(itemStack.getItem());
        int itemDamage = getItemDamage(itemStack);
        if (itemDamage == 0 || itemDamage == 32767)
            return id;
        else
            return id + ":" + itemDamage;
    }

    public static boolean isItemFrame(EntityHanging entity)
    {
        return entity instanceof EntityItemFrame;
    }

    public static boolean isSign(Block block)
    {
        return block == Blocks.wall_sign;
    }

    public static String[] getSignText(WorldPoint point)
    {
        TileEntity te = point.getTileEntity();
        if (te instanceof TileEntitySign)
        {
            TileEntitySign sign = (TileEntitySign) te;
            return sign.signText;
        }
        return null;
    }

    public static NBTTagCompound getTagCompound(ItemStack itemStack)
    {
        NBTTagCompound tag = itemStack.getTagCompound();
        if (tag == null)
        {
            tag = new NBTTagCompound();
            itemStack.setTagCompound(tag);
        }
        return tag;
    }

    
    public static NBTTagCompound getCompoundTag(NBTTagCompound tag, String side)
    {
        NBTTagCompound subTag = tag.getCompoundTag(side);
        tag.setTag(side, subTag);
        return subTag;
    }

    public static boolean isStackDataEquals(ItemStack stack1, ItemStack stack2)
    {
        if (!stack1.isItemEqual(stack2))
            return false;

        NBTTagCompound comp1 = stack1.getTagCompound();
        NBTTagCompound comp2 = stack2.getTagCompound();

        System.out.println("DEBUG ItemUtil::isStackDataEquals: comp1 = " + comp1);
        System.out.println("DEBUG ItemUtil::isStackDataEquals: comp2 = " + comp2);

        if (comp1 == null)
            return comp2 == null;

        if (comp2 == null)
            return false;

        return comp1.equals(comp2);
    }
}
