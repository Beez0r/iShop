package com.minedhype.ishop.inventories;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import com.minedhype.ishop.Messages;
import com.minedhype.ishop.Permission;
import com.minedhype.ishop.Shop;
import com.minedhype.ishop.StockShop;
import com.minedhype.ishop.iShop;
import com.minedhype.ishop.gui.GUI;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;

public class InvStock extends GUI {

	public static final HashMap<Player, UUID> inShopInv = new HashMap<>();
	private static final List<InvStock> inventories = new ArrayList<>();
	private final ItemStack airItem = new ItemStack(Material.AIR, 0);
	private final UUID owner;
	private int pag;
	private Player player;
	
	private InvStock(UUID owner) {
		super(54, Messages.SHOP_TITLE_STOCK.toString());
		inventories.add(this);
		this.owner = owner;
		this.pag = 0;
	}
	
	public static InvStock getInvStock(UUID owner) { return inventories.parallelStream().filter(inv -> inv.owner.equals(owner)).findFirst().orElse(new InvStock(owner)); }
	
	@Override
	public void onClick(InventoryClickEvent event) {
		super.onClick(event);
		if(event.getRawSlot() >= 45 && event.getRawSlot() < 54)
			return;
		if(event.getRawSlot() >= 54 && !player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			if(InvCreateRow.strictStock) {
				ItemStack item = event.getCurrentItem();
				ItemStack item2 = event.getCursor();
				if(Shop.strictStockShopCheck(item, owner) || Shop.strictStockShopCheck(item2, owner))
					return;
			}
			if(InvCreateRow.itemsDisabled) {
				ItemStack item = event.getCurrentItem();
				ItemStack item2 = event.getCursor();
				for(String itemsList:InvCreateRow.disabledItemList) {
					Material disabledItemsList = Material.matchMaterial(itemsList);
					if(item == null)
						item = airItem;
					if(item2 == null)
						item2 = airItem;
					if(disabledItemsList != null) {
						if(!item.isSimilar(airItem)) {
							if(item.getType().equals(disabledItemsList))
								return;
							if(item.getType().toString().contains("SHULKER_BOX") && item.getItemMeta() instanceof BlockStateMeta) {
								BlockStateMeta itemMeta1 = (BlockStateMeta) item.getItemMeta();
								ShulkerBox shulkerBox1 = (ShulkerBox) itemMeta1.getBlockState();
								if(shulkerBox1.getInventory().contains(disabledItemsList))
									return;
							} else if(item.getType().equals(Material.BUNDLE)) {
								BundleMeta bundleIn1 = (BundleMeta) item.getItemMeta();
								if(bundleIn1.hasItems()) {
									ItemStack itemDisabledOut = new ItemStack(disabledItemsList);
									List<ItemStack> bundleIn1Items = bundleIn1.getItems();
									for(ItemStack bundleList : bundleIn1Items)
										if(bundleList.isSimilar(itemDisabledOut))
											return;
								}
							}
						}
						if(!item2.isSimilar(airItem)) {
							if(item2.getType().equals(disabledItemsList))
								return;
							if(item2.getType().toString().contains("SHULKER_BOX") && item2.getItemMeta() instanceof BlockStateMeta) {
								BlockStateMeta itemMeta2 = (BlockStateMeta) item2.getItemMeta();
								ShulkerBox shulkerBox2 = (ShulkerBox) itemMeta2.getBlockState();
								if(shulkerBox2.getInventory().contains(disabledItemsList))
									return;
							} else if(item2.getType().equals(Material.BUNDLE)) {
								BundleMeta bundleIn2 = (BundleMeta) item2.getItemMeta();
								if(bundleIn2.hasItems()) {
									ItemStack itemDisabledOut = new ItemStack(disabledItemsList);
									List<ItemStack> bundleIn2Items = bundleIn2.getItems();
									for(ItemStack bundleList : bundleIn2Items)
										if(bundleList.isSimilar(itemDisabledOut))
											return;
								}
							}
						}
					}
				}
			}
		}
		event.setCancelled(false);
	}
	
	public void refreshItems() {
		Optional<StockShop> stockOpt = StockShop.getStockShopByOwner(owner, pag);
		StockShop stock;
		stock = stockOpt.orElseGet(() -> new StockShop(owner, pag));
		Inventory inv = stock.getInventory();
		for(int i=0; i<45; i++)
			placeItem(i, inv.getItem(i));

		for(int i=45; i<54; i++) {
			if(i == 47 && pag > 0)
				placeItem(i, GUI.createItem(Material.ARROW, Messages.SHOP_PAGE + " " + (pag)), p -> openPage(p, pag-1));
			else if(i == 51 && pag < iShop.config.getInt("stockPages")-1)
				placeItem(i, GUI.createItem(Material.ARROW, Messages.SHOP_PAGE + " " + (pag+2)), p -> openPage(p, pag+1));
			else
				placeItem(i, GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, ""));
		}
	}
	
	private void openPage(Player player, int pag) {
		for(int i=45; i<54; i++)
			placeItem(i, new ItemStack(Material.AIR));
		player.closeInventory();
		this.pag = pag;
		this.open(player);
	}
	
	@Override
	public void open(Player player) {
		this.player = player;
		refreshItems();
		super.open(player);
	}
	
	@Override
	public void onClose(InventoryCloseEvent event) {
		Inventory inventory = event.getInventory();
		Optional<StockShop> stock = StockShop.getStockShopByOwner(owner, pag);
		if(!stock.isPresent())
			return;
		stock.get().setInventory(inventory);
	}
	
	public void setPag(int pag) {
		this.pag = pag;
	}
}
