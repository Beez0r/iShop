package com.minedhype.ishop.inventories;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.minedhype.ishop.RowStore;
import com.minedhype.ishop.Messages;
import com.minedhype.ishop.Shop;
import com.minedhype.ishop.gui.GUI;
import java.util.Objects;

public class InvCreateRow extends GUI {
	private ItemStack itemIn;
	private ItemStack itemOut;
	
	public InvCreateRow(Shop shop, int index) {
		super(9*3, Messages.SHOP_TITLE_CREATESHOP.toString());
		
		for(int i=0; i<9*3; i++) {
			if(i == 2) {
				placeItem(i, GUI.createItem(Material.OAK_SIGN, ChatColor.GREEN + Messages.SHOP_TITLE_SELL.toString()));
			} else if(i == 6) {
				placeItem(i, GUI.createItem(Material.OAK_SIGN, ChatColor.RED + Messages.SHOP_TITLE_BUY.toString()));
			} else if(i == 11) {	// Item reserve
			} else if(i == 13) {
				placeItem(i, GUI.createItem(Material.LIME_DYE, ChatColor.BOLD + Messages.SHOP_TITLE_CREATE.toString()), p -> {
					if(itemIn == null || itemIn.getType().equals(Material.AIR)) {
						return;
					}
					if(itemOut == null || itemOut.getType().equals(Material.AIR)) {
						return;
					}
					
					shop.getRows()[index] = new RowStore(itemOut, itemIn, false);
					InvAdminShop inv = new InvAdminShop(shop);
					inv.open(p);
				});
			} else if(i == 15) {	// Item reserve
			} else { placeItem(i, GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, "")); }
		}
	}
	
	public void onDrag(InventoryDragEvent event) {		
		super.onDrag(event);
		
		Inventory inv = event.getInventory();
		if(inv.getType().equals(InventoryType.CHEST) && event.getView().getTitle().contains(Messages.SHOP_TITLE_CREATESHOP.toString()))
			event.setCancelled(true);
	}
	
	@Override
	public void onClick(InventoryClickEvent event) {
		super.onClick(event);
		if(!event.getAction().equals(InventoryAction.PLACE_ALL) && !event.getAction().equals(InventoryAction.PICKUP_ALL))
			return;
		
		event.setCancelled(false);
		Inventory inv = event.getClickedInventory();
		if(Objects.requireNonNull(inv).getType().equals(InventoryType.CHEST) && event.getView().getTitle().contains(Messages.SHOP_TITLE_CREATESHOP.toString())) {
			event.setCancelled(true);
			
			if(event.getRawSlot() == 11 || event.getRawSlot() == 15) {				
				ItemStack item =  Objects.requireNonNull(event.getCursor()).clone();
				
				if(event.getClick().isRightClick()) {				
					item.setAmount(1);
					placeItem(event.getRawSlot(), item);
				} else { placeItem(event.getRawSlot(), item); }

				if(event.getRawSlot() == 11) {
					itemOut = item;
				} else if(event.getRawSlot() == 15) {
					itemIn = item;
				}
			}			
		}
	}
}
