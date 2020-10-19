package com.minedhype.ishop;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.bukkit.inventory.meta.ItemMeta;
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
	public static boolean shopEnabled = iShop.config.getBoolean("enableShopBlock");
	public static boolean particleEffects = iShop.config.getBoolean("showParticles");
	public static int maxDays = iShop.config.getInt("maxInactiveDays");
	private static final Plugin plugin = Bukkit.getPluginManager().getPlugin("iShop");
	private static final List<Shop> shops = new ArrayList<>();
	private static final long millisecondsPerDay = 86400000;
	private final ItemStack airItem = new ItemStack(Material.AIR);
	private final Map<Player, Long> cdTime = new HashMap<>();
	private final UUID owner;
	private final Location location;
	private final RowStore[] rows;
	private final boolean admin;
	private int idShop;

	private Shop(int idShop, UUID owner, Location loc, boolean admin) {
		this.idShop = idShop;
		this.owner = owner;
		this.location = loc;
		this.rows = new RowStore[5];
		this.admin = admin;

		if(idShop == -1) {
			final Shop shop = this;
			Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				PreparedStatement stmt = null;
				try {
					stmt = iShop.getConnection().prepareStatement("INSERT INTO ishopShops (location, owner, admin) VALUES (?,?,?);", 1);
					String locationRaw = loc.getBlockX()+";"+loc.getBlockY()+";"+loc.getBlockZ()+";"+ loc.getWorld().getName();
					stmt.setString(1, locationRaw);
					stmt.setString(2, owner.toString());
					stmt.setBoolean(3, admin);
					stmt.executeUpdate();
					ResultSet res = stmt.getGeneratedKeys();
					if(res.next())
						shop.idShop = res.getInt(1);
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
		return shops.parallelStream().filter(shop -> shop.idShop == id).findFirst();
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
						String manageMessage = ChatColor.GOLD + "Shop id " + ChatColor.GREEN + s.idShop + ChatColor.GOLD + " Location XYZ: " + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName();
						TextComponent manageMsg = new TextComponent(manageMessage);
						TextComponent manageText = new TextComponent(ChatColor.DARK_GRAY + " [" + Messages.SHOP_CLICK_MANAGE.toString() + ChatColor.DARK_GRAY + "]");
						manageText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop manage " + s.idShop));
						manageText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Messages.SHOP_CLICK_MANAGE_TEXT.toString())));
						manageMsg.addExtra(manageText);
						player.spigot().sendMessage(manageMsg);
					} else if(player.hasPermission(Permission.SHOP_ADMIN.toString())) {
						String manageMessage = ChatColor.GOLD + "Shop id " + ChatColor.GREEN + s.idShop + ChatColor.GOLD + " Location XYZ: " + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName();
						TextComponent manageMsg = new TextComponent(manageMessage);
						TextComponent manageText = new TextComponent(ChatColor.DARK_GRAY + " [" + Messages.SHOP_CLICK_MANAGE.toString() + ChatColor.DARK_GRAY + "]");
						manageText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop manage " + s.idShop));
						manageText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Messages.SHOP_CLICK_MANAGE_TEXT.toString())));
						manageMsg.addExtra(manageText);
						String shopMessage = "";
						if(!s.isOwner(player.getUniqueId())) {
							TextComponent shopMsg = new TextComponent(shopMessage);
							TextComponent shopText = new TextComponent(ChatColor.DARK_GRAY + " [" + Messages.SHOP_CLICK_SHOP.toString() + ChatColor.DARK_GRAY + "]");
							shopText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop view " + s.idShop));
							shopText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Messages.SHOP_CLICK_SHOP_TEXT.toString())));
							shopMsg.addExtra(shopText);
							player.spigot().sendMessage(manageMsg, shopMsg);
						} else
							player.spigot().sendMessage(manageMsg);
					} else if(iShop.config.getBoolean("remoteShopping") && !s.isOwner(player.getUniqueId())) {
						String shopMessage = ChatColor.GOLD + "Shop id " + ChatColor.GREEN + s.idShop + ChatColor.GOLD + " Location XYZ: " + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName();
						TextComponent shopMsg = new TextComponent(shopMessage);
						TextComponent shopText = new TextComponent(ChatColor.DARK_GRAY + " [" + Messages.SHOP_CLICK_SHOP.toString() + ChatColor.DARK_GRAY + "]");
						shopText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop view " + s.idShop));
						shopText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Messages.SHOP_CLICK_SHOP_TEXT.toString())));
						shopMsg.addExtra(shopText);
						player.spigot().sendMessage(shopMsg);
					} else
						player.sendMessage(ChatColor.GOLD + "Shop id " + ChatColor.GREEN + s.idShop + ChatColor.GOLD + " Location XYZ: " + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName());
				});
	}

	public static void getAdminShopList(Player player) {
		if(player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.SHOP_LIST_ADMINSHOPS.toString());
			AtomicInteger shopCount = new AtomicInteger(0);
			shops.parallelStream()
					.filter(s -> s.admin)
					.forEach(s -> {
						String manageMessage = ChatColor.GOLD + "Shop id " + ChatColor.GREEN + s.idShop + ChatColor.GOLD + " Location XYZ: " + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName();
						TextComponent manageMsg = new TextComponent(manageMessage);
						TextComponent manageText = new TextComponent(ChatColor.DARK_GRAY + " [" + Messages.SHOP_CLICK_MANAGE.toString() + ChatColor.DARK_GRAY + "]");
						manageText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop manage " + s.idShop));
						manageText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Messages.SHOP_CLICK_MANAGE_TEXT.toString())));
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
			loadStocks = iShop.getConnection().prepareStatement("SELECT owner, items, pag FROM ishopStock;");
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

			loadShops = iShop.getConnection().prepareStatement("SELECT location, owner, itemIn1, itemIn2, itemOut1, itemOut2, idShop, admin, broadcast FROM ishopShopsRows LEFT JOIN ishopShops ON id = idShop ORDER BY idShop;");
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
					int idShop = dataStore.getInt(7);
					boolean admin = dataStore.getBoolean(8);
					shops.add(new Shop(idShop, owner, location, admin));
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
				Map<String, Object> itemIn1Raw = configIn1.getValues(true);
				ItemStack itemIn1 = ItemStack.deserialize(itemIn1Raw);
				Map<String, Object> itemIn2Raw = configIn2.getValues(true);
				ItemStack itemIn2 = ItemStack.deserialize(itemIn2Raw);
				Map<String, Object> itemOut1Raw = configOut1.getValues(true);
				ItemStack itemOut1 = ItemStack.deserialize(itemOut1Raw);
				Map<String, Object> itemOut2Raw = configOut2.getValues(true);
				ItemStack itemOut2 = ItemStack.deserialize(itemOut2Raw);
				boolean broadcast = dataStore.getBoolean(9);
				rows[index] = new RowStore(itemOut1, itemOut2, itemIn1, itemIn2, broadcast);
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
			stmt = iShop.getConnection().prepareStatement("DELETE FROM ishopShopsRows;");
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
				row.saveData(idShop);
	}

	public void buy(Player player, int index) {
		Optional<RowStore> row = getRow(index);
		if(!row.isPresent())
			return;

		if(row.get().getItemIn1().equals(row.get().getItemIn2())) {
			if(!player.getInventory().containsAtLeast(row.get().getItemIn1(), row.get().getItemIn1().getAmount() + row.get().getItemIn2().getAmount()) && (!row.get().getItemIn1().equals(null) || !row.get().getItemIn1().equals(Material.AIR))) {
				player.sendMessage(Messages.SHOP_NO_ITEMS.toString());
				return;
			}
		}

		if(!player.getInventory().containsAtLeast(row.get().getItemIn1(), row.get().getItemIn1().getAmount()) && (!row.get().getItemIn1().equals(null) || !row.get().getItemIn1().equals(airItem))) {
			player.sendMessage(Messages.SHOP_NO_ITEMS.toString());
			return;
		}

		if(!this.admin) {
			if (!player.getInventory().containsAtLeast(row.get().getItemIn2(), row.get().getItemIn2().getAmount()) && (!row.get().getItemIn1().equals(null) || !row.get().getItemIn1().equals(airItem))) {
				player.sendMessage(Messages.SHOP_NO_ITEMS.toString());
				return;
			}
		}

		if(row.get().getItemOut1().equals(row.get().getItemOut2()) && !row.get().getItemOut1().equals(airItem)) {
			if(!Utils.hasDoubleStock(this, row.get().getItemOut1(), row.get().getItemOut1().getAmount() + row.get().getItemOut2().getAmount())) {
				player.sendMessage(Messages.SHOP_NO_STOCK.toString());

				Player ownerPlayer = Bukkit.getPlayer(owner);
				if(cdTime.containsKey(ownerPlayer)) {
					int cdTimeInSec = iShop.config.getInt("noStockCooldown");
					long secondsLeft = ((cdTime.get(ownerPlayer)/1000)+cdTimeInSec) - (System.currentTimeMillis()/1000);
					if(ownerPlayer != null && ownerPlayer.isOnline() && secondsLeft < 0) {
						if(row.get().getItemOut1().equals(row.get().getItemOut2())) {
								ownerPlayer.sendMessage(Messages.SHOP_NO_STOCK_SHELF.toString().replaceAll("%s", row.get().getItemOut1().getType().toString()));
						} else {
								ownerPlayer.sendMessage(Messages.SHOP_NO_STOCK_SHELF.toString().replaceAll("%s", row.get().getItemOut1().getType().toString()));
								ownerPlayer.sendMessage(Messages.SHOP_NO_STOCK_SHELF.toString().replaceAll("%s", row.get().getItemOut2().getType().toString()));
						}
						cdTime.put(ownerPlayer, System.currentTimeMillis());
					}
				} else {
					if(ownerPlayer != null && ownerPlayer.isOnline()) {
						if(row.get().getItemOut1().equals(row.get().getItemOut2())) {
							if(!row.get().getItemOut1().equals(airItem))
								ownerPlayer.sendMessage(Messages.SHOP_NO_STOCK_SHELF.toString().replaceAll("%s", row.get().getItemOut1().getType().toString()));
						} else {
								ownerPlayer.sendMessage(Messages.SHOP_NO_STOCK_SHELF.toString().replaceAll("%s", row.get().getItemOut1().getType().toString()));
								ownerPlayer.sendMessage(Messages.SHOP_NO_STOCK_SHELF.toString().replaceAll("%s", row.get().getItemOut2().getType().toString()));
						}
						cdTime.put(ownerPlayer, System.currentTimeMillis());
					}
				}

				return;
			}
		}

		if(!Utils.hasStock(this, row.get().getItemOut1()) && !row.get().getItemOut1().equals(airItem)) {
			player.sendMessage(Messages.SHOP_NO_STOCK.toString());

			Player ownerPlayer = Bukkit.getPlayer(owner);
			if(cdTime.containsKey(ownerPlayer)) {
				int cdTimeInSec = iShop.config.getInt("noStockCooldown");
				long secondsLeft = ((cdTime.get(ownerPlayer)/1000)+cdTimeInSec) - (System.currentTimeMillis()/1000);
				if(ownerPlayer != null && ownerPlayer.isOnline() && secondsLeft < 0) {
					ownerPlayer.sendMessage(Messages.SHOP_NO_STOCK_SHELF.toString().replaceAll("%s", row.get().getItemOut1().getType().toString()));
					cdTime.put(ownerPlayer, System.currentTimeMillis());
				}
			} else {
				cdTime.put(ownerPlayer, System.currentTimeMillis());
				if(ownerPlayer != null && ownerPlayer.isOnline()) {
					ownerPlayer.sendMessage(Messages.SHOP_NO_STOCK_SHELF.toString().replaceAll("%s", row.get().getItemOut1().getType().toString()));
				}
			}

			return;
		}

		if(!Utils.hasStock(this, row.get().getItemOut2()) && !row.get().getItemOut2().equals(airItem)) {
			player.sendMessage(Messages.SHOP_NO_STOCK.toString());

			Player ownerPlayer = Bukkit.getPlayer(owner);
			if(cdTime.containsKey(ownerPlayer)) {
				int cdTimeInSec = iShop.config.getInt("noStockCooldown");
				long secondsLeft = ((cdTime.get(ownerPlayer)/1000)+cdTimeInSec) - (System.currentTimeMillis()/1000);
				if(ownerPlayer != null && ownerPlayer.isOnline() && secondsLeft < 0) {
					ownerPlayer.sendMessage(Messages.SHOP_NO_STOCK_SHELF.toString().replaceAll("%s", row.get().getItemOut2().getType().toString()));
					cdTime.put(ownerPlayer, System.currentTimeMillis());
				}
			} else {
				cdTime.put(ownerPlayer, System.currentTimeMillis());
				if(ownerPlayer != null && ownerPlayer.isOnline()) {
					ownerPlayer.sendMessage(Messages.SHOP_NO_STOCK_SHELF.toString().replaceAll("%s", row.get().getItemOut2().getType().toString()));
				}
			}

			return;
		}

		if(player.getInventory().firstEmpty() != -1) {
			if(!row.get().getItemIn1().equals(airItem))
				player.getInventory().removeItem(row.get().getItemIn1().clone());
			if(!row.get().getItemIn2().equals(airItem))
				player.getInventory().removeItem(row.get().getItemIn2().clone());
			if(!row.get().getItemOut1().equals(airItem))
				player.getInventory().addItem(row.get().getItemOut1().clone());
			if(!row.get().getItemOut2().equals(airItem))
				player.getInventory().addItem(row.get().getItemOut2().clone());
		} else {
			player.sendMessage(Messages.PLAYER_INV_FULL.toString());
			return;
		}

		String nameIn1, nameIn2, nameOut1, nameOut2;
		int inA1 = 0;
		int inA2 = 0;
		int outA1 = 0;
		int outA2 = 0;

		try {
			nameIn1 = row.get().getItemIn1().getItemMeta().hasDisplayName() ? row.get().getItemIn1().getItemMeta().getDisplayName() : row.get().getItemIn1().getType().name().replaceAll("_", " ").toLowerCase();
			inA1 = row.get().getItemIn1().getAmount();
		} catch(Exception e) { nameIn1 = "empty"; inA1 = 0; }
		try {
			nameIn2 = row.get().getItemIn2().getItemMeta().hasDisplayName() ? row.get().getItemIn2().getItemMeta().getDisplayName() : row.get().getItemIn2().getType().name().replaceAll("_", " ").toLowerCase();
			inA2 = row.get().getItemIn2().getAmount();
		} catch(Exception e) { nameIn2 = "empty"; inA2 = 0; }
		try {
			nameOut1 = row.get().getItemOut1().getItemMeta().hasDisplayName() ? row.get().getItemOut1().getItemMeta().getDisplayName() : row.get().getItemOut1().getType().name().replaceAll("_", " ").toLowerCase();
			outA1 = row.get().getItemOut1().getAmount();
		} catch(Exception e) { nameOut1 = "empty"; outA1 = 0; }
		try {
			nameOut2 = row.get().getItemOut2().getItemMeta().hasDisplayName() ? row.get().getItemOut2().getItemMeta().getDisplayName() : row.get().getItemOut2().getType().name().replaceAll("_", " ").toLowerCase();
			outA2 = row.get().getItemOut2().getAmount();
		} catch(Exception e) { nameOut2 = "empty"; outA2 = 0; }

		String i1 = nameIn1 + " x " + inA1;
		String i2 = nameIn2 + " x " + inA2;
		String o1 = nameOut1 + " x " + outA1;
		String o2 = nameOut2 + " x " + outA2;

		if(!this.admin && !row.get().broadcast) {
			if(inA1 == 0 && outA1 == 0) {
				player.sendMessage(Messages.SHOP_PURCHASE.toString()
						.replaceAll("%in", o2)
						.replaceAll("%out", i2));
			} else if(inA1 == 0 && outA2 == 0) {
				player.sendMessage(Messages.SHOP_PURCHASE.toString()
						.replaceAll("%in", o1)
						.replaceAll("%out", i2));
			} else if(inA2 == 0 && outA1 == 0) {
				player.sendMessage(Messages.SHOP_PURCHASE.toString()
						.replaceAll("%in",o2)
						.replaceAll("%out", i1));
			} else if(inA2 == 0 && outA2 == 0) {
				player.sendMessage(Messages.SHOP_PURCHASE.toString()
						.replaceAll("%in", o1)
						.replaceAll("%out", i1));
			}  else if(inA1 == 0) {
				player.sendMessage(Messages.SHOP_PURCHASE.toString()
						.replaceAll("%in", o1 + " AND " + o2)
						.replaceAll("%out", i2));
			}  else if(inA2 == 0) {
				player.sendMessage(Messages.SHOP_PURCHASE.toString()
						.replaceAll("%in", o1 + " AND " + o2)
						.replaceAll("%out", i1));
			} else if(outA1 == 0) {
				player.sendMessage(Messages.SHOP_PURCHASE.toString()
						.replaceAll("%in", o2)
						.replaceAll("%out", i1 + " AND " + i2));
			} else if(outA2 == 0) {
				player.sendMessage(Messages.SHOP_PURCHASE.toString()
						.replaceAll("%in", o1)
						.replaceAll("%out", i1 + " AND " + i2));
			} else {
				player.sendMessage(Messages.SHOP_PURCHASE.toString()
						.replaceAll("%in", o1 + " AND " + o2)
						.replaceAll("%out", i1 + " AND " + i2));
			}
		}

		if(!this.admin) {
			try {
				if(outA1 > 0 || row.get().getItemOut1() != null || !row.get().getItemOut1().getType().isAir())
					this.takeItem(row.get().getItemOut1().clone());
			} catch(Exception ignored) { }
			try {
			if(outA2 > 0 || row.get().getItemOut2() != null || !row.get().getItemOut2().getType().isAir())
				this.takeItem(row.get().getItemOut2().clone());
			} catch(Exception ignored) { }
			try {
			if(inA1 > 0 || row.get().getItemIn1() != null || !row.get().getItemIn1().getType().isAir())
				this.giveItem(row.get().getItemIn1().clone());
			} catch(Exception ignored) { }
			try {
			if(inA2 > 0 || row.get().getItemIn2() != null || !row.get().getItemIn2().getType().isAir())
				this.giveItem(row.get().getItemIn2().clone());
			} catch(Exception ignored) { }

			Player ownerPlayer = Bukkit.getPlayer(owner);
			if(ownerPlayer != null && ownerPlayer.isOnline()) {
				if(inA1 == 0 && outA1 == 0) {
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString()
							.replaceAll("%in", o2)
							.replaceAll("%out", i2)
							.replaceAll("%p", player.getName()));
				} else if(inA1 == 0 && outA2 == 0) {
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString()
							.replaceAll("%in", o1)
							.replaceAll("%out", i2)
							.replaceAll("%p", player.getName()));
				} else if(inA2 == 0 && outA1 == 0) {
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString()
							.replaceAll("%in",o2)
							.replaceAll("%out", i1)
							.replaceAll("%p", player.getName()));
				} else if(inA2 == 0 && outA2 == 0) {
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString()
							.replaceAll("%in", o1)
							.replaceAll("%out", i1)
							.replaceAll("%p", player.getName()));
				}  else if(inA1 == 0) {
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString()
							.replaceAll("%in", o1 + " AND " + o2)
							.replaceAll("%out", i2)
							.replaceAll("%p", player.getName()));
				}  else if(inA2 == 0) {
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString()
							.replaceAll("%in", o1 + " AND " + o2)
							.replaceAll("%out", i1)
							.replaceAll("%p", player.getName()));
				} else if(outA1 == 0) {
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString()
							.replaceAll("%in", o2)
							.replaceAll("%out", i1 + " AND " + i2)
							.replaceAll("%p", player.getName()));
				} else if(outA2 == 0) {
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString()
							.replaceAll("%in", o1)
							.replaceAll("%out", i1 + " AND " + i2)
							.replaceAll("%p", player.getName()));
				} else {
					ownerPlayer.sendMessage(Messages.SHOP_SELL.toString()
							.replaceAll("%in", o1 + " AND " + o2)
							.replaceAll("%out", i1 + " AND " + i2)
							.replaceAll("%p", player.getName()));
				}
			}
		} else if(row.get().broadcast) {
			if(inA1 == 0 && outA1 == 0) {
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString()
						.replaceAll("%in", o2)
						.replaceAll("%out", i2)
						.replaceAll("%p", player.getName()));
			} else if(inA1 == 0 && outA2 == 0) {
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString()
						.replaceAll("%in", o1)
						.replaceAll("%out", i2)
						.replaceAll("%p", player.getName()));
			} else if(inA2 == 0 && outA1 == 0) {
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString()
						.replaceAll("%in",o2)
						.replaceAll("%out", i1)
						.replaceAll("%p", player.getName()));
			} else if(inA2 == 0 && outA2 == 0) {
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString()
						.replaceAll("%in", o1)
						.replaceAll("%out", i1)
						.replaceAll("%p", player.getName()));
			}  else if(inA1 == 0) {
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString()
						.replaceAll("%in", o1 + " AND " + o2)
						.replaceAll("%out", i2)
						.replaceAll("%p", player.getName()));
			}  else if(inA2 == 0) {
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString()
						.replaceAll("%in", o1 + " AND " + o2)
						.replaceAll("%out", i1)
						.replaceAll("%p", player.getName()));
			} else if(outA1 == 0) {
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString()
						.replaceAll("%in", o2)
						.replaceAll("%out", i1 + " AND " + i2)
						.replaceAll("%p", player.getName()));
			} else if(outA2 == 0) {
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString()
						.replaceAll("%in", o1)
						.replaceAll("%out", i1 + " AND " + i2)
						.replaceAll("%p", player.getName()));
			} else {
				Bukkit.broadcastMessage(Messages.SHOP_SELL.toString()
						.replaceAll("%in", o1 + " AND " + o2)
						.replaceAll("%out", i1 + " AND " + i2)
						.replaceAll("%p", player.getName()));
			}
		}
	}

	public boolean hasExpired() {
		if(this.admin)
			return false;

		if(maxDays <= 0)
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
				stmt1 = iShop.getConnection().prepareStatement("DELETE FROM ishopShopsRows WHERE idShop = ?;");
				stmt1.setInt(1, idShop);
				stmt1.execute();

				stmt2 = iShop.getConnection().prepareStatement("DELETE FROM ishopShops WHERE id = ?;");
				stmt2.setInt(1, idShop);
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
		if(index < 0 || index > 4)
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
