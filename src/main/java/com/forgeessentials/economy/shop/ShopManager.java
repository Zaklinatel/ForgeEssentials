package com.forgeessentials.economy.shop;

import static net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_AIR;

import java.io.File;
import java.lang.reflect.Type;
import java.sql.SQLOutput;
import java.util.*;

import com.forgeessentials.commands.util.PlayerInvChest;
import com.forgeessentials.util.UserIdentUtils;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.fe.event.entity.EntityAttackedEvent;
import net.minecraftforge.permission.PermissionLevel;

import com.forgeessentials.api.APIRegistry;
import com.forgeessentials.api.UserIdent;
import com.forgeessentials.api.economy.Wallet;
import com.forgeessentials.commons.selections.WorldPoint;
import com.forgeessentials.core.ForgeEssentials;
import com.forgeessentials.core.misc.TaskRegistry;
import com.forgeessentials.core.misc.Translator;
import com.forgeessentials.core.moduleLauncher.config.ConfigLoader;
import com.forgeessentials.data.v2.DataManager;
import com.forgeessentials.economy.ModuleEconomy;
import com.forgeessentials.protection.ProtectionEventHandler;
import com.forgeessentials.util.ItemUtil;
import com.forgeessentials.util.PlayerUtil;
import com.forgeessentials.util.events.FEModuleEvent.FEModuleServerInitEvent;
import com.forgeessentials.util.events.FEModuleEvent.FEModuleServerStoppedEvent;
import com.forgeessentials.util.events.ServerEventHandler;
import com.forgeessentials.util.output.ChatOutputHandler;
import com.forgeessentials.util.output.LoggingHandler;
import com.google.common.reflect.TypeToken;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ShopManager extends ServerEventHandler implements ConfigLoader
{

    public static final String PERM_BASE = ModuleEconomy.PERM + ".shop";

    public static final String PERM_BASE_ADMIN = PERM_BASE + ".admin";
    public static final String PERM_ADMIN_CREATE = PERM_BASE_ADMIN + ".create";
    public static final String PERM_ADMIN_DESTROY = PERM_BASE_ADMIN + ".destroy";
    public static final String PERM_ADMIN_USE = PERM_BASE_ADMIN + ".use";

    public static final String PERM_BASE_PLAYER = PERM_BASE + ".player";
    public static final String PERM_PLAYER_CREATE = PERM_BASE_PLAYER + ".create";
    public static final String PERM_PLAYER_DESTROY = PERM_BASE_PLAYER + ".destroy";
    public static final String PERM_PLAYER_DESTROY_ANY = PERM_PLAYER_DESTROY + ".any";
    public static final String PERM_PLAYER_USE = PERM_BASE_PLAYER + ".use";

    public static final String MSG_MODIFY_DENIED = "You are not allowed to modify this shop!";
    public static final String STOCK_HELP = "If disabled, shops have an infinite stock. Otherwise players can only buy items, that have been sold to the shop before";

    public static final String CONFIG_FILE = "EconomyConfig";

    public static final Set<String> shopTags = new HashSet<String>();

    public static boolean virtualStock;

    protected static Set<ShopData> shops = new HashSet<ShopData>();

    protected static Map<WorldPoint, ShopData> shopSignMap = new WeakHashMap<WorldPoint, ShopData>();

    protected static Map<UUID, ShopData> shopFrameMap = new WeakHashMap<UUID, ShopData>();

    /* ------------------------------------------------------------ */
    /* Data */

    public ShopManager()
    {
        shopTags.add("[FEShop]");
        ForgeEssentials.getConfigManager().registerLoader(CONFIG_FILE, this);
    }

    @Override
    @SubscribeEvent
    public void serverStopped(FEModuleServerStoppedEvent event)
    {
        super.serverStopped(event);
        save();
    }

    @SubscribeEvent
    public void serverStarting(FEModuleServerInitEvent event)
    {
        load();
        APIRegistry.perms.registerPermissionDescription(PERM_BASE, "Shop permissions");
        APIRegistry.perms.registerPermissionDescription(PERM_BASE_ADMIN, "Admin shop permissions");
        APIRegistry.perms.registerPermissionDescription(PERM_BASE_PLAYER, "Player shop permissions");

        APIRegistry.perms.registerPermission(PERM_ADMIN_USE, PermissionLevel.TRUE, "Allow usage of admin shops");
        APIRegistry.perms.registerPermission(PERM_ADMIN_CREATE, PermissionLevel.OP, "Allow creating admin shops");
        APIRegistry.perms.registerPermission(PERM_ADMIN_DESTROY, PermissionLevel.OP, "Allow destroying admin shops");

        APIRegistry.perms.registerPermission(PERM_PLAYER_USE, PermissionLevel.TRUE, "Allow usage of player shops");
        APIRegistry.perms.registerPermission(PERM_PLAYER_CREATE, PermissionLevel.TRUE, "Allow to create player shops with stock chest");
        APIRegistry.perms.registerPermission(PERM_PLAYER_DESTROY, PermissionLevel.TRUE, "Allow players destroying its own shops");
        APIRegistry.perms.registerPermission(PERM_PLAYER_DESTROY_ANY, PermissionLevel.OP, "Allow destroying other's player shops");
    }

    public static File getSaveFile()
    {
        return new File(DataManager.getInstance().getBasePath(), "shops.json");
    }

    public static void save()
    {
        DataManager.save(shops, getSaveFile());
    }

    public static void load()
    {
        shops.clear();
        shopSignMap.clear();

        Type type = new TypeToken<List<ShopData>>() {
        }.getType();

        List<ShopData> shopList = DataManager.load(type, getSaveFile());

        if (shopList == null)
            return;

        for (ShopData shop : shopList)
            addShop(shop);
    }

    /* ------------------------------------------------------------ */

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void breakEvent(BreakEvent event)
    {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient())
            return;

        WorldPoint point = new WorldPoint(event);
        ShopData shop = getShop(point, event.getPlayer());

        if (shop == null)
            return;

        UserIdent ident = UserIdent.get(event.getPlayer());

        if (!checkDestroyPermission(ident, point, shop))
        {
            event.setCanceled(true);

            TileEntity te = event.world.getTileEntity(event.x, event.y, event.z);
            if (te != null)
                ProtectionEventHandler.updateBrokenTileEntity((EntityPlayerMP) event.getPlayer(), te);

            ChatOutputHandler.chatError(event.getPlayer(), Translator.translate(MSG_MODIFY_DENIED));

            return;
        }

        removeShop(shop);
        ChatOutputHandler.chatNotification(event.getPlayer(), Translator.translate("Shop destroyed"));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void entityAttackedEvent(EntityAttackedEvent event)
    {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient() || !(event.entity instanceof EntityItemFrame))
            return;

        final ShopData shop = shopFrameMap.get(event.entity.getPersistentID());

        if (shop == null)
            return;

        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void attackEntityEvent(final AttackEntityEvent event)
    {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient() || !(event.target instanceof EntityItemFrame))
            return;

        final ShopData shop = shopFrameMap.get(event.target.getPersistentID());

        if (shop == null)
            return;

        UserIdent ident = UserIdent.get(event.entityPlayer);
        WorldPoint point = new WorldPoint(event.target);

        if (!checkDestroyPermission(ident, point, shop))
        {
            event.setCanceled(true);
            ChatOutputHandler.chatError(event.entityPlayer, Translator.translate(MSG_MODIFY_DENIED));
            return;
        }

        TaskRegistry.runLater(() -> {
            shop.update();
            if (!shop.isValid)
            {
                removeShop(shop);
                ChatOutputHandler.chatNotification(event.entityPlayer, Translator.translate("Shop destroyed"));
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void entityInteractEvent(EntityInteractEvent event)
    {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient())
            return;

        ShopData shop = shopFrameMap.get(event.target.getPersistentID());

        if (shop == null)
            return;

        if (!shop.owner.equals(UserIdent.get(event.entityPlayer).getOrGenerateUuid()))
        {
            ChatComponentTranslation msg = new ChatComponentTranslation(
                    "Owner: %s, item: %s",
                    UserIdent.get(shop.owner).getUsername(),
                    shop.item.func_151000_E()
            );

            ChatOutputHandler.sendMessage(event.entityPlayer, msg);

            event.setCanceled(true);
        }

    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void playerInteractEvent(final PlayerInteractEvent event)
    {
        if (event.action == Action.LEFT_CLICK_BLOCK || FMLCommonHandler.instance().getEffectiveSide().isClient())
            return;

        ItemStack equippedStack = event.entityPlayer.getCurrentEquippedItem();
        Item equippedItem = equippedStack != null ? equippedStack.getItem() : null;

        WorldPoint point;
        if (event.action == RIGHT_CLICK_AIR)
        {
            if (!(equippedItem instanceof ItemBlock))
                return;

            MovingObjectPosition mop = PlayerUtil.getPlayerLookingSpot(event.entityPlayer);

            if (mop == null)
                return;

            point = new WorldPoint(event.world, mop.blockX, mop.blockY, mop.blockZ);
        }
        else
        {
            point = new WorldPoint(event.world, event.x, event.y, event.z);
        }

        Block block = event.world.getBlock(event.x, event.y, event.z);

        if (!ItemUtil.isSign(block))
            return;

        UserIdent ident = UserIdent.get(event.entityPlayer);
        ShopData shop = shopSignMap.get(point);
        boolean newShop = shop == null;

        if (newShop)
        {
            String[] text = ItemUtil.getSignText(point);
            LoggingHandler.felog.info("DEBUG text = " + text + " / type of text = " + text[0].getClass().getName());

            if (!shopTags.isEmpty())
                LoggingHandler.felog.info("DEBUG shopTags(0) type = " + shopTags.iterator().next());

            if (text == null || text.length == 0|| !shopTags.contains(text[0]))
                return;

            EntityItemFrame frame = ShopData.findFrame(point);
            if (frame == null)
            {
                ChatOutputHandler.chatError(event.entityPlayer, Translator.translate("No item frame found"));
                return;
            }

            if (frame.getDisplayedItem() == null)
            {
                ChatOutputHandler.chatError(event.entityPlayer, Translator.translate("Item frame empty"));
                return;
            }

            if (shopFrameMap.containsKey(frame.getPersistentID()))
            {
                ChatOutputHandler.chatError(event.entityPlayer, Translator.translate("Item frame already used for another shop!"));
                return;
            }

            TileEntityChest chest = ShopData.findChest(point);
            if (chest == null)
            {
                // Check player can create admin shops
                if (!APIRegistry.perms.checkUserPermission(ident, point, PERM_ADMIN_CREATE))
                {
                    ChatOutputHandler.chatError(event.entityPlayer, Translator.translate("You are not allowed to create admin shops!"));
                    return;
                }

                shop = new ShopData(ident.getOrGenerateUuid(), point, frame, false);
            }
            else
            {
                // Check player can create owned shops
                if (!APIRegistry.perms.checkUserPermission(ident, point, PERM_PLAYER_CREATE))
                {
                    ChatOutputHandler.chatError(event.entityPlayer, Translator.translate("You are not allowed to create your own shops!"));
                    return;
                }

                shop = new ShopData(ident.getOrGenerateUuid(), point, frame, true);
            }
        }

        shop.update();
        event.setCanceled(true);

        if (!shop.isValid)
        {
            ChatOutputHandler.chatError(event.entityPlayer, Translator.format("Shop invalid: %s", shop.getError()));

            if (!newShop)
                removeShop(shop);

            return;
        }

        if (newShop)
        {
            ChatOutputHandler.chatConfirmation(event.entityPlayer, Translator.translate(shop.useStockChest ? "Created shop with stock!" : "Created shop!"));
            addShop(shop);
            return;
        }

        if (shop.useStockChest)
        {
            if (!APIRegistry.perms.checkUserPermission(ident, point, PERM_ADMIN_USE))
            {
                ChatOutputHandler.chatError(event.entityPlayer, Translator.translate("You are not allowed to use admin shops!"));
                return;
            }
        }
        else
        {
            if (!APIRegistry.perms.checkUserPermission(ident, point, PERM_PLAYER_USE))
            {
                ChatOutputHandler.chatError(event.entityPlayer, Translator.translate("You are not allowed to use owned shops!"));
                return;
            }
        }

        ItemStack transactionStack = shop.getItemStack();
        boolean sameItem = transactionStack.getItem() == equippedItem;
        transactionStack.stackSize = shop.amount;
        IChatComponent itemName = transactionStack.func_151000_E();

        Wallet buyerWallet = APIRegistry.economy.getWallet(UserIdent.get((EntityPlayerMP) event.entityPlayer));
        Wallet ownerWallet = APIRegistry.economy.getWallet(UserIdent.get(shop.owner));

        if (shop.sellPrice >= 0 && (shop.buyPrice < 0 || sameItem)) // Buy from player action
        {
            if (!ItemUtil.isStackDataEquals(transactionStack, equippedStack))
            {
                ChatComponentTranslation msg = new ChatComponentTranslation("Meta-data of your items is not equals to shop item");
                msg.getChatStyle().setColor(ChatOutputHandler.chatConfirmationColor);
                ChatOutputHandler.sendMessage(event.entityPlayer, msg);
                return;
            }

            if (ModuleEconomy.countInventoryItems(event.entityPlayer, transactionStack) < transactionStack.stackSize)
            {
                ChatComponentTranslation msg = new ChatComponentTranslation("You do not have enough %s", itemName);
                msg.getChatStyle().setColor(ChatOutputHandler.chatErrorColor);
                ChatOutputHandler.sendMessage(event.entityPlayer, msg);
                return;
            }

            // If shop is owned, check owner have enough funds
            if (shop.useStockChest && ownerWallet.get() < shop.sellPrice)
            {
                ChatComponentTranslation msg = new ChatComponentTranslation("Shop owner out of money");
                msg.getChatStyle().setColor(ChatOutputHandler.chatConfirmationColor);
                ChatOutputHandler.sendMessage(event.entityPlayer, msg);
                return;
            }

            if (!shop.incrStock(transactionStack.stackSize))
            {
                ChatOutputHandler.chatError(event.entityPlayer, "Shop have not enough space!");
                return;
            }

            int removedAmount = 0;

            if (sameItem)
            {
                removedAmount = Math.min(equippedStack.stackSize, transactionStack.stackSize);
                equippedStack.stackSize -= removedAmount;
                if (equippedStack.stackSize <= 0)
                    event.entityPlayer.inventory.mainInventory[event.entityPlayer.inventory.currentItem] = null;
            }

            if (removedAmount < transactionStack.stackSize) {
                removedAmount += ModuleEconomy.tryRemoveItems(event.entityPlayer, transactionStack, transactionStack.stackSize - removedAmount);
            }

            buyerWallet.add(shop.sellPrice);

            if (shop.useStockChest)
                ownerWallet.withdraw(shop.sellPrice);

            String price = APIRegistry.economy.toString(shop.sellPrice);
            ChatComponentTranslation msg = new ChatComponentTranslation("Sold %s x %s for %s (wallet: %s)", shop.amount, itemName, price, buyerWallet.toString());
            msg.getChatStyle().setColor(ChatOutputHandler.chatConfirmationColor);
            ChatOutputHandler.sendMessage(event.entityPlayer, msg);
        }
        else // Sell to player action
        {
            if ((virtualStock || shop.useStockChest) && shop.getStock() < shop.amount)
            {
                ChatOutputHandler.chatError(event.entityPlayer, "Shop stock is empty");
                return;
            }

            if (!buyerWallet.withdraw(shop.buyPrice))
            {
                String errorMsg = Translator.format("You do not have enough %s in your wallet", APIRegistry.economy.currency(2));
                ChatOutputHandler.chatError(event.entityPlayer, errorMsg);
                return;
            }

            if (shop.useStockChest)
                ownerWallet.add(shop.buyPrice);

            shop.decrStock(shop.amount);

            PlayerUtil.give(event.entityPlayer, transactionStack);
            String price = APIRegistry.economy.toString(shop.buyPrice);
            ChatComponentTranslation msg = new ChatComponentTranslation("Bought %s x %s for %s (wallet: %s)", shop.amount, itemName, price, buyerWallet.toString());
            msg.getChatStyle().setColor(ChatOutputHandler.chatConfirmationColor);
            ChatOutputHandler.sendMessage(event.entityPlayer, msg);
        }
    }

    public static ShopData getShop(WorldPoint point, ICommandSender sender)
    {
        ShopData shop = shopSignMap.get(point);

        if (shop == null)
            return null;

        if (!shop.isValid)
            shop.update();

        if (!shop.isValid)
        {
            ChatOutputHandler.chatError(sender, Translator.format("Shop invalid: %s", shop.getError()));
            return null;
        }
        return shop;
    }

    private static void removeShop(ShopData shop)
    {
        shops.remove(shop);
        shopSignMap.remove(shop.pos);
        shopFrameMap.remove(shop.itemFrameId);
    }

    private static void addShop(ShopData shop)
    {
        shops.add(shop);
        shopSignMap.put(shop.pos, shop);
        shopFrameMap.put(shop.itemFrameId, shop);
    }

    private static boolean checkDestroyPermission(UserIdent ident, WorldPoint point, ShopData shop)
    {
        boolean canDestroyAdmin = APIRegistry.perms.checkUserPermission(ident, point, PERM_ADMIN_DESTROY);
        boolean canDestroyPlayer = APIRegistry.perms.checkUserPermission(ident, point, PERM_PLAYER_DESTROY);
        boolean canDestroyAnyPlayer = APIRegistry.perms.checkUserPermission(ident, point, PERM_PLAYER_DESTROY_ANY);
        boolean isShopOwner = shop.owner.equals(ident.getUuid());

        return shop.useStockChest
                ? canDestroyAnyPlayer || (canDestroyPlayer && isShopOwner)
                : canDestroyAdmin;
    }

    /* ------------------------------------------------------------ */

    @Override
    public void load(Configuration config, boolean isReload)
    {
        virtualStock = config.getBoolean(CONFIG_FILE, "adminVirtualStock", false, STOCK_HELP);
        String[] tags = config.get(CONFIG_FILE, "shopTags", shopTags.toArray(new String[shopTags.size()])).getStringList();
        shopTags.clear();

        Collections.addAll(shopTags, tags);
    }

    @Override
    public boolean supportsCanonicalConfig()
    {
        return true;
    }

}
