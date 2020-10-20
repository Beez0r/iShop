package com.minedhype.ishop;

import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Utils {
	
	public static boolean hasStock(Shop shop, ItemStack item, int itemAmount) {
		if(itemAmount == 0)
			itemAmount = item.getAmount();
		if(shop.isAdmin() || item == null || item.getType().equals(Material.AIR))
			return true;

		int max = iShop.config.getInt("stockPages");
		for(int i=0; i<max; i++) {
			Optional<StockShop> stockStore = StockShop.getStockShopByOwner(shop.getOwner(), i);
			if(!stockStore.isPresent())
				continue;
			if(stockStore.get().getInventory().containsAtLeast(item, itemAmount))
				return true;
		}
		return false;
	}
}
