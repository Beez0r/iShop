package com.minedhype.ishop.inventories;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import com.minedhype.ishop.iShop;
import com.minedhype.ishop.RowStore;
import com.minedhype.ishop.Messages;
import com.minedhype.ishop.gui.GUI;
import com.minedhype.ishop.Shop;

public class InvShop extends GUI {
	public static boolean listAllShops = iShop.config.getBoolean("publicShopListCommand");

	public InvShop(Shop shop) {
		super(54, getShopName(shop));
		
		for(int x=0; x<9; x++) {
			for(int y=0; y<6; y++) {
				if(x == 1) {
					if(y == 0) {
						placeItem(y*9+x, GUI.createItem(Material.GREEN_STAINED_GLASS_PANE, ChatColor.GREEN+ Messages.SHOP_TITLE_SELL.toString()));
					} else {
						Optional<RowStore> row = shop.getRow(y-1);
						if(row.isPresent()) {
							placeItem(y*9+x, row.get().getItemOut());
						}
					}
				} else if(x == 2) {
					if(y == 0) {
						placeItem(y*9+x, GUI.createItem(Material.GREEN_STAINED_GLASS_PANE, ChatColor.GREEN+ Messages.SHOP_TITLE_SELL2.toString()));
					} else {
						Optional<RowStore> row = shop.getRow(y-1);
						if(row.isPresent()) {
							placeItem(y*9+x, row.get().getItemOut2());
						}
					}
				} else if(x == 5) {
					if(y == 0) {
						placeItem(x, GUI.createItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED+ Messages.SHOP_TITLE_BUY.toString()));
					} else {
						Optional<RowStore> row = shop.getRow(y-1);
						if(row.isPresent()) {
							placeItem(y*9+x, row.get().getItemIn());
						}
					}
				} else if(x == 6) {
					if(y == 0) {
						placeItem(y*9+x, GUI.createItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED+ Messages.SHOP_TITLE_BUY2.toString()));
					} else {
						Optional<RowStore> row = shop.getRow(y-1);
						if(row.isPresent()) {
							placeItem(y*9+x, row.get().getItemIn2());
						}
					}
				}  else if(x == 8 && y == 0) {
					if(listAllShops) {
						placeItem(y*9+x, GUI.createItem(Material.END_CRYSTAL, Messages.SHOP_LIST_ALL.toString()), p -> {
							p.closeInventory();
							p.performCommand("shop listshops");
						});
					} else
						placeItem(y*9+x, GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, ""));
				} else if(x == 8 && y >= 1) {
					Optional<RowStore> row = shop.getRow(y-1);
					if(row.isPresent()) {
						final int index = y-1;
						placeItem(y*9+x, GUI.createItem(Material.LIME_DYE, ChatColor.BOLD+ Messages.SHOP_TITLE_BUYACTION.toString()), p -> {
							p.closeInventory();
							shop.buy(p, index);
						});
					} else {
						placeItem(y*9+x, GUI.createItem(Material.GRAY_DYE, ""));
					}
				} else {
					placeItem(y*9+x, GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, ""));
				}
			}
		}
	}
	
	private static String getShopName(Shop shop) {
		if(shop.isAdmin())
			return Messages.SHOP_TITLE_ADMIN_SHOP.toString();
		
		String msg = Messages.SHOP_TITLE_NORMAL_SHOP.toString();
		OfflinePlayer pl = Bukkit.getOfflinePlayer(shop.getOwner());
		if(pl == null)
			return msg.replaceAll("%player%", "<unknown>");
		
		return msg.replaceAll("%player%", pl.getName());
	}
}
