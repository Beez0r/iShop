package com.minedhype.ishop;

import com.minedhype.ishop.inventories.InvAdminShop;
import com.minedhype.ishop.inventories.InvShop;
import com.minedhype.ishop.inventories.InvStock;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Optional;

public class EventShop implements Listener {
	public static boolean adminShopEnabled = iShop.config.getBoolean("enableAdminShop");
	public static boolean stockEnabled = iShop.config.getBoolean("enableStockBlock");
	public static boolean shopEnabled = iShop.config.getBoolean("enableShopBlock");
	public static String shopBlock = iShop.config.getString("shopBlock");
	public static String stockBlock = iShop.config.getString("stockBlock");
	public static Material shopBlk = Material.matchMaterial(shopBlock);
	public static Material stockBlk = Material.matchMaterial(stockBlock);

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if(!stockEnabled && !shopEnabled)
			return;

		Block block = event.getClickedBlock();
		if(block == null)
			return;

		if(stockBlk == null) {
			try {
				stockBlk = Material.matchMaterial(stockBlock.split("minecraft:")[1].toUpperCase());
			} catch (Exception e) { stockBlk = Material.COMPOSTER; }
		}

		if(shopBlk == null) {
			try {
				shopBlk = Material.matchMaterial(shopBlock.split("minecraft:")[1].toUpperCase());
			} catch (Exception e) { shopBlk = Material.BARREL; }
		}

		if(!block.getType().equals(stockBlk) && !block.getType().equals(shopBlk))
			return;

		if(block.getType().equals(stockBlk) && stockEnabled) {
			boolean isShopLoc;
			if(iShop.wgLoader != null)
				isShopLoc = iShop.wgLoader.checkRegion(block);
			else
				isShopLoc = true;

			if(!isShopLoc || event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getPlayer().isSneaking())
				return;

			event.setCancelled(true);
			if(event.getAction() == Action.LEFT_CLICK_BLOCK) {
				if(Shop.getNumShops(event.getPlayer().getUniqueId()) < 1) {
					event.getPlayer().sendMessage(Messages.NO_SHOP_STOCK.toString());
					return;
				}
				if(InvStock.inShopInv.containsValue(event.getPlayer().getUniqueId())) {
					event.getPlayer().sendMessage(Messages.SHOP_BUSY.toString());
					return;
				} else { InvStock.inShopInv.put(event.getPlayer(), event.getPlayer().getUniqueId()); }

				InvStock inv = InvStock.getInvStock(event.getPlayer().getUniqueId());
				inv.setPag(0);
				inv.open(event.getPlayer());
			}
			return;
		}

		if(block.getType().equals(shopBlk) && shopEnabled) {
			boolean isShopLoc;
			if(iShop.wgLoader != null)
				isShopLoc = iShop.wgLoader.checkRegion(block);
			else
				isShopLoc = true;

			Optional<Shop> shop = Shop.getShopByLocation(block.getLocation());
			if(!shop.isPresent() || !isShopLoc)
				return;

			if(shop.get().isAdmin() && !adminShopEnabled) {
				event.setCancelled(true);
				event.getPlayer().sendMessage(Messages.ADMIN_SHOP_DISABLED.toString());
				return;
			}
			event.setCancelled(true);
			if(InvStock.inShopInv.containsValue(shop.get().getOwner()) && (event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
				event.getPlayer().sendMessage(Messages.SHOP_BUSY.toString());
				return;
			}

			if((shop.get().isAdmin() && event.getPlayer().hasPermission(Permission.SHOP_ADMIN.toString())) || shop.get().isOwner(event.getPlayer().getUniqueId())) {
				InvAdminShop inv = new InvAdminShop(shop.get());
				inv.open(event.getPlayer(), shop.get().getOwner());
			} else {
				InvShop inv = new InvShop(shop.get());
				inv.open(event.getPlayer(), shop.get().getOwner());
			}
		}
	}
}
