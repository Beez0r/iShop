package com.minedhype.ishop;

import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Utils {

	public static boolean hasStock(Player player, ItemStack item) {
		if(item == null || item.getType() == Material.AIR)
			return true;

		return player.getInventory().containsAtLeast(item, item.getAmount());
	}

	public static boolean hasDoubleItemStock(Player player, ItemStack item, ItemStack item2) {
		if(item == null || item2 == null || item.getType() == Material.AIR || item2.getType() == Material.AIR)
			return true;
		int bothAmounts = item.getAmount() + item2.getAmount();
		return player.getInventory().containsAtLeast(item, bothAmounts);
	}
	
	public static boolean hasStock(Shop shop, ItemStack item) {
		if(shop.isAdmin() || item == null || item.getType() == Material.AIR)
			return true;

		int max = iShop.config.getInt("stockPages");
		for(int i=0; i<max; i++) {
			Optional<StockShop> stockStore = StockShop.getStockShopByOwner(shop.getOwner(), i);
			if(!stockStore.isPresent())
				continue;
			if(stockStore.get().getInventory().containsAtLeast(item, item.getAmount()))
				return true;
		}
		return false;
	}

	public static boolean hasDoubleItemStock(Shop shop, ItemStack item, int amount) {
		if(shop.isAdmin() || item == null || item.getType() == Material.AIR)
			return true;

		int max = iShop.config.getInt("stockPages");
		for(int i=0; i<max; i++) {
			Optional<StockShop> stockStore = StockShop.getStockShopByOwner(shop.getOwner(), i);
			if(!stockStore.isPresent())
				continue;
			if(stockStore.get().getInventory().containsAtLeast(item, amount))
				return true;
		}
		return false;
	}
}
