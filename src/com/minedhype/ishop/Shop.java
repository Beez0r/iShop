package com.minedhype.ishop;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.minedhype.ishop.inventories.InvStock;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class Shop {
	private static final Plugin plugin = Bukkit.getPluginManager().getPlugin("iShop");
	private static final List<Shop> shops = new ArrayList<>();
	private final Map<Player, Long> cdTime = new HashMap<>();
	private int idTienda;
	private final UUID owner;
	private final Location location;
	private final RowStore[] rows;
	private final boolean admin;
	
	private Shop(int idTienda, UUID owner, Location loc, boolean admin) {
		this.idTienda = idTienda;
		this.owner = owner;
		this.location = loc;
		this.rows = new RowStore[4];
		this.admin = admin;
		
		if(idTienda == -1) {
			final Shop shop = this;
			Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				PreparedStatement stmt = null;
				try {
					stmt = iShop.getConnection().prepareStatement("INSERT INTO zooMercaTiendas (location, owner, admin) VALUES (?,?,?);", 1);
					String locationRaw = loc.getBlockX()+";"+loc.getBlockY()+";"+loc.getBlockZ()+";"+ loc.getWorld().getName();
					stmt.setString(1, locationRaw);
					stmt.setString(2, owner.toString());
					stmt.setBoolean(3, admin);
					stmt.executeUpdate();
					ResultSet res = stmt.getGeneratedKeys();
					if(res.next())
						shop.idTienda = res.getInt(1);
				} catch (Exception e) { e.printStackTrace(); }
					finally {
					try {
						if(stmt != null)
							stmt.close();
					} catch (Exception e) { e.printStackTrace(); }
				}
			});
		}
	}
	
	public static Optional<Shop> getShopByLocation(Location location) {
		return shops.parallelStream().filter(shop -> shop.location.equals(location)).findFirst();
	}
	
	public static Optional<Shop> getShopById(int id) {
		return shops.parallelStream().filter(shop -> shop.idTienda == id).findFirst();
	}
	
	public static int getNumShops(UUID owner) {
		return (int) shops.parallelStream().filter(t -> !t.admin && t.owner.equals(owner)).count();
	}

	public static void getShopList(Player player, UUID sOwner, String pOwner) {
		player.sendMessage(ChatColor.GOLD + "Found " + ChatColor.GREEN + getNumShops(sOwner) + ChatColor.GOLD + " shop(s) for player: " + ChatColor.GREEN + pOwner);
		shops.parallelStream()
				.filter(s -> !s.admin && s.isOwner(sOwner))
				.forEach(s -> {
					if(s.isOwner(player.getUniqueId()) && iShop.config.getBoolean("remoteManage")) {
						String manageMessage = ChatColor.GOLD + "Shop id " + ChatColor.GREEN + (s.idTienda) + ChatColor.GOLD + " Location XYZ: " + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName();
						TextComponent manageMsg = new TextComponent(manageMessage);
						TextComponent manageText = new TextComponent(ChatColor.DARK_GRAY + " [" + ChatColor.GOLD + "MANAGE" + ChatColor.DARK_GRAY + "]");
						manageText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop manage "+s.idTienda));
						manageText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GOLD + "Manage this shop!")));
						manageMsg.addExtra(manageText);
						player.spigot().sendMessage(manageMsg);
					} else if(player.hasPermission(Permission.SHOP_ADMIN.toString())) {
						String manageMessage = ChatColor.GOLD + "Shop id " + ChatColor.GREEN + (s.idTienda) + ChatColor.GOLD + " Location XYZ: " + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName();
						TextComponent manageMsg = new TextComponent(manageMessage);
						TextComponent manageText = new TextComponent(ChatColor.DARK_GRAY + " [" + ChatColor.GOLD + "MANAGE" + ChatColor.DARK_GRAY + "]");
						manageText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop manage " + s.idTienda));
						manageText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GOLD + "Manage this shop!")));
						manageMsg.addExtra(manageText);
						String shopMessage = "";
						if(!s.isOwner(player.getUniqueId())) {
							TextComponent shopMsg = new TextComponent(shopMessage);
							TextComponent shopText = new TextComponent(ChatColor.DARK_GRAY + " [" + ChatColor.GOLD + "SHOP" + ChatColor.DARK_GRAY + "]");
							shopText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop view " + s.idTienda));
							shopText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GOLD + "Remotely shop here!")));
							shopMsg.addExtra(shopText);
							player.spigot().sendMessage(manageMsg, shopMsg);
						} else
							player.spigot().sendMessage(manageMsg);
					} else if(iShop.config.getBoolean("remoteShopping") && !s.isOwner(player.getUniqueId())) {
						String shopMessage = ChatColor.GOLD + "Shop id " + ChatColor.GREEN + (s.idTienda) + ChatColor.GOLD + " Location XYZ: " + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName();
						TextComponent shopMsg = new TextComponent(shopMessage);
						TextComponent shopText = new TextComponent(ChatColor.DARK_GRAY + " [" + ChatColor.GOLD + "SHOP" + ChatColor.DARK_GRAY + "]");
						shopText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop view " + s.idTienda));
						shopText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GOLD + "Remotely shop here!")));
						shopMsg.addExtra(shopText);
						player.spigot().sendMessage(shopMsg);
					} else
						player.sendMessage(ChatColor.GOLD + "Shop id " + ChatColor.GREEN + (s.idTienda) + ChatColor.GOLD + " Location XYZ: " + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName());
				});
	}

	public static void getAdminShopList(Player player) {
		player.sendMessage(ChatColor.GOLD + "Listing all found admin shops:");
		shops.parallelStream()
				.filter(s -> s.admin)
				.forEach(s -> {
					if(player.hasPermission(Permission.SHOP_ADMIN.toString())) {
						String manageMessage = ChatColor.GOLD + "Shop id " + ChatColor.GREEN + (s.idTienda) + ChatColor.GOLD + " Location XYZ: " + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName();
						TextComponent manageMsg = new TextComponent(manageMessage);
						TextComponent manageText = new TextComponent(ChatColor.DARK_GRAY + " [" + ChatColor.GOLD + "MANAGE" + ChatColor.DARK_GRAY + "]");
						manageText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop manage " + s.idTienda));
						manageText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GOLD + "Manage this shop!")));
						manageMsg.addExtra(manageText);
						player.spigot().sendMessage(manageMsg);
						}});
	}
	
	public static Shop createShop(Location loc, UUID owner) {
		return createShop(loc, owner, false);
	}
	
	public static Shop createShop(Location loc, UUID owner, boolean admin) {
		Shop shop = new Shop(-1, owner, loc, admin);
		shops.add(shop);
		Optional<StockShop> stockShop = StockShop.getStockShopByOwner(owner, 0);
		if(!stockShop.isPresent())
			new StockShop(owner, 0);
		
		return shop;
	}
	
	public static void tickShops() {
		if(!iShop.config.getBoolean("enableShopBlock"))
			return;

		if(iShop.config.getBoolean("showParticles"));
		for(Shop shop : shops)
			if(shop.hasItems()) {
				double x = shop.location.getBlockX() + 0.5;
				double y = shop.location.getBlockY() + 1.25;
				double z = shop.location.getBlockZ() + 0.5;
				shop.location.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, x, y, z, 10, 0.1, 0.1, 0.1);
			}
	}

	public static void expiredShops() {
		List<Shop> shopDelete = new ArrayList<>();
		for(Shop shop : shops)
			if(shop.hasExpired() || shop.location.getWorld() == null)
				shopDelete.add(shop);

		for(Shop shop : shopDelete)
			shop.deleteShop();
	}
	
	public static void loadData() {
		PreparedStatement loadStocks = null;
		PreparedStatement loadRows = null;
		PreparedStatement loadShops = null;
		
		try {
			loadStocks = iShop.getConnection().prepareStatement("SELECT owner, items, pag FROM zooMercaStocks;");
			ResultSet dataStocks = loadStocks.executeQuery();
			while(dataStocks.next()) {
				String ownerRaw = dataStocks.getString(1);
				UUID owner = UUID.fromString(ownerRaw);
				String dataItems = dataStocks.getString(2);
				List<ItemStack> itemsList = new ArrayList<>();
				
				JsonArray itemsArray = new JsonParser().parse(dataItems).getAsJsonArray();
				for(JsonElement jsonItem : itemsArray) {
					String itemstackRaw = jsonItem.getAsString();
					YamlConfiguration config = new YamlConfiguration();
					try {
						config.loadFromString(itemstackRaw);
					} catch (InvalidConfigurationException e) { e.printStackTrace(); }
					Map<String, Object> itemRaw = config.getValues(true);
					itemsList.add(ItemStack.deserialize(itemRaw));
				}

				int pag = dataStocks.getInt(3);
				StockShop stock = new StockShop(owner, pag);
				stock.getInventory().setContents(itemsList.toArray(new ItemStack[0]));
			}
			
			loadShops = iShop.getConnection().prepareStatement("SELECT location, owner, itemIn, itemOut, idTienda, admin, broadcast FROM zooMercaTiendasFilas LEFT JOIN zooMercaTiendas ON id = idTienda ORDER BY idTienda;");
			ResultSet dataStore = loadShops.executeQuery();
			while(dataStore.next()) {
				String[] locationRaw = dataStore.getString(1).split(";");
				int x = Integer.parseInt(locationRaw[0]);
				int y = Integer.parseInt(locationRaw[1]);
				int z = Integer.parseInt(locationRaw[2]);
				World world = Bukkit.getWorld(locationRaw[3]);
				if(world == null)
					continue;
				
				Location location = new Location(world, x, y, z);
				Optional<Shop> shop = Shop.getShopByLocation(location);
				if(!shop.isPresent()) {
					String ownerRaw = dataStore.getString(2);
					UUID owner = UUID.fromString(ownerRaw);
					int idTienda = dataStore.getInt(5);
					boolean admin = dataStore.getBoolean(6);
					shops.add(new Shop(idTienda, owner, location, admin));
					shop = Shop.getShopByLocation(location);
				}
				
				RowStore[] rows = shop.get().getRows();
				int index = 0;
				for(int len=rows.length; index<len; index++) {
					RowStore row = rows[index];
					if(row == null)
						break;
				}
				if(index >= rows.length)
					continue;
								
				String itemInstackRaw = dataStore.getString(3);
				YamlConfiguration configIn = new YamlConfiguration();
				String itemOutstackRaw = dataStore.getString(4);
				YamlConfiguration configOut = new YamlConfiguration();
				try {
					configIn.loadFromString(itemInstackRaw);
					configOut.loadFromString(itemOutstackRaw);
				} catch (InvalidConfigurationException e) { e.printStackTrace(); }
				Map<String, Object> itemInRaw = configIn.getValues(true);
				ItemStack itemIn = ItemStack.deserialize(itemInRaw);
				Map<String, Object> itemOutRaw = configOut.getValues(true);
				ItemStack itemOut = ItemStack.deserialize(itemOutRaw);
				boolean broadcast = dataStore.getBoolean(7);
				rows[index] = new RowStore(itemOut, itemIn, broadcast);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
			Bukkit.shutdown();
		} finally {
			try {
				if(loadStocks != null)
					loadStocks.close();
				if(loadRows != null)
					loadRows.close();
				if(loadShops != null)
					loadShops.close();
			} catch (Exception e) { e.printStackTrace(); }
		}
	}
	
	public static void saveData() {
		StockShop.saveData();
		PreparedStatement stmt = null;
		try {
			stmt = iShop.getConnection().prepareStatement("DELETE FROM zooMercaTiendasFilas;");
			stmt.execute();
		} catch (Exception e) { e.printStackTrace(); }
			finally {
			try {
				if(stmt != null)
					stmt.close();
			} catch (Exception e) { e.printStackTrace(); }
		}

		for(Shop shop : shops)
			shop.saveDataShop();
	}
	
	public boolean hasItems() {
		for(RowStore row : rows)
			if(row != null)
				return true;
		
		return false;
	}
	
	private void saveDataShop() {
		for(RowStore row : rows)
			if(row != null)
				row.saveData(idTienda);
	}
	
	public void buy(Player player, int index) {
		Optional<RowStore> row = getRow(index);
		if(!row.isPresent())
			return;
		
		if(!Utils.hasStock(player, row.get().getItemIn())) {
			player.sendMessage(Messages.SHOP_NO_ITEMS.toString());
			return;
		}
		if(!Utils.hasStock(this, row.get().getItemOut())) {
			player.sendMessage(Messages.SHOP_NO_STOCK.toString());

			Player ownerPlayer = Bukkit.getPlayer(owner);
			if(cdTime.containsKey(ownerPlayer)) {
				int cdTimeInSec = iShop.config.getInt("noStockCooldown");
				long secondsLeft = ((cdTime.get(ownerPlayer)/1000)+cdTimeInSec) - (System.currentTimeMillis()/1000);
				if(ownerPlayer != null && ownerPlayer.isOnline() && secondsLeft < 0) {
					ownerPlayer.sendMessage(Messages.SHOP_NO_STOCK_SHELF.toString().replaceAll("%s", row.get().getItemOut().getType().toString()));
					cdTime.put(ownerPlayer, System.currentTimeMillis());
				}
			} else {
				cdTime.put(ownerPlayer, System.currentTimeMillis());
				if(ownerPlayer != null && ownerPlayer.isOnline())
					ownerPlayer.sendMessage(Messages.SHOP_NO_STOCK_SHELF.toString().replaceAll("%s", row.get().getItemOut().getType().toString()));
			  }
			
			return;
		}

		if(player.getInventory().firstEmpty() != -1) {
			player.getInventory().removeItem(row.get().getItemIn().clone());
			player.getInventory().addItem(row.get().getItemOut().clone());
		} else {
			player.sendMessage(Messages.PLAYER_INV_FULL.toString());
			return;
		}
		
		String nameIn = row.get().getItemIn().getItemMeta().hasDisplayName() ? row.get().getItemIn().getItemMeta().getDisplayName() : row.get().getItemIn().getType().name().replaceAll("_", " ").toLowerCase();
		String nameOut = row.get().getItemOut().getItemMeta().hasDisplayName() ? row.get().getItemOut().getItemMeta().getDisplayName() : row.get().getItemOut().getType().name().replaceAll("_", " ").toLowerCase();
		
		player.sendMessage(Messages.SHOP_PURCHASE.toString()
				.replaceAll("%in", nameOut + " x "+row.get().getItemOut().getAmount())
				.replaceAll("%out", nameIn + " x "+row.get().getItemIn().getAmount())
		);
		
		if(!this.admin) {
			this.takeItem(row.get().getItemOut().clone());
			this.giveItem(row.get().getItemIn().clone());
			
			Player ownerPlayer = Bukkit.getPlayer(owner);
			if(ownerPlayer != null && ownerPlayer.isOnline()) {
				ownerPlayer.sendMessage(Messages.SHOP_SELL.toString()
					.replaceAll("%in", nameOut + " x "+row.get().getItemOut().getAmount())
					.replaceAll("%out", nameIn + " x "+row.get().getItemIn().getAmount())
					.replaceAll("%p", player.getName()));
			}
		} else if(row.get().broadcast) {
			Bukkit.broadcastMessage(Messages.SHOP_SELL.toString()
					.replaceAll("%in", nameOut + " x "+row.get().getItemOut().getAmount())
					.replaceAll("%out", nameIn + " x "+row.get().getItemIn().getAmount())
					.replaceAll("%p", player.getName()));
		}
	}
	
	public boolean hasExpired() {
		if(this.admin)
			return false;

		int maxDays = iShop.config.getInt("maxInactiveDays");
		if(maxDays <= 0)
			return false;
		
		OfflinePlayer player = Bukkit.getOfflinePlayer(this.owner);
		if(player.isOnline())
			return false;
		long millisecondsPerDay = 86400000;
		return ((System.currentTimeMillis() - player.getLastPlayed()) / millisecondsPerDay) >= maxDays;
	}
	
	public void giveItem(ItemStack item) {
		ItemStack copy = item.clone();
		int max = iShop.config.getInt("stockPages");
		for(int i=0; i<max; i++) {
			Optional<StockShop> stock = StockShop.getStockShopByOwner(this.owner, i);
			if(!stock.isPresent())
				continue;
			
			Map<Integer, ItemStack> res = stock.get().getInventory().addItem(copy);
			if(res.isEmpty())
				break;
			else
				copy.setAmount(res.get(0).getAmount());
		}		
		
		InvStock.getInvStock(this.owner).refreshItems();
	}
	
	public void takeItem(ItemStack item) {
		ItemStack copy = item.clone();
		
		int max = iShop.config.getInt("stockPages");
		for(int i=0; i<max; i++) {
			Optional<StockShop> stock = StockShop.getStockShopByOwner(this.owner, i);
			if(!stock.isPresent())
				continue;
			
			Map<Integer, ItemStack> res = stock.get().getInventory().removeItem(copy);
			if(res.isEmpty())
				break;
			else
				copy.setAmount(res.get(0).getAmount());
		}
		
		InvStock.getInvStock(this.owner).refreshItems();
	}
	
	public void delete(Player player, int index) {
		rows[index] = null;
	}
	
	public void deleteShop() {
		deleteShop(true);
	}
	
	public void deleteShop(boolean removalOfArray) {
		if(removalOfArray) {
			shops.remove(this);
			if(iShop.config.getBoolean("deleteBlock"))
				this.location.getBlock().setType(Material.AIR);
		}

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			PreparedStatement stmt1 = null;
			PreparedStatement stmt2 = null;
			try {
				stmt1 = iShop.getConnection().prepareStatement("DELETE FROM zooMercaTiendasFilas WHERE idTienda = ?;");
				stmt1.setInt(1, idTienda);
				stmt1.execute();
				
				stmt2 = iShop.getConnection().prepareStatement("DELETE FROM zooMercaTiendas WHERE id = ?;");
				stmt2.setInt(1, idTienda);
				stmt2.execute();
			} catch (Exception e) { e.printStackTrace(); }
				finally {
				try {
					if(stmt1 != null) {
						stmt1.close();
					}
					if(stmt2 != null) {
						stmt2.close();
					}
				} catch (Exception e) { e.printStackTrace(); }
			}
		});
	}
	
	public Optional<RowStore> getRow(int index) {
		if(index < 0 || index > 3)
			return Optional.empty();
		
		return Optional.ofNullable(rows[index]);
	}
	
	public RowStore[] getRows() {
		return rows;
	}
	
	public boolean isOwner(UUID owner) {		
		return this.owner.equals(owner);
	}
	
	public boolean isAdmin() {
		return this.admin;
	}
	
	public UUID getOwner() {
		return owner;
	}
	
	public Location getLocation() {
		return location;
	}
}
