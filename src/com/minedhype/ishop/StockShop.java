package com.minedhype.ishop;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.bukkit.inventory.ItemStack;

public class StockShop {
	private static final List<StockShop> stocks = new ArrayList<>();
	private final UUID owner;
	private final Inventory inventory;
	private final int pag;

	public StockShop(UUID owner, int pag) {
		this(owner, Bukkit.createInventory(null, 45, ChatColor.GREEN + Bukkit.getOfflinePlayer(owner).getName()+"'s shop"), pag);
	}

	public StockShop(UUID owner, Inventory inv, int pag) {
		this.owner = owner;
		this.inventory = inv;
		this.pag = pag;
		stocks.add(this);
	}

	public static Optional<StockShop> getStockShopByOwner(UUID owner, int pag) {
		return stocks.parallelStream().filter(t -> t.owner.equals(owner) && t.pag == pag).findFirst();
	}
	
	public static void saveData() {

		if(!hasStock())
			return;

		PreparedStatement stmt = null;
		try {
			stmt = iShop.getConnection().prepareStatement("DELETE FROM zooMercaStocks;");
			stmt.execute();
		} catch (Exception e) { e.printStackTrace(); }
			finally {
			try {
				if(stmt != null)
					stmt.close();
			} catch (Exception e) { e.printStackTrace(); }
		}

		for(StockShop stock : stocks)
			stock.saveStockData();
	}
	
	private static boolean hasStock() {
		return (int) stocks.parallelStream().filter(stock -> Arrays.asList(stock.getInventory().getContents()).parallelStream().anyMatch(item -> item != null && item.getAmount() > 0)).count() > 0;
	}
	
	private void saveStockData() {
		PreparedStatement stmt = null;
		try {
			stmt = iShop.getConnection().prepareStatement("INSERT INTO zooMercaStocks (owner, items, pag) VALUES (?,?,?);");

			JsonArray items = new JsonArray();
			for(ItemStack item : inventory.getContents()) {
				if(item == null)
					continue;

				YamlConfiguration config = new YamlConfiguration();
				item.serialize().forEach(config::set);
				String itemRaw = config.saveToString();
				items.add(itemRaw);
			}
			String itemsJson = (new Gson()).toJson(items);
			stmt.setString(1, owner.toString());
			stmt.setString(2, itemsJson);
			stmt.setInt(3, pag);
			stmt.execute();
		} catch (Exception e) { e.printStackTrace(); }
			finally {
			try {
				if(stmt != null)
					stmt.close();
			} catch (Exception e) { e.printStackTrace(); }
		}
	}

	public Inventory getInventory() {
		return inventory;
	}
	
	public void setInventory(Inventory inventory) {
		for(int i=0; i<45; i++)
			this.inventory.setItem(i, inventory.getItem(i));
	}

	public UUID getOwner() {
		return owner;
	}
}
