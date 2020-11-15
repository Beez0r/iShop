package com.minedhype.ishop;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.minedhype.ishop.inventories.InvAdminShop;
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
import net.md_5.bungee.api.chat.TextComponent;

public class Shop {
	public static boolean shopOutStock = iShop.config.getBoolean("enableOutOfStockMessages");
	public static boolean shopEnabled = iShop.config.getBoolean("enableShopBlock");
	public static boolean showOwnedShops = iShop.config.getBoolean("publicShopListShowsOwned");
	public static boolean shopNotifications = iShop.config.getBoolean("enableShopNotifications");
	public static boolean particleEffects = iShop.config.getBoolean("showParticles");
	public static int maxDays = iShop.config.getInt("maxInactiveDays");
	public static final ConcurrentHashMap<Integer, UUID> shopList = new ConcurrentHashMap<>();
	private static final List<Shop> shops = new ArrayList<>();
	private static final Plugin plugin = Bukkit.getPluginManager().getPlugin("iShop");
	private static final long millisecondsPerDay = 86400000;
	private final ItemStack airItem = new ItemStack(Material.AIR, 0);
	private final Map<Player, Long> cdTime = new HashMap<>();
	private final UUID owner;
	private final Location location;
	private final RowStore[] rows;
	private final boolean admin;
	private int idTienda;

