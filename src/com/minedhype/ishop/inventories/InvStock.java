package com.minedhype.ishop.inventories;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.minedhype.ishop.Messages;
import com.minedhype.ishop.StockShop;
import com.minedhype.ishop.iShop;
import com.minedhype.ishop.gui.GUI;

public class InvStock extends GUI {

	private static final List<InvStock> inventories = new ArrayList<>();
	public static final HashMap<Player, UUID> inShopInv = new HashMap<>();
	private final UUID owner;
	private int pag;
	
	private InvStock(UUID owner) {
		super(54, Messages.SHOP_TITLE_STOCK.toString());
		inventories.add(this);
		this.owner = owner;
		this.pag = 0;
	}
	
	public static InvStock getInvStock(UUID owner) {
		return inventories.parallelStream().filter(inv -> inv.owner.equals(owner)).findFirst().orElse(new InvStock(owner));
	}
	
	@Override
	public void onClick(InventoryClickEvent event) {
		super.onClick(event);
		if(event.getRawSlot() >= 45 && event.getRawSlot() < 54)
			return;

		event.setCancelled(false);
	}
	
	public void refreshItems() {
		Optional<StockShop> stockOpt = StockShop.getStockShopByOwner(owner, pag);
		StockShop stock = null;
		stock = stockOpt.orElseGet(() -> new StockShop(owner, pag));
		Inventory inv = stock.getInventory();

		for(int i=0; i<45; i++) {
			ItemStack item = inv.getItem(i);
			placeItem(i, item);
		}

		for(int i=45; i<54; i++) {
			if(i == 47 && pag > 0) {
				ItemStack item = GUI.createItem(Material.ARROW, Messages.SHOP_PAGE.toString()+" " + (pag));
				placeItem(i, item, p -> openPage(p, pag-1));
			} else if(i == 51 && pag < iShop.config.getInt("stockPages")-1) {
				ItemStack item = GUI.createItem(Material.ARROW, Messages.SHOP_PAGE.toString()+" " + (pag+2));
				placeItem(i, item, p -> openPage(p, pag+1));
			} else {
				ItemStack item = GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, "");
				placeItem(i, item);
			}
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
