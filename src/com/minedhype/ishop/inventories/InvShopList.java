package com.minedhype.ishop.inventories;

import com.minedhype.ishop.Shop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import com.minedhype.ishop.Messages;
import com.minedhype.ishop.gui.GUI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InvShopList extends GUI {
	private final ArrayList<ItemStack> shopslist = new ArrayList<>();
	private final ItemStack airItem = new ItemStack(Material.AIR, 0);
	private final ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD,1);
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
			player.performCommand("shop view " + itemLore.get(0));
		}
	}

	public void PlayerShopList() {
		int index = pag*45;
		for(int i = 0; i < 45; i++) {
			if(index <= shopslist.size()-1) {
				placeItem(i, shopslist.get(index));
				index++;
			} else placeItem(i, airItem);
		}

		int shopListPages = (int)Math.ceil(shopslist.size()-1)/44;
		for(int i=45; i<54; i++) {
			if(i == 47 && pag > 0) {
				ItemStack item = GUI.createItem(Material.ARROW, Messages.SHOP_PAGE.toString()+" " + (pag));
				placeItem(i, item, p -> openPage(p, pag-1));
			} else if(i == 51 && pag < shopListPages) {
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
			placeItem(i, new ItemStack(airItem));

		player.closeInventory();
		shopslist.clear();
		this.pag = pag;
		this.open(player);
	}

	@Override
	public void open(Player player) {
		getShopList(player.getUniqueId());
		PlayerShopList();
		super.open(player);
	}

	@Override
	public void onDrag(InventoryDragEvent event) {
		super.onDrag(event);
		event.setCancelled(true);
	}

	public void setPag(int pag) {
		this.pag = pag;
	}

	private void getShopList(UUID uuid) {
		for(Integer id : Shop.shopList.keySet()) {
			if(Shop.shopList.get(id) != null && !Shop.shopList.get(id).equals(uuid)) {
				ItemStack item = new ItemStack(playerHead);
				SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(Shop.shopList.get(id));
				skullMeta.setOwningPlayer(offlinePlayer);
				skullMeta.setDisplayName(offlinePlayer.getName() + Messages.SHOP_NUMBER.toString() + id);
				List<String> skullLore = new ArrayList<>();
				skullLore.add(id.toString());
				skullMeta.setLore(skullLore);
				item.setItemMeta(skullMeta);
				shopslist.add(item);
			}
		}
	}
}
