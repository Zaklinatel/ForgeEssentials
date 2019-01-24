package com.forgeessentials.economy.shop;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.forgeessentials.api.UserIdent;
import com.forgeessentials.util.output.ChatOutputHandler;
import com.forgeessentials.util.output.LoggingHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.forgeessentials.commons.selections.WorldPoint;
import com.forgeessentials.core.misc.Translator;
import com.forgeessentials.util.ItemUtil;
import com.forgeessentials.util.ServerUtil;
import com.google.gson.annotations.Expose;

public class ShopData
{

    public static final Pattern PATTERN_BUY = Pattern.compile("buy\\s+(?:for\\s+)?(\\d+)");

    public static final Pattern PATTERN_SELL = Pattern.compile("sell\\s+(?:for\\s+)?(\\d+)");

    public static final Pattern PATTERN_AMOUNT = Pattern.compile("amount\\s+(\\d+)");

    /* ------------------------------------------------------------ */

    protected final WorldPoint pos;

    protected final UUID itemFrameId;

    protected boolean useChestStock = false;

    protected final UUID owner;

    @Expose(serialize = false, deserialize = false)
    protected WeakReference<EntityItemFrame> itemFrame;

    @Expose(serialize = false, deserialize = false)
    protected WeakReference<TileEntityChest> chest;

    @Expose(serialize = false, deserialize = false)
    protected boolean isValid;

    @Expose(serialize = false, deserialize = false)
    protected int buyPrice = -1;

    @Expose(serialize = false, deserialize = false)
    protected int sellPrice = -1;

    @Expose(serialize = false, deserialize = false)
    protected int amount = 1;

    @Expose(serialize = false, deserialize = false)
    protected String error;

    @Expose(serialize = false, deserialize = false)
    protected ItemStack item;

    private int stock;

    /* ------------------------------------------------------------ */

    public ShopData(UUID ownerUUID, WorldPoint point, EntityItemFrame frame, TileEntityChest entityChest)
    {
        pos = point;
        itemFrameId = frame.getPersistentID();
        itemFrame = new WeakReference<EntityItemFrame>(frame);
        owner = ownerUUID;

        if (entityChest != null) {
            chest = new WeakReference<TileEntityChest>(entityChest);
            useChestStock = true;
        }
    }

    public void update()
    {
        isValid = false;
        error = null;
        item = null;

        // if (!ItemUtil.isSign(signPosition.getBlock())) return;
        String[] text = ItemUtil.getSignText(pos);
        if (text == null || text.length < 2 || !ShopManager.shopTags.contains(text[0]))
        {
            error = Translator.translate("Sign header missing");
            return;
        }

        EntityItemFrame frame = getItemFrame();
        if (frame == null)
        {
            error = Translator.translate("Item frame missing");
            return;
        }

        item = frame.getDisplayedItem();
        if (item == null)
        {
            error = Translator.translate("Item frame empty");
            return;
        }

        if (item.isItemDamaged())
        {
            error = Translator.translate("You can not sell or by damaged items");
            return;
        }

        if (useChestStock)
        {
            TileEntityChest chest = getChest();

            if (chest == null) {
                error = Translator.translate("This shop needs a stock chest, but it not found");
                return;
            }
        }

        buyPrice = -1;
        sellPrice = -1;
        amount = 1;
        for (int i = 1; i < text.length; i++)
        {
            Matcher matcher = PATTERN_BUY.matcher(text[i]);
            if (matcher.matches())
            {
                if (buyPrice != -1)
                {
                    error = Translator.translate("Buy price specified twice");
                    return;
                }
                buyPrice = ServerUtil.parseIntDefault(matcher.group(1), -1);
                continue;
            }
            matcher = PATTERN_SELL.matcher(text[i]);
            if (matcher.matches())
            {
                if (sellPrice != -1)
                {
                    error = Translator.translate("Sell price specified twice");
                    return;
                }
                sellPrice = ServerUtil.parseIntDefault(matcher.group(1), -1);
                continue;
            }
            matcher = PATTERN_AMOUNT.matcher(text[i]);
            if (matcher.matches())
            {
                if (amount != 1)
                {
                    error = Translator.translate("Amount specified twice");
                    return;
                }
                amount = ServerUtil.parseIntDefault(matcher.group(1), 1);
                continue;
            }
        }

        if (buyPrice == -1 && sellPrice == -1)
        {
            error = Translator.translate("No price specified");
            return;
        }
        if (amount < 1)
        {
            error = Translator.translate("Amount smaller than 1");
            return;
        }

        isValid = true;
    }

