package com.minedhype.ishop;

import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.minedhype.ishop.inventories.InvAdminShop;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

public class Utils {
	public static boolean hasStock(Player player, ItemStack item) {
		if(item == null || item.getType().equals(Material.AIR))
			return true;
		if(item.hasItemMeta()) {
			ItemStack itemClone = item.clone();
			return (player.getInventory().containsAtLeast(item, item.getAmount()) || player.getInventory().containsAtLeast(itemClone, itemClone.getAmount()));
		} else
			return player.getInventory().containsAtLeast(item, item.getAmount());
	}

	public static boolean hasStock(Shop shop, ItemStack item) {
		if(shop.isAdmin() || item == null || item.getType().equals(Material.AIR))
			return true;
		int max;
		if(InvAdminShop.usePerms)
			max = InvAdminShop.permissionMax;
		else
			max = InvAdminShop.maxPages;
		int amount = item.getAmount();
		int itemAmountCount = 0;
		for(int i=0; i<max; i++) {
			Optional<StockShop> stockStore = StockShop.getStockShopByOwner(shop.getOwner(), i);
			if(!stockStore.isPresent())
				continue;
			if(stockStore.get().getInventory().containsAtLeast(item, amount))
				return true;
			if(item.hasItemMeta()) {
				ItemStack itemClone = item.clone();
				if(stockStore.get().getInventory().containsAtLeast(item, amount) || stockStore.get().getInventory().containsAtLeast(itemClone, amount))
					return true;
			} else {
				if(stockStore.get().getInventory().containsAtLeast(item, amount))
					return true;
			}
			if(stockStore.get().getInventory().contains(item.getType())) {
				for(int j=0; j<stockStore.get().getInventory().getSize()-1; j++) {
					if(item.hasItemMeta()) {
						ItemStack itemClone = item.clone();
						if(stockStore.get().getInventory().getItem(j) != null && (stockStore.get().getInventory().getItem(j).isSimilar(item) || stockStore.get().getInventory().getItem(j).isSimilar(itemClone)))
							itemAmountCount += stockStore.get().getInventory().getItem(j).getAmount();
					} else {
						if(stockStore.get().getInventory().getItem(j) != null && stockStore.get().getInventory().getItem(j).isSimilar(item))
							itemAmountCount += stockStore.get().getInventory().getItem(j).getAmount();
					}
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
		if(!item.hasItemMeta() && !item2.hasItemMeta())
			return player.getInventory().containsAtLeast(item, bothAmounts);
		else {
			boolean hasItems = false;
			boolean hasItems2 = false;
			if(item.hasItemMeta()) {
				ItemStack itemClone = item.clone();
				hasItems = player.getInventory().containsAtLeast(itemClone, bothAmounts);
			}
			if(item2.hasItemMeta()) {
				ItemStack item2Clone = item2.clone();
				hasItems2 = player.getInventory().containsAtLeast(item2Clone, bothAmounts);
			}
			return (player.getInventory().containsAtLeast(item, bothAmounts) || player.getInventory().containsAtLeast(item2, bothAmounts) || hasItems || hasItems2);
		}
	}

	public static boolean hasDoubleItemStock(Shop shop, ItemStack item, ItemStack item2) {
		if(shop.isAdmin() || (item == null && item2 == null) || (item.getType().equals(Material.AIR) && item2.getType().equals(Material.AIR)))
			return true;
		int item1Amount = 0;
		int item2Amount = 0;
		int item1Total = item.getAmount();
		int item2Total = item2.getAmount();
		int max;
		if(InvAdminShop.usePerms)
			max = InvAdminShop.permissionMax;
		else
			max = InvAdminShop.maxPages;
		for(int i=0; i<max; i++) {
			Optional<StockShop> stockStore = StockShop.getStockShopByOwner(shop.getOwner(), i);
			if(!stockStore.isPresent())
				continue;
			if(stockStore.get().getInventory().contains(item.getType()) || stockStore.get().getInventory().contains(item2.getType())) {
				for(int j=0; j<stockStore.get().getInventory().getSize(); j++) {
					if(stockStore.get().getInventory().getItem(j) != null) {
						if(!item.hasItemMeta() && !item2.hasItemMeta()) {
							if(item1Amount < item1Total && stockStore.get().getInventory().getItem(j).isSimilar(item)) {
								item1Amount += stockStore.get().getInventory().getItem(j).getAmount();
								if(item1Amount > item1Total && item.isSimilar(item2)) {
									int difference = item1Amount - item1Total;
									item2Amount += difference;
								}
							} else if(item2Amount < item2Total && stockStore.get().getInventory().getItem(j).isSimilar(item2))
								item2Amount += stockStore.get().getInventory().getItem(j).getAmount();
						} else {
							ItemStack itemClone = item.clone();
							ItemStack item2Clone = item2.clone();
							if(item1Amount < item1Total && (stockStore.get().getInventory().getItem(j).isSimilar(item) || stockStore.get().getInventory().getItem(j).isSimilar(itemClone))) {
								item1Amount += stockStore.get().getInventory().getItem(j).getAmount();
								if(item1Amount > item1Total && (item.isSimilar(item2) || item.isSimilar(item2Clone) || itemClone.isSimilar(item2) || itemClone.isSimilar(item2Clone))) {
									int difference = item1Amount - item1Total;
									item2Amount += difference;
								}
							} else if(item2Amount < item2Total && (stockStore.get().getInventory().getItem(j).isSimilar(item2) || stockStore.get().getInventory().getItem(j).isSimilar(item2Clone)))
								item2Amount += stockStore.get().getInventory().getItem(j).getAmount();
						}
					}
					if(item1Amount >= item1Total && item2Amount >= item2Total)
						return true;
				}
			}
		}
		return false;
	}

	public static boolean hasEnchantment(Shop shop, String bookName, boolean doubleBook) {
		if(shop.isAdmin())
			return true;
		int max;
		if(InvAdminShop.usePerms)
			max = InvAdminShop.permissionMax;
		else
			max = InvAdminShop.maxPages;
		int count = 0;
		for(int i=0; i<max; i++) {
			Optional<StockShop> stockStore = StockShop.getStockShopByOwner(shop.getOwner(), i);
			if(!stockStore.isPresent())
				continue;
			if(stockStore.get().getInventory().contains(Material.ENCHANTED_BOOK)) {
				for(int j=0; j<stockStore.get().getInventory().getSize(); j++) {
					if(stockStore.get().getInventory().getItem(j) != null && stockStore.get().getInventory().getItem(j).getType().equals(Material.ENCHANTED_BOOK) && stockStore.get().getInventory().getItem(j).hasItemMeta()) {
							EnchantmentStorageMeta meta = (EnchantmentStorageMeta) stockStore.get().getInventory().getItem(j).getItemMeta();
							if(meta.getStoredEnchants().toString().contains(bookName)) {
								if(doubleBook) {
									count++;
									if(count>1)
										return true;
								}
								else
									return true;
							}
					}
				}
			}
		}
		return false;
	}
}
