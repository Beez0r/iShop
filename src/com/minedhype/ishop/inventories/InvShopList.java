package com.minedhype.ishop.inventories;

import com.minedhype.ishop.Shop;
import com.minedhype.ishop.iShop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import com.minedhype.ishop.Messages;
import com.minedhype.ishop.gui.GUI;
import java.util.ArrayList;
import java.util.List;

public class InvShopList extends GUI {
	public final static ArrayList<ItemStack> shopslist = new ArrayList<>();
	private final ItemStack airItem = new ItemStack(Material.AIR, 0);
	private int pag;

	private InvShopList(String shopTitle) {
		super(54, shopTitle);
	}
	public static InvShopList setShopTitle(String shopTitle) {
		return new InvShopList(shopTitle);
	}

	@Override
	public void onClick(InventoryClickEvent event) {
		super.onClick(event);
		if(event.getRawSlot() >= 45 && event.getRawSlot() < 54)
			return;
		if(!event.getAction().equals(InventoryAction.PLACE_ALL) && !event.getAction().equals(InventoryAction.PICKUP_ALL))
			return;
		if(event.getCurrentItem() == null || event.getCurrentItem().getType().isAir())
			return;
		if(event.isLeftClick() && event.getCurrentItem().getItemMeta().hasLore() && event.getCurrentItem().getType().equals(Material.PLAYER_HEAD)) {
			event.setCancelled(true);
			Player player = (Player) event.getWhoClicked();
			List<String> itemLore = event.getCurrentItem().getItemMeta().getLore();
			player.closeInventory();
			Bukkit.getScheduler().runTaskAsynchronously(iShop.getPlugin(), shopslist::clear);
			player.performCommand("shop view " + itemLore.get(0));
		}
	}

	public void PlayerShopList() {
		int index = pag * 45;
		for(int i = 0; i < 45; i++) {
			if(index <= shopslist.size()-1) {
				placeItem(i, shopslist.get(index));
				index++;
			} else
				placeItem(i, airItem);
		}
		int shopListPages = (int)Math.ceil(shopslist.size()-1)/44;
		for(int i=45; i<54; i++) {
			if(i == 47 && pag > 0) {
				placeItem(i, GUI.createItem(Material.ARROW, Messages.SHOP_PAGE.toString()+" " + (pag)), p -> openPage(p, pag-1));
			} else if(i == 51 && pag < shopListPages) {
				placeItem(i, GUI.createItem(Material.ARROW, Messages.SHOP_PAGE.toString()+" " + (pag+2)), p -> openPage(p, pag+1));
			} else {
				placeItem(i, GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, ""));
			}
		}
	}

	private void openPage(Player player, int pag) {
		for(int i=45; i<54; i++)
			placeItem(i, new ItemStack(airItem));
		player.closeInventory();
		shopslist.clear();
		this.pag = pag;
		this.open(player);
	}

	@Override
	public void open(Player player) {
		Bukkit.getScheduler().runTaskAsynchronously(iShop.getPlugin(), () ->  {
			Shop.getShopList(player.getUniqueId());
			Bukkit.getScheduler().runTask(iShop.getPlugin(), () -> {
				PlayerShopList();
				super.open(player);
			});
		});
	}

	@Override
	public void onDrag(InventoryDragEvent event) {
		super.onDrag(event);
		event.setCancelled(true);
	}

	public void setPag(int pag) { this.pag = pag; }
}