    public ItemStack getItemStack()
    {
        if (!isValid) {
            return null;
        }

        ItemStack itemStackCopy = item.copy();
        item.stackSize = amount;

        return itemStackCopy;
    }

    public WorldPoint getSignPosition()
    {
        return pos;
    }

    public String getError()
    {
        return error;
    }

    public TileEntityChest getChest() {
        if (chest == null) {
            TileEntityChest chestEntity = findChest(new WorldPoint(pos).setY(pos.getY() - 1));

            if (chestEntity != null) {
                chest = new WeakReference<TileEntityChest>(chestEntity);
            }
        }

        return chest.get();
    }

    public EntityItemFrame getItemFrame()
    {
        EntityItemFrame frame = itemFrame == null ? null : itemFrame.get();
        if (frame == null)
        {
            List<EntityItemFrame> entities = getEntitiesWithinAABB(pos.getWorld(), EntityItemFrame.class, getSignAABB(pos));
            for (EntityItemFrame entityItemFrame : entities)
            {
                if (entityItemFrame.getPersistentID().equals(itemFrameId))
                {
                    frame = entityItemFrame;
                    itemFrame = new WeakReference<EntityItemFrame>(frame);
                    break;
                }
            }
        }
        return frame;
    }

    public static EntityItemFrame findFrame(WorldPoint p)
    {
        AxisAlignedBB aabb = getSignAABB(p);
        List<EntityItemFrame> entities = getEntitiesWithinAABB(p.getWorld(), EntityItemFrame.class, aabb);
        if (entities.isEmpty())
            return null;
        if (entities.size() == 1)
            return entities.get(0);

        final Vec3 offset = Vec3.createVectorHelper(p.getX(), p.getY() + 0.5, p.getZ());
        Collections.sort(entities, new Comparator<EntityItemFrame>() {
            @Override
            public int compare(EntityItemFrame o1, EntityItemFrame o2)
            {
                Vec3 v1 = Vec3.createVectorHelper(o1.posX, o1.posY, o1.posZ);
                Vec3 v2 = Vec3.createVectorHelper(o2.posX, o2.posY, o2.posZ);
                return (int) Math.signum(offset.distanceTo(v1) - offset.distanceTo(v2));
            }
        });

        for (Iterator<EntityItemFrame> it = entities.iterator(); it.hasNext();)
        {
            if (entities.size() == 1)
                break;
            if (ShopManager.shopFrameMap.containsKey(it.next().getPersistentID()))
                it.remove();
        }
        return entities.get(0);
    }

    public static TileEntityChest findChest(WorldPoint signPoint)
    {
        WorldPoint tilePoint = new WorldPoint(signPoint).setY(signPoint.getY() - 1);
        TileEntity tileEntity = tilePoint.getTileEntity();
        TileEntityChest chest;

        // Check block is a chest
        if (tileEntity != null && tileEntity.getBlockType() == Block.getBlockById(54)) {
            chest = (TileEntityChest) tileEntity;
        } else {
            chest = null;
        }

        return chest;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getEntitiesWithinAABB(World world, Class<? extends T> clazz, AxisAlignedBB aabb)
    {
        return world.getEntitiesWithinAABB(clazz, aabb);
    }

    public static AxisAlignedBB getSignAABB(WorldPoint p)
    {
        double x = p.getX();
        double y = p.getY() + 0.5;
        double z = p.getZ();
        double D = 1.4;
        AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(x - D, y - D, z - D, x + D, y + D, z + D);
        return aabb;
    }

    public int getStock()
    {
        if (!useChestStock)
            return stock;

        return 0; // todo get stock from chest
    }

    public void setStock(int stock)
    {
        if (!useChestStock)
            this.stock = stock;

        // todo set items count in chest
    }

}
