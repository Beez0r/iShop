package com.minedhype.ishop;

import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Utils {
	private static final ItemStack airItem = new ItemStack(Material.AIR, 0);

	public static boolean hasStock(Player player, ItemStack item) {
		if(item == null || item == airItem)
			return true;
		return player.getInventory().containsAtLeast(item, item.getAmount());
	}

	public static boolean hasStock(Shop shop, ItemStack item) {
		if(shop.isAdmin() || item == null || item == airItem)
			return true;
		int max = iShop.config.getInt("stockPages");
		int amount = item.getAmount();
		int itemAmountCount = 0;
		for(int i=0; i<max; i++) {
			Optional<StockShop> stockStore = StockShop.getStockShopByOwner(shop.getOwner(), i);
			if(!stockStore.isPresent())
				continue;
			if(stockStore.get().getInventory().containsAtLeast(item, amount))
				return true;
			if(stockStore.get().getInventory().contains(item.getType())) {
				for(int j=0; j<stockStore.get().getInventory().getSize()-1; j++) {
					if(stockStore.get().getInventory().getItem(j) != null) {
						if(item.hasItemMeta()) {
							if(stockStore.get().getInventory().getItem(j).equals(item))
								itemAmountCount += stockStore.get().getInventory().getItem(j).getAmount();
						}
						else {
							if(stockStore.get().getInventory().getItem(j).equals(item.getType()))
								itemAmountCount += stockStore.get().getInventory().getItem(j).getAmount();
						}
					}
					if(itemAmountCount >= amount)
						return true;
				}
			}
		}
		return false;
	}

	public static boolean hasDoubleItemStock(Player player, ItemStack item, ItemStack item2) {
		if(item == null || item2 == null || item == airItem || item2 == airItem)
			return true;
		int bothAmounts = item.getAmount() + item2.getAmount();
		return player.getInventory().containsAtLeast(item, bothAmounts);
	}

	public static boolean hasDoubleItemStock(Shop shop, ItemStack item, int amount) {
		if(shop.isAdmin() || item == null || item == airItem)
			return true;
		int max = iShop.config.getInt("stockPages");
		int itemAmountCount = 0;
		for(int i=0; i<max; i++) {
			Optional<StockShop> stockStore = StockShop.getStockShopByOwner(shop.getOwner(), i);
			if(!stockStore.isPresent())
				continue;
			if(stockStore.get().getInventory().containsAtLeast(item, amount))
				return true;
			if(stockStore.get().getInventory().contains(item.getType())) {
				for(int j=0; j<stockStore.get().getInventory().getSize()-1; j++) {
					if(stockStore.get().getInventory().getItem(j) != null) {
						if(item.hasItemMeta()) {
							if(stockStore.get().getInventory().getItem(j).equals(item))
								itemAmountCount += stockStore.get().getInventory().getItem(j).getAmount();
						}
						else {
							if(stockStore.get().getInventory().getItem(j).equals(item.getType()))
								itemAmountCount += stockStore.get().getInventory().getItem(j).getAmount();
						}
					}
					if(itemAmountCount >= amount)
						return true;
				}
			}
		}
		return false;
	}
}
