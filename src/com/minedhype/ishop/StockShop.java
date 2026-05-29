package com.minedhype.ishop;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;

public class StockShop {
	private static final List<StockShop> stocks = new ArrayList<>();
	private final UUID owner;
	private final Inventory inventory;
	private final int pag;
	private static boolean exemptStockListInactive;
	private static final long millisecondsPerDay = 86400000;
	public boolean isOwner(UUID owner) { return this.owner.equals(owner); }
	public static List<UUID> expiredStockOwners = new ArrayList<>();
	public static List<String> exemptStockExpiringList = iShop.config.getStringList("exemptExpiringStock");
	public static Optional<StockShop> getStockShopByOwner(UUID owner, int pag) { return stocks.parallelStream().filter(t -> t.owner.equals(owner) && t.pag == pag).findFirst(); }
	public static final ConcurrentHashMap<UUID, String> stockList = new ConcurrentHashMap<>();
	public static int maxStockDays = iShop.config.getInt("maxStockInactiveDays");
	public static void getPlayersStockList() { stocks.parallelStream().filter(s -> !s.inventory.isEmpty()).forEach(s -> stockList.putIfAbsent(s.owner, "Owner")); }
	public StockShop(UUID owner, int pag) { this(owner, Bukkit.createInventory(null, 45, ChatColor.GREEN + Bukkit.getOfflinePlayer(owner).getName()+"'s shop"), pag); }

	public StockShop(UUID owner, Inventory inv, int pag) {
		this.owner = owner;
		this.inventory = inv;
		this.pag = pag;
		stocks.add(this);
	}

	public static void purgePlayerStock(UUID stockOwner) {
		List<StockShop> stockDelete = new ArrayList<>();
		for(StockShop stock : stocks) {
			if(stock.isOwner(stockOwner))
				stockDelete.add(stock);
		}
		for(StockShop stock : stockDelete)
            stock.deleteStock(true, !expiredStockOwners.contains(stock.getOwner()));
	}

	public static void expiredStock() {
		if(maxStockDays <= 0)
			return;
		exemptStockListInactive = exemptStockExpiringList.size() == 1 && exemptStockExpiringList.get(0).equals("00000000-0000-0000-0000-000000000000");
		List<StockShop> stockDelete = new ArrayList<>();
		List<UUID> deleteCount = new ArrayList<>();
		for(StockShop stock : stocks) {
			if(stock.hasStockExpired()) {
				stockDelete.add(stock);
				stock.getInventory().clear();
				if(!deleteCount.contains(stock.getOwner()))
					deleteCount.add(stock.getOwner());
			}
		}
		for(StockShop stock : stockDelete)
			stock.deleteStock(true, !expiredStockOwners.contains(stock.getOwner()));
		if(!deleteCount.isEmpty())
			Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[iShop] Stock has expired for " + deleteCount.size() + " players.");
	}

	public boolean hasStockExpired() {
		if(!exemptStockListInactive)
			for(String exemptCheck : exemptStockExpiringList) {
				if(exemptCheck != null && exemptCheck.equalsIgnoreCase(this.owner.toString()))
					return false;
			}
		OfflinePlayer player = Bukkit.getOfflinePlayer(this.owner);
		if(player.isOnline())
			return false;
		return ((System.currentTimeMillis() - player.getLastPlayed()) / millisecondsPerDay) >= maxStockDays;
	}

	public void deleteStock(boolean removalOfStock, boolean deleteFromDatabase) {
		if(removalOfStock) {
			stocks.remove(this);
			if(deleteFromDatabase) {
				expiredStockOwners.add(owner);
				Bukkit.getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> {
					PreparedStatement stmt1 = null;
					try {
						stmt1 = iShop.getConnection().prepareStatement("DELETE FROM zooMercaStocks WHERE owner = ?;");
						stmt1.setString(1, owner.toString());
						stmt1.execute();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							if (stmt1 != null)
								stmt1.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		}
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
	
	private static boolean hasStock() { return (int) stocks.parallelStream().filter(stock -> Arrays.asList(stock.getInventory().getContents()).parallelStream().anyMatch(item -> item != null && item.getAmount() > 0)).count() > 0; }
	
	private void saveStockData() {
		PreparedStatement stmt = null;
		try {
			stmt = iShop.getConnection().prepareStatement("INSERT INTO zooMercaStocks (owner, itemsNew, pag) VALUES (?,?,?);");
			stmt.setString(1, owner.toString());
			stmt.setBytes(2,iShop.encodeByte(inventory.getContents()));
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