	private Shop(int idTienda, UUID owner, Location loc, boolean admin) {
		this.idTienda = idTienda;
		this.owner = owner;
		this.location = loc;
		this.rows = new RowStore[5];
		this.admin = admin;

		if(idTienda == -1) {
			final Shop shop = this;
			Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				PreparedStatement stmt = null;
				try {
					stmt = iShop.getConnection().prepareStatement("INSERT INTO zooMercaTiendas (location, owner, admin) VALUES (?,?,?);", Statement.RETURN_GENERATED_KEYS);
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

	public static Optional<Shop> getShopByLocation(Location location) { return shops.parallelStream().filter(shop -> shop.location.equals(location)).findFirst(); }
	public static Optional<Shop> getShopById(int id) { return shops.parallelStream().filter(shop -> shop.idTienda == id).findFirst(); }
	public static int getNumShops(UUID owner) { return (int) shops.parallelStream().filter(t -> !t.admin && t.owner.equals(owner)).count(); }

	public static void getPlayersShopList() {
		if(iShop.config.getBoolean("adminShopPublic"))
			shops.parallelStream().forEach(s -> shopList.putIfAbsent(s.idTienda, s.owner));
		else {
			shops.parallelStream()
					.filter(s -> !s.admin)
					.forEach(s -> shopList.putIfAbsent(s.idTienda, s.owner));
		}
	}

	public static void getShopList(Player player, UUID sOwner, String pOwner) {
		player.sendMessage(Messages.SHOP_FOUND.toString().replaceAll("%p", pOwner).replaceAll("%shops", String.valueOf(getNumShops(sOwner))));
		shops.parallelStream()
				.filter(s -> !s.admin && s.isOwner(sOwner))
				.forEach(s -> {
					if(s.isOwner(player.getUniqueId()) && InvAdminShop.remoteManage) {
						String manageMessage = Messages.SHOP_LOCATION.toString().replaceAll("%id", String.valueOf(s.idTienda)) + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName();
						TextComponent manageMsg = new TextComponent(manageMessage);
						TextComponent manageText = new TextComponent(ChatColor.DARK_GRAY + " [" + Messages.SHOP_CLICK_MANAGE.toString() + ChatColor.DARK_GRAY + "]");
						manageText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop manage " + s.idTienda));
						manageMsg.addExtra(manageText);
						player.spigot().sendMessage(manageMsg);
					} else if(player.hasPermission(Permission.SHOP_ADMIN.toString())) {
						String manageMessage = Messages.SHOP_LOCATION.toString().replaceAll("%id", String.valueOf(s.idTienda)) + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName();
						TextComponent manageMsg = new TextComponent(manageMessage);
						TextComponent manageText = new TextComponent(ChatColor.DARK_GRAY + " [" + Messages.SHOP_CLICK_MANAGE.toString() + ChatColor.DARK_GRAY + "]");
						manageText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop manage " + s.idTienda));
						manageMsg.addExtra(manageText);
						String shopMessage = "";
						if(!s.isOwner(player.getUniqueId())) {
							TextComponent shopMsg = new TextComponent(shopMessage);
							TextComponent shopText = new TextComponent(ChatColor.DARK_GRAY + " [" + Messages.SHOP_CLICK_SHOP.toString() + ChatColor.DARK_GRAY + "]");
							shopText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop view " + s.idTienda));
							shopMsg.addExtra(shopText);
							player.spigot().sendMessage(manageMsg, shopMsg);
						} else
							player.spigot().sendMessage(manageMsg);
					} else if(iShop.config.getBoolean("remoteShopping") && !s.isOwner(player.getUniqueId())) {
						String shopMessage = Messages.SHOP_LOCATION.toString().replaceAll("%id", String.valueOf(s.idTienda)) + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName();
						TextComponent shopMsg = new TextComponent(shopMessage);
						TextComponent shopText = new TextComponent(ChatColor.DARK_GRAY + " [" + Messages.SHOP_CLICK_SHOP.toString() + ChatColor.DARK_GRAY + "]");
						shopText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop view " + s.idTienda));
						shopMsg.addExtra(shopText);
						player.spigot().sendMessage(shopMsg);
					} else
						player.sendMessage(Messages.SHOP_LOCATION.toString().replaceAll("%id", String.valueOf(s.idTienda)) + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName());
				});
	}

	public static void getAdminShopList(Player player) {
		if(player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.SHOP_LIST_ADMINSHOPS.toString());
			AtomicInteger shopCount = new AtomicInteger(0);
			shops.parallelStream()
					.filter(s -> s.admin)
					.forEach(s -> {
						String manageMessage = Messages.SHOP_LOCATION.toString().replaceAll("%id", String.valueOf(s.idTienda)) + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName();
						TextComponent manageMsg = new TextComponent(manageMessage);
						TextComponent manageText = new TextComponent(ChatColor.DARK_GRAY + " [" + Messages.SHOP_CLICK_MANAGE.toString() + ChatColor.DARK_GRAY + "]");
						manageText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop manage " + s.idTienda));
						manageMsg.addExtra(manageText);
						player.spigot().sendMessage(manageMsg);
						shopCount.getAndIncrement();
					});
			if(shopCount.get() == 0)
				player.sendMessage(Messages.SHOP_NO_ADMINSHOPS_FOUND.toString());
		}
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
		if(!shopEnabled)
			return;
		if(particleEffects) {
			for(Shop shop : shops)
				if(shop.hasItems()) {
					double x = shop.location.getBlockX() + 0.5;
					double y = shop.location.getBlockY() + 1.25;
					double z = shop.location.getBlockZ() + 0.5;
					shop.location.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, x, y, z, 10, 0.1, 0.1, 0.1);
				}
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
			loadShops = iShop.getConnection().prepareStatement("SELECT location, owner, itemIn, itemIn2, itemOut, itemOut2, idTienda, admin, broadcast FROM zooMercaTiendasFilas LEFT JOIN zooMercaTiendas ON id = idTienda ORDER BY idTienda;");
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
					int idTienda = dataStore.getInt(7);
					boolean admin = dataStore.getBoolean(8);
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
				String itemInstack1Raw = dataStore.getString(3);
				YamlConfiguration configIn1 = new YamlConfiguration();
				String itemInstack2Raw = dataStore.getString(4);
				YamlConfiguration configIn2 = new YamlConfiguration();
				String itemOutstack1Raw = dataStore.getString(5);
				YamlConfiguration configOut1 = new YamlConfiguration();
				String itemOutstack2Raw = dataStore.getString(6);
				YamlConfiguration configOut2 = new YamlConfiguration();
				try {
					configIn1.loadFromString(itemInstack1Raw);
					configIn2.loadFromString(itemInstack2Raw);
					configOut1.loadFromString(itemOutstack1Raw);
					configOut2.loadFromString(itemOutstack2Raw);
				} catch (InvalidConfigurationException e) { e.printStackTrace(); }
				Map<String, Object> itemInRaw = configIn1.getValues(true);
				ItemStack itemIn = ItemStack.deserialize(itemInRaw);
				Map<String, Object> itemIn2Raw = configIn2.getValues(true);
				ItemStack itemIn2 = ItemStack.deserialize(itemIn2Raw);
				Map<String, Object> itemOutRaw = configOut1.getValues(true);
				ItemStack itemOut = ItemStack.deserialize(itemOutRaw);
				Map<String, Object> itemOut2Raw = configOut2.getValues(true);
				ItemStack itemOut2 = ItemStack.deserialize(itemOut2Raw);
				boolean broadcast = dataStore.getBoolean(9);
				rows[index] = new RowStore(itemOut, itemOut2, itemIn, itemIn2, broadcast);
			}

		} catch(Exception e) {
			e.printStackTrace();
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[iShop] Failed to load database properly! Shutting down to prevent data corruption.");
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
		if(row.get().getItemIn().isSimilar(row.get().getItemIn2()) && !Utils.hasDoubleItemStock(player, row.get().getItemIn(), row.get().getItemIn2())) {
				player.sendMessage(Messages.SHOP_NO_ITEMS.toString());
				return;
		} else if(!Utils.hasStock(player, row.get().getItemIn()) || !Utils.hasStock(player, row.get().getItemIn2())) {
				player.sendMessage(Messages.SHOP_NO_ITEMS.toString());
				return;
		}
		if(row.get().getItemOut().isSimilar(row.get().getItemOut2()) && !Utils.hasDoubleItemStock(this, row.get().getItemOut(), row.get().getItemOut().getAmount() + row.get().getItemOut2().getAmount())) {
				player.sendMessage(Messages.SHOP_NO_STOCK.toString());
				if(shopOutStock) {
					final Player ownerPlayer = Bukkit.getPlayer(owner);
					final String itemString = row.get().getItemOut().getType().toString();
					Bukkit.getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> outOfStockItem(ownerPlayer, itemString));
				}
				return;
		} else {
			if(!Utils.hasStock(this, row.get().getItemOut())) {
				player.sendMessage(Messages.SHOP_NO_STOCK.toString());
				if(shopOutStock) {
					final Player ownerPlayer = Bukkit.getPlayer(owner);
					final String itemString = row.get().getItemOut().getType().toString();
					Bukkit.getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> outOfStockItem(ownerPlayer, itemString));
				}
				return;
			}
			if(!Utils.hasStock(this, row.get().getItemOut2())) {
				player.sendMessage(Messages.SHOP_NO_STOCK.toString());
				if(shopOutStock) {
					final Player ownerPlayer = Bukkit.getPlayer(owner);
					final String itemString = row.get().getItemOut2().getType().toString();
					Bukkit.getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> outOfStockItem(ownerPlayer, itemString));
				}
				return;
			}
		}
		if(player.getInventory().firstEmpty() != -1) {
			if(!row.get().getItemIn().isSimilar(airItem))
				player.getInventory().removeItem(row.get().getItemIn().clone());
			if(!row.get().getItemIn2().isSimilar(airItem))
				player.getInventory().removeItem(row.get().getItemIn2().clone());
			if(!row.get().getItemOut().isSimilar(airItem))
				player.getInventory().addItem(row.get().getItemOut().clone());
			if(!row.get().getItemOut2().isSimilar(airItem))
				player.getInventory().addItem(row.get().getItemOut2().clone());
		} else {
			player.sendMessage(Messages.PLAYER_INV_FULL.toString());
			return;
		}
		String nameIn1, nameIn2, nameOut1, nameOut2;
		int inA1, inA2, outA1, outA2;
		try {
			nameIn1 = row.get().getItemIn().getItemMeta().hasDisplayName() ? row.get().getItemIn().getItemMeta().getDisplayName() : row.get().getItemIn().getType().name().replaceAll("_", " ").toLowerCase();
			inA1 = row.get().getItemIn().getAmount();
		} catch(Exception e) { nameIn1 = "empty"; inA1 = 0; }
		try {
			nameIn2 = row.get().getItemIn2().getItemMeta().hasDisplayName() ? row.get().getItemIn2().getItemMeta().getDisplayName() : row.get().getItemIn2().getType().name().replaceAll("_", " ").toLowerCase();
			inA2 = row.get().getItemIn2().getAmount();
		} catch(Exception e) { nameIn2 = "empty"; inA2 = 0; }
		try {
			nameOut1 = row.get().getItemOut().getItemMeta().hasDisplayName() ? row.get().getItemOut().getItemMeta().getDisplayName() : row.get().getItemOut().getType().name().replaceAll("_", " ").toLowerCase();
			outA1 = row.get().getItemOut().getAmount();
		} catch(Exception e) { nameOut1 = "empty"; outA1 = 0; }
		try {
			nameOut2 = row.get().getItemOut2().getItemMeta().hasDisplayName() ? row.get().getItemOut2().getItemMeta().getDisplayName() : row.get().getItemOut2().getType().name().replaceAll("_", " ").toLowerCase();
			outA2 = row.get().getItemOut2().getAmount();
		} catch(Exception e) { nameOut2 = "empty"; outA2 = 0; }
		if(!this.admin) {
			if(row.get().getItemOut() != null || !row.get().getItemOut().getType().isAir())
				this.takeItem(row.get().getItemOut().clone());
			if(row.get().getItemOut2() != null || !row.get().getItemOut2().getType().isAir())
				this.takeItem(row.get().getItemOut2().clone());
			if(row.get().getItemIn() != null || !row.get().getItemIn().getType().isAir())
				this.giveItem(row.get().getItemIn().clone());
			if(row.get().getItemIn2() != null || !row.get().getItemIn2().getType().isAir())
				this.giveItem(row.get().getItemIn2().clone());
		}
		Player ownerPlayer = Bukkit.getPlayer(owner);
		final boolean rowBroadcast = row.get().broadcast;
		final String i1 = nameIn1 + " x " + inA1;
		final String i2 = nameIn2 + " x " + inA2;
		final String o1 = nameOut1 + " x " + outA1;
		final String o2 = nameOut2 + " x " + outA2;
		final int iA1 = inA1;
		final int iA2 = inA2;
		final int oA1 = outA1;
		final int oA2 = outA2;
		Bukkit.getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> sendShopMessages(i1, i2, o1, o2, iA1, iA2, oA1, oA2, ownerPlayer, player, this.admin, rowBroadcast));
	}

	public boolean hasExpired() {
		if(this.admin || maxDays <= 0)
			return false;
		OfflinePlayer player = Bukkit.getOfflinePlayer(this.owner);
		if(player.isOnline())
			return false;
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
					if(stmt1 != null)
						stmt1.close();
					if(stmt2 != null)
						stmt2.close();
				} catch (Exception e) { e.printStackTrace(); }
			}
		});
	}

	public void sendShopMessages(String i1, String i2, String o1, String o2, int inA1, int inA2, int outA1, int outA2, Player ownerPlayer, Player player, boolean isAdminShop, boolean rowBroadcast) {
		if(!isAdminShop) {
			if(ownerPlayer != null && ownerPlayer.isOnline() && shopNotifications) {
				if(inA1 == 0 && outA1 == 0)
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o2).replaceAll("%out", i2).replaceAll("%p", player.getName()));
				else if(inA1 == 0 && outA2 == 0)
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1).replaceAll("%out", i2).replaceAll("%p", player.getName()));
				else if(inA2 == 0 && outA1 == 0)
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o2).replaceAll("%out", i1).replaceAll("%p", player.getName()));
				else if(inA2 == 0 && outA2 == 0)
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1).replaceAll("%out", i1).replaceAll("%p", player.getName()));
				else if(inA1 == 0)
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i2).replaceAll("%p", player.getName()));
				else if(inA2 == 0)
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i1).replaceAll("%p", player.getName()));
				else if(outA1 == 0)
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o2).replaceAll("%out", i1 + " & " + i2).replaceAll("%p", player.getName()));
				else if(outA2 == 0)
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1).replaceAll("%out", i1 + " & " + i2).replaceAll("%p", player.getName()));
				else
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i1 + " & " + i2).replaceAll("%p", player.getName()));
			}
		} else if(rowBroadcast) {
			if(inA1 == 0 && outA1 == 0)
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o2).replaceAll("%out", i2).replaceAll("%p", player.getName()));
			else if(inA1 == 0 && outA2 == 0)
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1).replaceAll("%out", i2).replaceAll("%p", player.getName()));
			else if(inA2 == 0 && outA1 == 0)
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o2).replaceAll("%out", i1).replaceAll("%p", player.getName()));
			else if(inA2 == 0 && outA2 == 0)
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1).replaceAll("%out", i1).replaceAll("%p", player.getName()));
			else if(inA1 == 0)
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i2).replaceAll("%p", player.getName()));
			else if(inA2 == 0)
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i1).replaceAll("%p", player.getName()));
			else if(outA1 == 0)
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o2).replaceAll("%out", i1 + " & " + i2).replaceAll("%p", player.getName()));
			else if(outA2 == 0)
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1).replaceAll("%out", i1 + " & " + i2).replaceAll("%p", player.getName()));
			else
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i1 + " & " + i2).replaceAll("%p", player.getName()));
		}
		if(!rowBroadcast && shopNotifications) {
			if(inA1 == 0 && outA1 == 0)
				player.sendMessage(Messages.SHOP_PURCHASE.toString().replaceAll("%in", o2).replaceAll("%out", i2));
			else if(inA1 == 0 && outA2 == 0)
				player.sendMessage(Messages.SHOP_PURCHASE.toString().replaceAll("%in", o1).replaceAll("%out", i2));
			else if(inA2 == 0 && outA1 == 0)
				player.sendMessage(Messages.SHOP_PURCHASE.toString().replaceAll("%in",o2).replaceAll("%out", i1));
			else if(inA2 == 0 && outA2 == 0)
				player.sendMessage(Messages.SHOP_PURCHASE.toString().replaceAll("%in", o1).replaceAll("%out", i1));
			else if(inA1 == 0)
				player.sendMessage(Messages.SHOP_PURCHASE.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i2));
			else if(inA2 == 0)
				player.sendMessage(Messages.SHOP_PURCHASE.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i1));
			else if(outA1 == 0)
				player.sendMessage(Messages.SHOP_PURCHASE.toString().replaceAll("%in", o2).replaceAll("%out", i1 + " & " + i2));
			else if(outA2 == 0)
				player.sendMessage(Messages.SHOP_PURCHASE.toString().replaceAll("%in", o1).replaceAll("%out", i1 + " & " + i2));
			else
				player.sendMessage(Messages.SHOP_PURCHASE.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i1 + " & " + i2));
		}
	}

	public void outOfStockItem(Player ownerPlayer, String itemString) {
		if(cdTime.containsKey(ownerPlayer)) {
			int cdTimeInSec = iShop.config.getInt("noStockCooldown");
			long secondsLeft = ((cdTime.get(ownerPlayer) / 1000) + cdTimeInSec) - (System.currentTimeMillis() / 1000);
			if(ownerPlayer != null && ownerPlayer.isOnline() && secondsLeft < 0) {
				if(!itemString.equals("AIR")) {
					ownerPlayer.sendMessage(Messages.SHOP_NO_STOCK_SHELF.toString().replaceAll("%s", itemString));
					cdTime.put(ownerPlayer, System.currentTimeMillis());
				}
			}
		} else if(ownerPlayer != null && ownerPlayer.isOnline()) {
				if(!itemString.equals("AIR")) {
					ownerPlayer.sendMessage(Messages.SHOP_NO_STOCK_SHELF.toString().replaceAll("%s", itemString));
					cdTime.put(ownerPlayer, System.currentTimeMillis());
				}
			}
	}

	public Optional<RowStore> getRow(int index) {
		if(index < 0 || index > 4)
			return Optional.empty();
		return Optional.ofNullable(rows[index]);
	}

	public RowStore[] getRows() { return rows; }
	public boolean isOwner(UUID owner) { return this.owner.equals(owner); }
	public boolean isAdmin() { return this.admin; }
	public UUID getOwner() { return owner; }
	public int shopId() { return this.idTienda; }
	public Location getLocation() { return location; }
}
