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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.Plugin;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.minedhype.ishop.inventories.InvStock;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;

public class Shop {
	public static boolean deletePlayerShop = iShop.config.getBoolean("deleteBlock");
	public static boolean particleEffects = iShop.config.getBoolean("showParticles");
	public static boolean shopOutStock = iShop.config.getBoolean("enableOutOfStockMessages");
	public static boolean shopEnabled = iShop.config.getBoolean("enableShopBlock");
	public static boolean showOwnedShops = iShop.config.getBoolean("publicShopListShowsOwned");
	public static boolean shopNotifications = iShop.config.getBoolean("enableShopNotifications");
	public static boolean stockMessages = iShop.config.getBoolean("enableShopSoldMessage");
	public static boolean stockMessagesSaveAll = iShop.config.getBoolean("enableSavingAllShopSoldMessages");
	public static List<String> exemptExpiringList = iShop.config.getStringList("exemptExpiringShops");
	public static int maxDays = iShop.config.getInt("maxInactiveDays");
	public static final ConcurrentHashMap<Integer, UUID> shopList = new ConcurrentHashMap<>();
	public static final ConcurrentHashMap<UUID, ArrayList<String>> shopMessages = new ConcurrentHashMap<>();
	private static boolean exemptListInactive;
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
	public static boolean checkShopDistanceFromStockBlock(Location stockLocation, UUID shopOwner) { return shops.parallelStream().filter(s -> !s.admin && s.isOwner(shopOwner)).anyMatch(s -> s.getLocation().getWorld().equals(stockLocation.getWorld()) && s.location.distanceSquared(stockLocation) <= EventShop.stockRangeLimit*EventShop.stockRangeLimit); }
	public static int getNumShops(UUID owner) { return (int) shops.parallelStream().filter(t -> !t.admin && t.owner.equals(owner)).count(); }

	public static void getPlayersShopList() {
		if(iShop.config.getBoolean("adminShopPublic"))
			shops.parallelStream().forEach(s -> shopList.putIfAbsent(s.idTienda, s.owner));
		else
			shops.parallelStream().filter(s -> !s.admin).forEach(s -> shopList.putIfAbsent(s.idTienda, s.owner));
	}

	public static void getShopList(Player player, UUID sOwner, String pOwner) {
		player.sendMessage(Messages.SHOP_FOUND.toString().replaceAll("%p", pOwner).replaceAll("%shops", String.valueOf(getNumShops(sOwner))));
		shops.parallelStream().filter(s -> !s.admin && s.isOwner(sOwner)).forEach(s -> {
			if(s.isOwner(player.getUniqueId()) && InvAdminShop.remoteManage) {
				String manageMessage = Messages.SHOP_LOCATION.toString().replaceAll("%id", String.valueOf(s.idTienda)) + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName();
				TextComponent manageMsg = new TextComponent(manageMessage);
				TextComponent manageText = new TextComponent(ChatColor.DARK_GRAY + " [" + Messages.SHOP_CLICK_MANAGE + ChatColor.DARK_GRAY + "]");
				manageText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop manage " + s.idTienda));
				manageMsg.addExtra(manageText);
				player.spigot().sendMessage(manageMsg);
			} else if(player.hasPermission(Permission.SHOP_ADMIN.toString())) {
				String manageMessage = Messages.SHOP_LOCATION.toString().replaceAll("%id", String.valueOf(s.idTienda)) + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName();
				TextComponent manageMsg = new TextComponent(manageMessage);
				TextComponent manageText = new TextComponent(ChatColor.DARK_GRAY + " [" + Messages.SHOP_CLICK_MANAGE + ChatColor.DARK_GRAY + "]");
				manageText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop manage " + s.idTienda));
				manageMsg.addExtra(manageText);
				if(!s.isOwner(player.getUniqueId())) {
					String shopMessage = "";
					TextComponent shopMsg = new TextComponent(shopMessage);
					TextComponent shopText = new TextComponent(ChatColor.DARK_GRAY + " [" + Messages.SHOP_CLICK_SHOP + ChatColor.DARK_GRAY + "]");
					shopText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop view " + s.idTienda));
					shopMsg.addExtra(shopText);
					player.spigot().sendMessage(manageMsg, shopMsg);
				} else
					player.spigot().sendMessage(manageMsg);
			} else if(iShop.config.getBoolean("remoteShopping") && !s.isOwner(player.getUniqueId())) {
				String shopMessage = Messages.SHOP_LOCATION.toString().replaceAll("%id", String.valueOf(s.idTienda)) + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName();
				TextComponent shopMsg = new TextComponent(shopMessage);
				TextComponent shopText = new TextComponent(ChatColor.DARK_GRAY + " [" + Messages.SHOP_CLICK_SHOP + ChatColor.DARK_GRAY + "]");
				shopText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop view " + s.idTienda));
				shopMsg.addExtra(shopText);
				player.spigot().sendMessage(shopMsg);
			} else
				player.sendMessage(Messages.SHOP_LOCATION.toString().replaceAll("%id", String.valueOf(s.idTienda)) + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName());
		});
	}

