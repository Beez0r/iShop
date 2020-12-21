package com.minedhype.ishop;

import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Utils {
	public static boolean hasStock(Player player, ItemStack item) {
		if(item == null || item.getType().equals(Material.AIR))
			return true;
		return player.getInventory().containsAtLeast(item, item.getAmount());
	}

	public static boolean hasStock(Shop shop, ItemStack item) {
		if(shop.isAdmin() || item == null || item.getType().equals(Material.AIR))
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
					if(stockStore.get().getInventory().getItem(j) != null && stockStore.get().getInventory().getItem(j).isSimilar(item))
						itemAmountCount += stockStore.get().getInventory().getItem(j).getAmount();
					if(itemAmountCount >= amount)
						return true;
				}
			}
		}
		return false;
	}

	public static boolean hasDoubleItemStock(Player player, ItemStack item, ItemStack item2) {
		if((item == null && item2 == null) || (item.getType().equals(Material.AIR) && item2.getType().equals(Material.AIR)))
			return true;
		int bothAmounts = item.getAmount() + item2.getAmount();
		return player.getInventory().containsAtLeast(item, bothAmounts);
	}

	public static boolean hasDoubleItemStock(Shop shop, ItemStack item, ItemStack item2) {
		if(shop.isAdmin() || (item == null && item2 == null) || (item.getType().equals(Material.AIR) && item2.getType().equals(Material.AIR)))
			return true;
		int item1Amount = 0;
		int item2Amount = 0;
		int item1Total = item.getAmount();
		int item2Total = item2.getAmount();
		int max = iShop.config.getInt("stockPages");
		for(int i=0; i<max; i++) {
			Optional<StockShop> stockStore = StockShop.getStockShopByOwner(shop.getOwner(), i);
			if(!stockStore.isPresent())
				continue;
			if(stockStore.get().getInventory().containsAtLeast(item, item1Total) || stockStore.get().getInventory().containsAtLeast(item2, item2Total))
				return true;
			if(stockStore.get().getInventory().contains(item.getType()) || stockStore.get().getInventory().contains(item2.getType())) {
				for(int j=0; j<stockStore.get().getInventory().getSize()-1; j++) {
					if(stockStore.get().getInventory().getItem(j) != null) {
						if(item1Amount < item1Total && stockStore.get().getInventory().getItem(j).isSimilar(item))
							item1Amount += stockStore.get().getInventory().getItem(j).getAmount();
						else if(item2Amount < item2Total && stockStore.get().getInventory().getItem(j).isSimilar(item2))
							item2Amount += stockStore.get().getInventory().getItem(j).getAmount();
					}
					if(item1Amount >= item1Total && item2Amount >= item2Total)
						return true;
				}
			}
		}
		return false;
	}
}
