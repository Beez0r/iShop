package com.minedhype.ishop;

import com.minedhype.ishop.inventories.InvAdminShop;
import com.minedhype.ishop.inventories.InvShop;
import com.minedhype.ishop.inventories.InvStock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import java.util.Optional;

public class EventShop implements Listener {
	public static boolean adminShopEnabled = iShop.config.getBoolean("enableAdminShop");
	public static boolean stockEnabled = iShop.config.getBoolean("enableStockBlock");
	public static boolean shopEnabled = iShop.config.getBoolean("enableShopBlock");
	public static boolean noShopNoStock = iShop.config.getBoolean("mustOwnShopForStock");
	public static boolean placeFrameSign = iShop.config.getBoolean("placeItemFrameSigns");
	public static boolean protectShopFromExplosion = iShop.config.getBoolean("protectShopBlocksFromExplosions");
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
		if(block.getType().equals(stockBlk) && block.getType().equals(shopBlk) && shopEnabled) {
			boolean isShopLoc;
			if(iShop.wgLoader != null)
				isShopLoc = iShop.wgLoader.checkRegion(block);
			else
				isShopLoc = true;
			Optional<Shop> shop = Shop.getShopByLocation(block.getLocation());
			if(!shop.isPresent() || !isShopLoc)
				return;
			if(placeFrameSign) {
				if(event.getPlayer().isSneaking() && event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && (shop.get().isOwner(event.getPlayer().getUniqueId()) || event.getPlayer().hasPermission(Permission.SHOP_ADMIN.toString())) && (event.getPlayer().getInventory().getItemInMainHand().getType().toString().endsWith("_SIGN") || event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.ITEM_FRAME)))
					return;
			}
			if(shop.get().isAdmin() && !adminShopEnabled) {
				event.setCancelled(true);
				event.getPlayer().sendMessage(Messages.ADMIN_SHOP_DISABLED.toString());
				return;
			}
			event.setCancelled(true);
			if(InvStock.inShopInv.containsValue(shop.get().getOwner())) {
				if(event.getHand().equals(EquipmentSlot.HAND))
					event.getPlayer().sendMessage(Messages.SHOP_BUSY.toString());
				else if(event.getHand().equals(EquipmentSlot.OFF_HAND))
					event.getPlayer().sendMessage(Messages.SHOP_BUSY.toString());
				return;
			}
			if((shop.get().isAdmin() && event.getPlayer().hasPermission(Permission.SHOP_ADMIN.toString())) || shop.get().isOwner(event.getPlayer().getUniqueId())) {
				InvAdminShop inv = new InvAdminShop(shop.get(), event.getPlayer());
				inv.open(event.getPlayer(), shop.get().getOwner());
			} else {
				InvShop inv = new InvShop(shop.get());
				inv.open(event.getPlayer(), shop.get().getOwner());
			}
			return;
		}
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
				if(Shop.getNumShops(event.getPlayer().getUniqueId()) < 1 && noShopNoStock) {
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
			if(placeFrameSign)
				if(event.getPlayer().isSneaking() && event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && (shop.get().isOwner(event.getPlayer().getUniqueId()) || event.getPlayer().hasPermission(Permission.SHOP_ADMIN.toString())) && (event.getPlayer().getInventory().getItemInMainHand().getType().toString().endsWith("_SIGN") || event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.ITEM_FRAME)))
					return;
			if(shop.get().isAdmin() && !adminShopEnabled) {
				event.setCancelled(true);
				event.getPlayer().sendMessage(Messages.ADMIN_SHOP_DISABLED.toString());
				return;
			}
			event.setCancelled(true);
			if(InvStock.inShopInv.containsValue(shop.get().getOwner())) {
				if(event.getHand().equals(EquipmentSlot.HAND))
					event.getPlayer().sendMessage(Messages.SHOP_BUSY.toString());
				else if(event.getHand().equals(EquipmentSlot.OFF_HAND))
					event.getPlayer().sendMessage(Messages.SHOP_BUSY.toString());
				return;
			}
			if((shop.get().isAdmin() && event.getPlayer().hasPermission(Permission.SHOP_ADMIN.toString())) || shop.get().isOwner(event.getPlayer().getUniqueId())) {
				InvAdminShop inv = new InvAdminShop(shop.get(), event.getPlayer());
				inv.open(event.getPlayer(), shop.get().getOwner());
			} else {
				InvShop inv = new InvShop(shop.get());
				inv.open(event.getPlayer(), shop.get().getOwner());
			}
		}
	}

	@EventHandler
	public void shopSignBreak(BlockBreakEvent event) {
		if(placeFrameSign && shopEnabled) {
			Location attachedBlock;
			if(event.getBlock().getBlockData() instanceof WallSign) {
				WallSign wallSign = (WallSign) event.getBlock().getBlockData();
				BlockFace blockFace = wallSign.getFacing();
				Block block = event.getBlock().getRelative(blockFace.getOppositeFace());
				attachedBlock = block.getLocation();
			} else if(event.getBlock().getBlockData() instanceof org.bukkit.block.data.type.Sign)
				attachedBlock = event.getBlock().getLocation().subtract(0, 1, 0);
			else
				return;
			Optional<Shop> shop = Shop.getShopByLocation(attachedBlock);
			if(!shop.isPresent() || shop.get().isOwner(event.getPlayer().getUniqueId()) || event.getPlayer().hasPermission(Permission.SHOP_ADMIN.toString()))
				return;
			else
				event.setCancelled(true);
		}
	}

	@EventHandler
	public void protectFromWitherDamage(EntityChangeBlockEvent event) {
		if(protectShopFromExplosion && shopEnabled) {
			if(event.getEntity() instanceof Wither && checkShopLoc(event.getBlock().getLocation()))
				event.setCancelled(true);
		}
	}

	@EventHandler
	public void itemFrameItem(EntityDamageByEntityEvent event) {
		if(placeFrameSign && shopEnabled) {
			if(event.getEntity() instanceof ItemFrame) {
				BlockFace blockFace = event.getEntity().getFacing();
				Block attachedBlock = event.getEntity().getLocation().getBlock().getRelative(blockFace.getOppositeFace());
				Optional<Shop> shop = Shop.getShopByLocation(attachedBlock.getLocation());
				if(!shop.isPresent() || shop.get().isOwner(event.getDamager().getUniqueId()) || event.getDamager().hasPermission(Permission.SHOP_ADMIN.toString()))
					return;
				else
					event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void entityExplosion(EntityExplodeEvent event) {
		if(protectShopFromExplosion && shopEnabled) {
			if(shopBlk == null) {
				try {
					shopBlk = Material.matchMaterial(shopBlock.split("minecraft:")[1].toUpperCase());
				} catch (Exception e) {
					shopBlk = Material.BARREL;
				}
			}
			event.blockList().removeIf(block -> block.getType().equals(shopBlk) && checkShopLoc(block.getLocation()));
		}
	}

	@EventHandler
	public void HangingBreakItemFrameEntity(HangingBreakByEntityEvent event) {
		if(placeFrameSign && shopEnabled) {
			if(event.getEntity() instanceof ItemFrame) {
				BlockFace blockFace = event.getEntity().getAttachedFace();
				Block attachedBlock = event.getEntity().getLocation().getBlock().getRelative(blockFace);
				Optional<Shop> shop = Shop.getShopByLocation(attachedBlock.getLocation());
				if(!shop.isPresent() || shop.get().isOwner(event.getRemover().getUniqueId()) || event.getRemover().hasPermission(Permission.SHOP_ADMIN.toString()))
					return;
				else
					event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void HangingBreakItemFrame(HangingBreakEvent event) {
		if(!event.getCause().equals(HangingBreakEvent.RemoveCause.ENTITY) && placeFrameSign && shopEnabled) {
			if(event.getEntity() instanceof ItemFrame) {
				BlockFace blockFace = event.getEntity().getAttachedFace();
				Block attachedBlock = event.getEntity().getLocation().getBlock().getRelative(blockFace);
				Optional<Shop> shop = Shop.getShopByLocation(attachedBlock.getLocation());
				if(!shop.isPresent())
					return;
				else
					event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void itemFrameItemRotate(PlayerInteractEntityEvent event) {
		if(placeFrameSign && shopEnabled) {
			if(event.getRightClicked() instanceof ItemFrame) {
				BlockFace blockFace = event.getRightClicked().getFacing();
				Block attachedBlock = event.getRightClicked().getLocation().getBlock().getRelative(blockFace.getOppositeFace());
				Optional<Shop> shop = Shop.getShopByLocation(attachedBlock.getLocation());
				if(!shop.isPresent() || shop.get().isOwner(event.getPlayer().getUniqueId()) || event.getPlayer().hasPermission(Permission.SHOP_ADMIN.toString()))
					return;
				else
					event.setCancelled(true);
			}
		}
	}

	private boolean checkShopLoc(Location location) {
		Optional<Shop> shop = Shop.getShopByLocation(location);
		return shop.isPresent();
	}
}