	public static void getAdminShopList(Player player) {
		player.sendMessage(Messages.SHOP_LIST_ADMINSHOPS.toString());
		AtomicInteger shopCount = new AtomicInteger(0);
		shops.parallelStream().filter(s -> s.admin).forEach(s -> {
			String manageMessage = Messages.SHOP_LOCATION.toString().replaceAll("%id", String.valueOf(s.idTienda)) + ChatColor.GREEN + s.location.getBlockX() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockY() + ChatColor.GOLD + " / " + ChatColor.GREEN + s.location.getBlockZ() + ChatColor.GOLD + " in " + ChatColor.GREEN + s.location.getWorld().getName();
			TextComponent manageMsg = new TextComponent(manageMessage);
			TextComponent manageText = new TextComponent(ChatColor.DARK_GRAY + " [" + Messages.SHOP_CLICK_MANAGE + ChatColor.DARK_GRAY + "]");
			manageText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/shop manage " + s.idTienda));
			manageMsg.addExtra(manageText);
			player.spigot().sendMessage(manageMsg);
			shopCount.getAndIncrement();
		});
		if(shopCount.get() == 0)
			player.sendMessage(Messages.SHOP_NO_ADMINSHOPS_FOUND.toString());
	}

	public static void getOutOfStock(Player player, UUID sOwner, String pOwner) {
		AtomicInteger outCount = new AtomicInteger(0);
		shops.parallelStream().filter(s -> !s.admin && s.isOwner(sOwner)).forEach(s -> {
			Optional<RowStore> row0 = s.getRow(0);
			if(row0.isPresent())
				if(row0.get().getItemOut().isSimilar(row0.get().getItemOut2()) && !Utils.hasDoubleItemStock(s, row0.get().getItemOut(), row0.get().getItemOut2()) || !Utils.hasStock(s, row0.get().getItemOut()) || !Utils.hasStock(s, row0.get().getItemOut2())) {
					String row0out = "";
					String row0out2 = "";
					boolean out0Changed = false;
					boolean out0Changed2 = false;
					if(row0.get().getItemOut().getType().equals(Material.ENCHANTED_BOOK) && row0.get().getItemOut().getItemMeta() instanceof EnchantmentStorageMeta) {
						EnchantmentStorageMeta metaOut = (EnchantmentStorageMeta) row0.get().getItemOut().getItemMeta();
						StringBuilder sb = new StringBuilder();
						sb.append("enchanted book: ");
						for(Map.Entry<Enchantment, Integer> entry : metaOut.getStoredEnchants().entrySet()) {
							String enchants = entry.getKey().getKey().toString().replaceAll("_", " ").replaceAll("minecraft:", "");
							sb.append(enchants).append(" ").append(entry.getValue()).append(", ");
						}
						sb.deleteCharAt(sb.length()-1).deleteCharAt(sb.length()-1);
						row0out = sb.toString().toUpperCase();
						out0Changed = true;
					}
					if(row0.get().getItemOut2().getType().equals(Material.ENCHANTED_BOOK) && row0.get().getItemOut2().getItemMeta() instanceof EnchantmentStorageMeta) {
						EnchantmentStorageMeta metaOut = (EnchantmentStorageMeta) row0.get().getItemOut2().getItemMeta();
						StringBuilder sb = new StringBuilder();
						sb.append("enchanted book: ");
						for(Map.Entry<Enchantment, Integer> entry : metaOut.getStoredEnchants().entrySet()) {
							String enchants = entry.getKey().getKey().toString().replaceAll("_", " ").replaceAll("minecraft:", "");
							sb.append(enchants).append(" ").append(entry.getValue()).append(", ");
						}
						sb.deleteCharAt(sb.length()-1).deleteCharAt(sb.length()-1);
						row0out2 = sb.toString().toUpperCase();
						out0Changed2 = true;
					}
					if(!out0Changed)
						row0out = row0.get().getItemOut().getType().toString();
					if(!out0Changed2)
						row0out2 = row0.get().getItemOut2().getType().toString();
					if(row0.get().getItemOut2() == null || row0.get().getItemOut2().getType().equals(Material.AIR))
						player.sendMessage(Messages.SHOP_OUT.toString().replaceAll("%row", String.valueOf(1)).replaceAll("%shop", String.valueOf(s.idTienda)) + ChatColor.RED + " (" + row0out + ")");
					else if(row0.get().getItemOut() == null || row0.get().getItemOut().getType().equals(Material.AIR))
						player.sendMessage(Messages.SHOP_OUT.toString().replaceAll("%row", String.valueOf(1)).replaceAll("%shop", String.valueOf(s.idTienda)) + ChatColor.RED + " (" + row0out2 + ")");
					else
						player.sendMessage(Messages.SHOP_OUT.toString().replaceAll("%row", String.valueOf(1)).replaceAll("%shop", String.valueOf(s.idTienda)) + ChatColor.RED + " (" + row0out + " & " + row0out2 + ")");
					outCount.getAndIncrement();
				}
			Optional<RowStore> row1 = s.getRow(1);
			if(row1.isPresent())
				if(row1.get().getItemOut().isSimilar(row1.get().getItemOut2()) && !Utils.hasDoubleItemStock(s, row1.get().getItemOut(), row1.get().getItemOut2()) || !Utils.hasStock(s, row1.get().getItemOut()) || !Utils.hasStock(s, row1.get().getItemOut2())) {
					String row1out = "";
					String row1out2 = "";
					boolean out1Changed = false;
					boolean out1Changed2 = false;
					if(row1.get().getItemOut().getType().equals(Material.ENCHANTED_BOOK) && row1.get().getItemOut().getItemMeta() instanceof EnchantmentStorageMeta) {
						EnchantmentStorageMeta metaOut = (EnchantmentStorageMeta) row1.get().getItemOut().getItemMeta();
						StringBuilder sb = new StringBuilder();
						sb.append("enchanted book: ");
						for(Map.Entry<Enchantment, Integer> entry : metaOut.getStoredEnchants().entrySet()) {
							String enchants = entry.getKey().getKey().toString().replaceAll("_", " ").replaceAll("minecraft:", "");
							sb.append(enchants).append(" ").append(entry.getValue()).append(", ");
						}
						sb.deleteCharAt(sb.length()-1).deleteCharAt(sb.length()-1);
						row1out = sb.toString().toUpperCase();
						out1Changed = true;
					}
					if(row1.get().getItemOut2().getType().equals(Material.ENCHANTED_BOOK) && row1.get().getItemOut2().getItemMeta() instanceof EnchantmentStorageMeta) {
						EnchantmentStorageMeta metaOut = (EnchantmentStorageMeta) row1.get().getItemOut2().getItemMeta();
						StringBuilder sb = new StringBuilder();
						sb.append("enchanted book: ");
						for(Map.Entry<Enchantment, Integer> entry : metaOut.getStoredEnchants().entrySet()) {
							String enchants = entry.getKey().getKey().toString().replaceAll("_", " ").replaceAll("minecraft:", "");
							sb.append(enchants).append(" ").append(entry.getValue()).append(", ");
						}
						sb.deleteCharAt(sb.length()-1).deleteCharAt(sb.length()-1);
						row1out2 = sb.toString().toUpperCase();
						out1Changed2 = true;
					}
					if(!out1Changed)
						row1out = row1.get().getItemOut().getType().toString();
					if(!out1Changed2)
						row1out2 = row1.get().getItemOut2().getType().toString();
					if(row1.get().getItemOut2() == null || row1.get().getItemOut2().getType().equals(Material.AIR))
						player.sendMessage(Messages.SHOP_OUT.toString().replaceAll("%row", String.valueOf(2)).replaceAll("%shop", String.valueOf(s.idTienda)) + ChatColor.RED + " (" + row1out + ")");
					else if(row1.get().getItemOut() == null || row1.get().getItemOut().getType().equals(Material.AIR))
						player.sendMessage(Messages.SHOP_OUT.toString().replaceAll("%row", String.valueOf(2)).replaceAll("%shop", String.valueOf(s.idTienda)) + ChatColor.RED + " (" + row1out2 + ")");
					else
						player.sendMessage(Messages.SHOP_OUT.toString().replaceAll("%row", String.valueOf(2)).replaceAll("%shop", String.valueOf(s.idTienda)) + ChatColor.RED + " (" + row1out + " & " + row1out2 + ")");
					outCount.getAndIncrement();
				}
			Optional<RowStore> row2 = s.getRow(2);
			if(row2.isPresent())
				if(row2.get().getItemOut().isSimilar(row2.get().getItemOut2()) && !Utils.hasDoubleItemStock(s, row2.get().getItemOut(), row2.get().getItemOut2()) || !Utils.hasStock(s, row2.get().getItemOut()) || !Utils.hasStock(s, row2.get().getItemOut2())) {
					String row2out = "";
					String row2out2 = "";
					boolean out2Changed = false;
					boolean out2Changed2 = false;
					if(row2.get().getItemOut().getType().equals(Material.ENCHANTED_BOOK) && row2.get().getItemOut().getItemMeta() instanceof EnchantmentStorageMeta) {
						EnchantmentStorageMeta metaOut = (EnchantmentStorageMeta) row2.get().getItemOut().getItemMeta();
						StringBuilder sb = new StringBuilder();
						sb.append("enchanted book: ");
						for(Map.Entry<Enchantment, Integer> entry : metaOut.getStoredEnchants().entrySet()) {
							String enchants = entry.getKey().getKey().toString().replaceAll("_", " ").replaceAll("minecraft:", "");
							sb.append(enchants).append(" ").append(entry.getValue()).append(", ");
						}
						sb.deleteCharAt(sb.length()-1).deleteCharAt(sb.length()-1);
						row2out = sb.toString().toUpperCase();
						out2Changed = true;
					}
					if(row2.get().getItemOut2().getType().equals(Material.ENCHANTED_BOOK) && row2.get().getItemOut2().getItemMeta() instanceof EnchantmentStorageMeta) {
						EnchantmentStorageMeta metaOut = (EnchantmentStorageMeta) row2.get().getItemOut2().getItemMeta();
						StringBuilder sb = new StringBuilder();
						sb.append("enchanted book: ");
						for(Map.Entry<Enchantment, Integer> entry : metaOut.getStoredEnchants().entrySet()) {
							String enchants = entry.getKey().getKey().toString().replaceAll("_", " ").replaceAll("minecraft:", "");
							sb.append(enchants).append(" ").append(entry.getValue()).append(", ");
						}
						sb.deleteCharAt(sb.length()-1).deleteCharAt(sb.length()-1);
						row2out2 = sb.toString().toUpperCase();
						out2Changed2 = true;
					}
					if(!out2Changed)
						row2out = row2.get().getItemOut().getType().toString();
					if(!out2Changed2)
						row2out2 = row2.get().getItemOut2().getType().toString();
					if(row2.get().getItemOut2() == null || row2.get().getItemOut2().getType().equals(Material.AIR))
						player.sendMessage(Messages.SHOP_OUT.toString().replaceAll("%row", String.valueOf(3)).replaceAll("%shop", String.valueOf(s.idTienda)) + ChatColor.RED + " (" + row2out + ")");
					else if(row2.get().getItemOut() == null || row2.get().getItemOut().getType().equals(Material.AIR))
						player.sendMessage(Messages.SHOP_OUT.toString().replaceAll("%row", String.valueOf(3)).replaceAll("%shop", String.valueOf(s.idTienda)) + ChatColor.RED + " (" + row2out2 + ")");
					else
						player.sendMessage(Messages.SHOP_OUT.toString().replaceAll("%row", String.valueOf(3)).replaceAll("%shop", String.valueOf(s.idTienda)) + ChatColor.RED + " (" + row2out + " & " + row2out2 + ")");
					outCount.getAndIncrement();
				}
			Optional<RowStore> row3 = s.getRow(3);
			if(row3.isPresent())
				if(row3.get().getItemOut().isSimilar(row3.get().getItemOut2()) && !Utils.hasDoubleItemStock(s, row3.get().getItemOut(), row3.get().getItemOut2()) || !Utils.hasStock(s, row3.get().getItemOut()) || !Utils.hasStock(s, row3.get().getItemOut2())) {
					String row3out = "";
					String row3out2 = "";
					boolean out3Changed = false;
					boolean out3Changed2 = false;
					if(row3.get().getItemOut().getType().equals(Material.ENCHANTED_BOOK) && row3.get().getItemOut().getItemMeta() instanceof EnchantmentStorageMeta) {
						EnchantmentStorageMeta metaOut = (EnchantmentStorageMeta) row3.get().getItemOut().getItemMeta();
						StringBuilder sb = new StringBuilder();
						sb.append("enchanted book: ");
						for(Map.Entry<Enchantment, Integer> entry : metaOut.getStoredEnchants().entrySet()) {
							String enchants = entry.getKey().getKey().toString().replaceAll("_", " ").replaceAll("minecraft:", "");
							sb.append(enchants).append(" ").append(entry.getValue()).append(", ");
						}
						sb.deleteCharAt(sb.length()-1).deleteCharAt(sb.length()-1);
						row3out = sb.toString().toUpperCase();
						out3Changed = true;
					}
					if(row3.get().getItemOut2().getType().equals(Material.ENCHANTED_BOOK) && row3.get().getItemOut2().getItemMeta() instanceof EnchantmentStorageMeta) {
						EnchantmentStorageMeta metaOut = (EnchantmentStorageMeta) row3.get().getItemOut2().getItemMeta();
						StringBuilder sb = new StringBuilder();
						sb.append("enchanted book: ");
						for(Map.Entry<Enchantment, Integer> entry : metaOut.getStoredEnchants().entrySet()) {
							String enchants = entry.getKey().getKey().toString().replaceAll("_", " ").replaceAll("minecraft:", "");
							sb.append(enchants).append(" ").append(entry.getValue()).append(", ");
						}
						sb.deleteCharAt(sb.length()-1).deleteCharAt(sb.length()-1);
						row3out2 = sb.toString().toUpperCase();
						out3Changed2 = true;
					}
					if(!out3Changed)
						row3out = row3.get().getItemOut().getType().toString();
					if(!out3Changed2)
						row3out2 = row3.get().getItemOut2().getType().toString();
					if(row3.get().getItemOut2() == null || row3.get().getItemOut2().getType().equals(Material.AIR))
						player.sendMessage(Messages.SHOP_OUT.toString().replaceAll("%row", String.valueOf(4)).replaceAll("%shop", String.valueOf(s.idTienda)) + ChatColor.RED + " (" + row3out + ")");
					else if(row3.get().getItemOut() == null || row3.get().getItemOut().getType().equals(Material.AIR))
						player.sendMessage(Messages.SHOP_OUT.toString().replaceAll("%row", String.valueOf(4)).replaceAll("%shop", String.valueOf(s.idTienda)) + ChatColor.RED + " (" + row3out2 + ")");
					else
						player.sendMessage(Messages.SHOP_OUT.toString().replaceAll("%row", String.valueOf(4)).replaceAll("%shop", String.valueOf(s.idTienda)) + ChatColor.RED + " (" + row3out + " & " + row3out2 + ")");
					outCount.getAndIncrement();
				}
			Optional<RowStore> row4 = s.getRow(4);
			if(row4.isPresent())
				if(row4.get().getItemOut().isSimilar(row4.get().getItemOut2()) && !Utils.hasDoubleItemStock(s, row4.get().getItemOut(), row4.get().getItemOut2()) || !Utils.hasStock(s, row4.get().getItemOut()) || !Utils.hasStock(s, row4.get().getItemOut2())) {
					String row4out = "";
					String row4out2 = "";
					boolean out4Changed = false;
					boolean out4Changed2 = false;
					if(row4.get().getItemOut().getType().equals(Material.ENCHANTED_BOOK) && row4.get().getItemOut().getItemMeta() instanceof EnchantmentStorageMeta) {
						EnchantmentStorageMeta metaOut = (EnchantmentStorageMeta) row4.get().getItemOut().getItemMeta();
						StringBuilder sb = new StringBuilder();
						sb.append("enchanted book: ");
						for(Map.Entry<Enchantment, Integer> entry : metaOut.getStoredEnchants().entrySet()) {
							String enchants = entry.getKey().getKey().toString().replaceAll("_", " ").replaceAll("minecraft:", "");
							sb.append(enchants).append(" ").append(entry.getValue()).append(", ");
						}
						sb.deleteCharAt(sb.length()-1).deleteCharAt(sb.length()-1);
						row4out = sb.toString().toUpperCase();
						out4Changed = true;
					}
					if(row4.get().getItemOut2().getType().equals(Material.ENCHANTED_BOOK) && row4.get().getItemOut2().getItemMeta() instanceof EnchantmentStorageMeta) {
						EnchantmentStorageMeta metaOut = (EnchantmentStorageMeta) row4.get().getItemOut2().getItemMeta();
						StringBuilder sb = new StringBuilder();
						sb.append("enchanted book: ");
						for(Map.Entry<Enchantment, Integer> entry : metaOut.getStoredEnchants().entrySet()) {
							String enchants = entry.getKey().getKey().toString().replaceAll("_", " ").replaceAll("minecraft:", "");
							sb.append(enchants).append(" ").append(entry.getValue()).append(", ");
						}
						sb.deleteCharAt(sb.length()-1).deleteCharAt(sb.length()-1);
						row4out2 = sb.toString().toUpperCase();
						out4Changed2 = true;
					}
					if(!out4Changed)
						row4out = row4.get().getItemOut().getType().toString();
					if(!out4Changed2)
						row4out2 = row4.get().getItemOut2().getType().toString();
					if(row4.get().getItemOut2() == null || row4.get().getItemOut2().getType().equals(Material.AIR))
						player.sendMessage(Messages.SHOP_OUT.toString().replaceAll("%row", String.valueOf(5)).replaceAll("%shop", String.valueOf(s.idTienda)) + ChatColor.RED + " (" + row4out + ")");
					else if(row4.get().getItemOut() == null || row4.get().getItemOut().getType().equals(Material.AIR))
						player.sendMessage(Messages.SHOP_OUT.toString().replaceAll("%row", String.valueOf(5)).replaceAll("%shop", String.valueOf(s.idTienda)) + ChatColor.RED + " (" + row4out2 + ")");
					else
						player.sendMessage(Messages.SHOP_OUT.toString().replaceAll("%row", String.valueOf(5)).replaceAll("%shop", String.valueOf(s.idTienda)) + ChatColor.RED + " (" + row4out + " & " + row4out2 + ")");
					outCount.getAndIncrement();
				}
		});
		if(outCount.get() == 0)
			player.sendMessage(Messages.SHOP_NOT_OUT.toString());
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
		exemptListInactive = exemptExpiringList.size() == 1 && exemptExpiringList.get(0).equals("00000000-0000-0000-0000-000000000000");
		List<Shop> shopDelete = new ArrayList<>();
		for(Shop shop : shops)
			if(shop.hasExpired() || shop.location.getWorld() == null)
				shopDelete.add(shop);

		for(Shop shop : shopDelete) {
			shopList.remove(shop.shopId());
			shop.deleteShop();
		}
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
		if(row.get().getItemOut().isSimilar(row.get().getItemOut2()) && !Utils.hasDoubleItemStock(this, row.get().getItemOut(), row.get().getItemOut2())) {
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
		if(!row.get().getItemOut().getType().isAir() && !row.get().getItemOut2().getType().isAir()) {
			int emptySlots = 0;
			for(ItemStack item : player.getInventory().getStorageContents())
				if(item == null || item.getType().isAir())
					emptySlots++;
			if(emptySlots > 1) {
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
		}
		else {
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
		}
		String nameIn1, nameIn2, nameOut1, nameOut2;
		int inA1, inA2, outA1, outA2;
		try {
			if(row.get().getItemIn().getType().equals(Material.ENCHANTED_BOOK) && row.get().getItemIn().getItemMeta() instanceof EnchantmentStorageMeta) {
				EnchantmentStorageMeta metaOut = (EnchantmentStorageMeta) row.get().getItemIn().getItemMeta();
				StringBuilder sbIn1 = new StringBuilder();
				sbIn1.append("enchanted book: ");
				for(Map.Entry<Enchantment, Integer> entry : metaOut.getStoredEnchants().entrySet()) {
					String enchants = entry.getKey().getKey().toString().replaceAll("_", " ").toLowerCase().replaceAll("minecraft:", "");
					sbIn1.append(enchants).append(" ").append(entry.getValue()).append(", ");
				}
				sbIn1.deleteCharAt(sbIn1.length()-1).deleteCharAt(sbIn1.length()-1);
				nameIn1 = sbIn1.toString().toLowerCase();
			}
			else
				nameIn1 = row.get().getItemIn().getItemMeta().hasDisplayName() ? row.get().getItemIn().getItemMeta().getDisplayName() : row.get().getItemIn().getType().name().replaceAll("_", " ").toLowerCase();
			inA1 = row.get().getItemIn().getAmount();
		} catch(Exception e) { nameIn1 = "empty"; inA1 = 0; }
		try {
			if(row.get().getItemIn2().getType().equals(Material.ENCHANTED_BOOK) && row.get().getItemIn2().getItemMeta() instanceof EnchantmentStorageMeta) {
				EnchantmentStorageMeta metaOut = (EnchantmentStorageMeta) row.get().getItemIn2().getItemMeta();
				StringBuilder sbIn2 = new StringBuilder();
				sbIn2.append("enchanted book: ");
				for(Map.Entry<Enchantment, Integer> entry : metaOut.getStoredEnchants().entrySet()) {
					String enchants = entry.getKey().getKey().toString().replaceAll("_", " ").toLowerCase().replaceAll("minecraft:", "");
					sbIn2.append(enchants).append(" ").append(entry.getValue()).append(", ");
				}
				sbIn2.deleteCharAt(sbIn2.length()-1).deleteCharAt(sbIn2.length()-1);
				nameIn2 = sbIn2.toString().toLowerCase();
			}
			else
				nameIn2 = row.get().getItemIn2().getItemMeta().hasDisplayName() ? row.get().getItemIn2().getItemMeta().getDisplayName() : row.get().getItemIn2().getType().name().replaceAll("_", " ").toLowerCase();
			inA2 = row.get().getItemIn2().getAmount();
		} catch(Exception e) { nameIn2 = "empty"; inA2 = 0; }
		try {
			if(row.get().getItemOut().getType().equals(Material.ENCHANTED_BOOK) && row.get().getItemOut().getItemMeta() instanceof EnchantmentStorageMeta) {
				EnchantmentStorageMeta metaOut = (EnchantmentStorageMeta) row.get().getItemOut().getItemMeta();
				StringBuilder sbOut1 = new StringBuilder();
				sbOut1.append("enchanted book: ");
				for(Map.Entry<Enchantment, Integer> entry : metaOut.getStoredEnchants().entrySet()) {
					String enchants = entry.getKey().getKey().toString().replaceAll("_", " ").toLowerCase().replaceAll("minecraft:", "");
					sbOut1.append(enchants).append(" ").append(entry.getValue()).append(", ");
				}
				sbOut1.deleteCharAt(sbOut1.length()-1).deleteCharAt(sbOut1.length()-1);
				nameOut1 = sbOut1.toString().toLowerCase();
			}
			else
				nameOut1 = row.get().getItemOut().getItemMeta().hasDisplayName() ? row.get().getItemOut().getItemMeta().getDisplayName() : row.get().getItemOut().getType().name().replaceAll("_", " ").toLowerCase();
			outA1 = row.get().getItemOut().getAmount();
		} catch(Exception e) { nameOut1 = "empty"; outA1 = 0; }
		try {
			if(row.get().getItemOut2().getType().equals(Material.ENCHANTED_BOOK) && row.get().getItemOut2().getItemMeta() instanceof EnchantmentStorageMeta) {
				EnchantmentStorageMeta metaOut = (EnchantmentStorageMeta) row.get().getItemOut2().getItemMeta();
				StringBuilder sbOut2 = new StringBuilder();
				sbOut2.append("enchanted book: ");
				for(Map.Entry<Enchantment, Integer> entry : metaOut.getStoredEnchants().entrySet()) {
					String enchants = entry.getKey().getKey().toString().replaceAll("_", " ").toLowerCase().replaceAll("minecraft:", "");
					sbOut2.append(enchants).append(" ").append(entry.getValue()).append(", ");
				}
				sbOut2.deleteCharAt(sbOut2.length()-1).deleteCharAt(sbOut2.length()-1);
				nameOut2 = sbOut2.toString().toLowerCase();
			}
			else
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
		final Player shoppingPlayer = player;
		final boolean rowBroadcast = row.get().broadcast;
		final String i1 = nameIn1 + " x " + inA1;
		final String i2 = nameIn2 + " x " + inA2;
		final String o1 = nameOut1 + " x " + outA1;
		final String o2 = nameOut2 + " x " + outA2;
		final int iA1 = inA1;
		final int iA2 = inA2;
		final int oA1 = outA1;
		final int oA2 = outA2;
		Bukkit.getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> sendShopMessages(i1, i2, o1, o2, iA1, iA2, oA1, oA2, this.owner, shoppingPlayer, this.admin, rowBroadcast));
	}

	public boolean hasExpired() {
		if(this.admin || maxDays <= 0)
			return false;
		if(!exemptListInactive)
			for(String exemptCheck : exemptExpiringList)
				if(exemptCheck != null && exemptCheck.equalsIgnoreCase(this.owner.toString()))
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
			if(deletePlayerShop)
				Bukkit.getScheduler().runTask(iShop.getPlugin(), () -> this.location.getBlock().setType(Material.AIR));
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

	public void sendShopMessages(String i1, String i2, String o1, String o2, int inA1, int inA2, int outA1, int outA2, UUID shopOwner, Player player, boolean isAdminShop, boolean rowBroadcast) {
		OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(shopOwner);
		if(!isAdminShop) {
			if(ownerPlayer != null && shopNotifications) {
				if(inA1 == 0 && outA1 == 0) {
					if(ownerPlayer.isOnline()) {
						Player ownerPlayerOnline = Bukkit.getPlayer(shopOwner);
						ownerPlayerOnline.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o2).replaceAll("%out", i2).replaceAll("%p", player.getName()));
						if(stockMessagesSaveAll) {
							ArrayList<String> addMsg;
							if(shopMessages.containsKey(shopOwner))
								addMsg = shopMessages.get(shopOwner);
							else
								addMsg = new ArrayList<>();
							addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o2).replaceAll("%out", i2).replaceAll("%p", player.getName()));
							shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
						}
					}
					else if(stockMessages) {
						ArrayList<String> addMsg;
						if(shopMessages.containsKey(shopOwner))
							addMsg = shopMessages.get(shopOwner);
						else
							addMsg = new ArrayList<>();
						addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o2).replaceAll("%out", i2).replaceAll("%p", player.getName()));
						shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
					}
				}
				else if(inA1 == 0 && outA2 == 0) {
					if(ownerPlayer.isOnline()) {
						Player ownerPlayerOnline = Bukkit.getPlayer(shopOwner);
						ownerPlayerOnline.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1).replaceAll("%out", i2).replaceAll("%p", player.getName()));
						if(stockMessagesSaveAll) {
							ArrayList<String> addMsg;
							if(shopMessages.containsKey(shopOwner))
								addMsg = shopMessages.get(shopOwner);
							else
								addMsg = new ArrayList<>();
							addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o1).replaceAll("%out", i2).replaceAll("%p", player.getName()));
							shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
						}
					}
					else if(stockMessages) {
						ArrayList<String> addMsg;
						if(shopMessages.containsKey(shopOwner))
							addMsg = shopMessages.get(shopOwner);
						else
							addMsg = new ArrayList<>();
						addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o1).replaceAll("%out", i2).replaceAll("%p", player.getName()));
						shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
					}
				}
				else if(inA2 == 0 && outA1 == 0) {
					if(ownerPlayer.isOnline()) {
						Player ownerPlayerOnline = Bukkit.getPlayer(shopOwner);
						ownerPlayerOnline.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o2).replaceAll("%out", i1).replaceAll("%p", player.getName()));
						if(stockMessagesSaveAll) {
							ArrayList<String> addMsg;
							if(shopMessages.containsKey(shopOwner))
								addMsg = shopMessages.get(shopOwner);
							else
								addMsg = new ArrayList<>();
							addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o2).replaceAll("%out", i1).replaceAll("%p", player.getName()));
							shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
						}
					}
					else if(stockMessages) {
						ArrayList<String> addMsg;
						if(shopMessages.containsKey(shopOwner))
							addMsg = shopMessages.get(shopOwner);
						else
							addMsg = new ArrayList<>();
						addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o2).replaceAll("%out", i1).replaceAll("%p", player.getName()));
						shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
					}
				}
				else if(inA2 == 0 && outA2 == 0) {
					if(ownerPlayer.isOnline()) {
						Player ownerPlayerOnline = Bukkit.getPlayer(shopOwner);
						ownerPlayerOnline.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1).replaceAll("%out", i1).replaceAll("%p", player.getName()));
						if(stockMessagesSaveAll) {
							ArrayList<String> addMsg;
							if(shopMessages.containsKey(shopOwner))
								addMsg = shopMessages.get(shopOwner);
							else
								addMsg = new ArrayList<>();
							addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o1).replaceAll("%out", i1).replaceAll("%p", player.getName()));
							shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
						}
					}
					else if(stockMessages) {
						ArrayList<String> addMsg;
						if(shopMessages.containsKey(shopOwner))
							addMsg = shopMessages.get(shopOwner);
						else
							addMsg = new ArrayList<>();
						addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o1).replaceAll("%out", i1).replaceAll("%p", player.getName()));
						shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
					}
				}
				else if(inA1 == 0) {
					if(ownerPlayer.isOnline()) {
						Player ownerPlayerOnline = Bukkit.getPlayer(shopOwner);
						ownerPlayerOnline.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i2).replaceAll("%p", player.getName()));
						if(stockMessagesSaveAll) {
							ArrayList<String> addMsg;
							if(shopMessages.containsKey(shopOwner))
								addMsg = shopMessages.get(shopOwner);
							else
								addMsg = new ArrayList<>();
							addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i2).replaceAll("%p", player.getName()));
							shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
						}
					}
					else if(stockMessages) {
						ArrayList<String> addMsg;
						if(shopMessages.containsKey(shopOwner))
							addMsg = shopMessages.get(shopOwner);
						else
							addMsg = new ArrayList<>();
						addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i2).replaceAll("%p", player.getName()));
						shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
					}
				}
				else if(inA2 == 0) {
					if(ownerPlayer.isOnline()) {
						Player ownerPlayerOnline = Bukkit.getPlayer(shopOwner);
						ownerPlayerOnline.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i1).replaceAll("%p", player.getName()));
						if(stockMessagesSaveAll) {
							ArrayList<String> addMsg;
							if(shopMessages.containsKey(shopOwner))
								addMsg = shopMessages.get(shopOwner);
							else
								addMsg = new ArrayList<>();
							addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i1).replaceAll("%p", player.getName()));
							shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
						}
					}
					else if(stockMessages) {
						ArrayList<String> addMsg;
						if(shopMessages.containsKey(shopOwner))
							addMsg = shopMessages.get(shopOwner);
						else
							addMsg = new ArrayList<>();
						addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i1).replaceAll("%p", player.getName()));
						shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
					}
				}
				else if(outA1 == 0) {
					if(ownerPlayer.isOnline()) {
						Player ownerPlayerOnline = Bukkit.getPlayer(shopOwner);
						ownerPlayerOnline.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o2).replaceAll("%out", i1 + " & " + i2).replaceAll("%p", player.getName()));
						if(stockMessagesSaveAll) {
							ArrayList<String> addMsg;
							if(shopMessages.containsKey(shopOwner))
								addMsg = shopMessages.get(shopOwner);
							else
								addMsg = new ArrayList<>();
							addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o2).replaceAll("%out", i1 + " & " + i2).replaceAll("%p", player.getName()));
							shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
						}
					}
					else if(stockMessages) {
						ArrayList<String> addMsg;
						if(shopMessages.containsKey(shopOwner))
							addMsg = shopMessages.get(shopOwner);
						else
							addMsg = new ArrayList<>();
						addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o2).replaceAll("%out", i1 + " & " + i2).replaceAll("%p", player.getName()));
						shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
					}
				}
				else if(outA2 == 0) {
					if(ownerPlayer.isOnline()) {
						Player ownerPlayerOnline = Bukkit.getPlayer(shopOwner);
						ownerPlayerOnline.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1).replaceAll("%out", i1 + " & " + i2).replaceAll("%p", player.getName()));
						if(stockMessagesSaveAll) {
							ArrayList<String> addMsg;
							if(shopMessages.containsKey(shopOwner))
								addMsg = shopMessages.get(shopOwner);
							else
								addMsg = new ArrayList<>();
							addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o1).replaceAll("%out", i1 + " & " + i2).replaceAll("%p", player.getName()));
							shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
						}
					}
					else if(stockMessages) {
						ArrayList<String> addMsg;
						if(shopMessages.containsKey(shopOwner))
							addMsg = shopMessages.get(shopOwner);
						else
							addMsg = new ArrayList<>();
						addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o1).replaceAll("%out", i1 + " & " + i2).replaceAll("%p", player.getName()));
						shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
					}
				}
				else {
					if(ownerPlayer.isOnline()) {
						Player ownerPlayerOnline = Bukkit.getPlayer(shopOwner);
						ownerPlayerOnline.sendMessage(Messages.SHOP_SELL.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i1 + " & " + i2).replaceAll("%p", player.getName()));
						if(stockMessagesSaveAll) {
							ArrayList<String> addMsg;
							if(shopMessages.containsKey(shopOwner))
								addMsg = shopMessages.get(shopOwner);
							else
								addMsg = new ArrayList<>();
							addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i1 + " & " + i2).replaceAll("%p", player.getName()));
							shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
						}
					}
					else if(stockMessages) {
						ArrayList<String> addMsg;
						if(shopMessages.containsKey(shopOwner))
							addMsg = shopMessages.get(shopOwner);
						else
							addMsg = new ArrayList<>();
						addMsg.add(Messages.SHOP_SELL.toString().replaceAll("%in", o1 + " & " + o2).replaceAll("%out", i1 + " & " + i2).replaceAll("%p", player.getName()));
						shopMessages.put(ownerPlayer.getUniqueId(), addMsg);
					}
				}
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
