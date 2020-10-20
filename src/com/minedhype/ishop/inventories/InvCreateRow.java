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

public class InvCreateRow extends GUI {
	private ItemStack itemIn;
	private ItemStack itemIn2;
	private ItemStack itemOut;
	private ItemStack itemOut2;
	private final ItemStack airItem = new ItemStack(Material.AIR);
	
	public InvCreateRow(Shop shop, int index) {
		super(9*3, Messages.SHOP_TITLE_CREATESHOP.toString());
		
		for(int i=0; i<9*3; i++) {
			if(i == 1) {
				placeItem(i, GUI.createItem(Material.OAK_SIGN, ChatColor.GREEN + Messages.SHOP_TITLE_SELL.toString()));
			} else if(i == 2) {
				placeItem(i, GUI.createItem(Material.OAK_SIGN, ChatColor.GREEN + Messages.SHOP_TITLE_SELL2.toString()));
			} else if(i == 6) {
				placeItem(i, GUI.createItem(Material.OAK_SIGN, ChatColor.RED + Messages.SHOP_TITLE_BUY.toString()));
			} else if(i == 7) {
				placeItem(i, GUI.createItem(Material.OAK_SIGN, ChatColor.RED + Messages.SHOP_TITLE_BUY2.toString()));
			} else if(i == 10 || i == 11) {
			} else if(i == 13) {
				placeItem(i, GUI.createItem(Material.LIME_DYE, ChatColor.BOLD + Messages.SHOP_TITLE_CREATE.toString()), p -> {
					if(itemIn == null)
						itemIn = airItem;
					if(itemIn2 == null)
						itemIn2 = airItem;
					if(itemOut == null)
						itemOut = airItem;
					if(itemOut2 == null)
						itemOut2 = airItem;

					if(itemIn.getType() == Material.AIR && itemIn2.getType() == Material.AIR && itemOut.getType() == Material.AIR && itemOut2.getType() == Material.AIR)
						return;
					if((itemOut.getType() == Material.AIR && itemOut2.getType() == Material.AIR) || (itemIn.getType() == Material.AIR && itemIn2.getType() == Material.AIR))
						return;

					shop.getRows()[index] = new RowStore(itemOut, itemOut2, itemIn, itemIn2, false);
					InvAdminShop inv = new InvAdminShop(shop);
					inv.open(p);
				});
			} else if(i == 15 || i == 16) {
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
		if(inv.getType().equals(InventoryType.CHEST) && event.getView().getTitle().contains(Messages.SHOP_TITLE_CREATESHOP.toString())) {
			event.setCancelled(true);
			
			if(event.getRawSlot() == 10 || event.getRawSlot() == 11 || event.getRawSlot() == 15 || event.getRawSlot() == 16) {
				ItemStack item =  event.getCursor().clone();
				
				if(event.getClick().isRightClick())
					item.setAmount(1);
				placeItem(event.getRawSlot(), item);

				if(event.getRawSlot() == 10)
					itemOut = item;
				else if(event.getRawSlot() == 11)
					itemOut2 = item;
				else if(event.getRawSlot() == 15)
					itemIn = item;
				else if(event.getRawSlot() == 16)
					itemIn2 = item;

			}			
		}
	}
}
