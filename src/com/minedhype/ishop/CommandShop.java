package com.minedhype.ishop;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;
import com.minedhype.ishop.inventories.InvCreateRow;
import com.minedhype.ishop.inventories.InvShop;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.block.Block;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import com.minedhype.ishop.inventories.InvAdminShop;
import com.minedhype.ishop.inventories.InvShopList;
import com.minedhype.ishop.inventories.InvStock;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public class CommandShop implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(sender instanceof ConsoleCommandSender && args.length > 0) {
			if(args[0].equalsIgnoreCase("reload")) {
				reloadShop(null);
				return true;
			}
			else if(args[0].equalsIgnoreCase("deleteid") && args[1] != null) {
				deleteShopID(null, args[1]);
				return true;
			}
			else {
				sender.sendMessage(Messages.NOT_A_PLAYER.toString());
				return false;
			}
		}
		if(!(sender instanceof Player)) {
			sender.sendMessage(Messages.NOT_A_PLAYER.toString());
			return false;
		}
		Player player = (Player) sender;
		if(args.length == 0)
			Bukkit.getServer().getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> listSubCmd(player, label));
		else if(args[0].equalsIgnoreCase("adminshop"))
			adminShop(player);
		else if(args[0].equalsIgnoreCase("count") && args.length >= 2)
			Bukkit.getServer().getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> count(player, args[1]));
		else if(args[0].equalsIgnoreCase("create"))
			createStore(player);
		else if(args[0].equalsIgnoreCase("createshop") && args.length >= 2)
			Bukkit.getServer().getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> createShop(player, args[1]));
		else if(args[0].equalsIgnoreCase("delete"))
			deleteShop(player);
		else if(args[0].equalsIgnoreCase("deleteid") && args.length >= 2)
			deleteShopID(player, args[1]);
		else if(args[0].equalsIgnoreCase("list") && args.length == 1)
			Bukkit.getServer().getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> listShops(player, null));
		else if(args[0].equalsIgnoreCase("list") && args.length >= 2)
			Bukkit.getServer().getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> listShops(player, args[1]));
		else if(args[0].equalsIgnoreCase("listadmin"))
			Bukkit.getServer().getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> listAdminShops(player));
		else if(args[0].equalsIgnoreCase("manage") && args.length >= 2)
			shopManage(player, args[1]);
		else if(args[0].equalsIgnoreCase("managestock") && args.length == 2)
			Bukkit.getServer().getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> manageStock(player, args[1], "1"));
		else if(args[0].equalsIgnoreCase("managestock") && args.length >= 3)
			Bukkit.getServer().getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> manageStock(player, args[1], args[2]));
		else if(args[0].equalsIgnoreCase("out") && args.length == 1)
			Bukkit.getServer().getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> outOfStock(player, null));
		else if(args[0].equalsIgnoreCase("out") && args.length >= 2)
			Bukkit.getServer().getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> outOfStock(player, args[1]));
		else if(args[0].equalsIgnoreCase("reload"))
			reloadShop(player);
		else if(args[0].equalsIgnoreCase("shops"))
			listAllShops(player);
		else if(args[0].equalsIgnoreCase("sold") && args.length == 1)
			Bukkit.getServer().getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> shopSold(player, null));
		else if(args[0].equalsIgnoreCase("sold") && args.length >= 2)
			Bukkit.getServer().getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> shopSold(player, args[1]));
		else if(args[0].equalsIgnoreCase("stock") && args.length == 1)
			stockShop(player, "1");
		else if(args[0].equalsIgnoreCase("stock") && args.length >= 2)
			stockShop(player, args[1]);
		else if(args[0].equalsIgnoreCase("view") && args.length >= 2)
			viewShop(player, args[1]);
		else
			Bukkit.getServer().getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> listSubCmd(player, label));

		return true;
	}

	private void listSubCmd(Player player, String label) {
		player.sendMessage(ChatColor.GOLD + "iShop Commands:");
		player.sendMessage(ChatColor.GRAY + "/" + label + " count <item>");
		player.sendMessage(ChatColor.GRAY + "/" + label + " create");
		player.sendMessage(ChatColor.GRAY + "/" + label + " delete");
		player.sendMessage(ChatColor.GRAY + "/" + label + " deleteid <id>");
		player.sendMessage(ChatColor.GRAY + "/" + label + " list");
		if(iShop.config.getBoolean("publicListCommand") || player.hasPermission(Permission.SHOP_ADMIN.toString()) || player.hasPermission(Permission.SHOP_LIST.toString()))
			player.sendMessage(ChatColor.GRAY + "/" + label + " list <player>");
		if(iShop.config.getBoolean("publicShopListCommand") || player.hasPermission(Permission.SHOP_ADMIN.toString()) || player.hasPermission(Permission.SHOP_SHOPS.toString()))
			player.sendMessage(ChatColor.GRAY + "/" + label + " shops");
		player.sendMessage(ChatColor.GRAY + "/" + label + " manage <id>");
		player.sendMessage(ChatColor.GRAY + "/" + label + " out");
		if(player.hasPermission(Permission.SHOP_ADMIN.toString()))
			player.sendMessage(ChatColor.GRAY + "/" + label + " out <player>");
		if(iShop.config.getBoolean("enableShopSoldMessage"));
			player.sendMessage(ChatColor.GRAY + "/" + label + " sold <page/clear>");
		player.sendMessage(ChatColor.GRAY + "/" + label + " stock <page>");
		player.sendMessage(ChatColor.GRAY + "/" + label + " view <id>");
		if(player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(ChatColor.GRAY + "/" + label + " adminshop");
			player.sendMessage(ChatColor.GRAY + "/" + label + " createshop <player>");
			player.sendMessage(ChatColor.GRAY + "/" + label + " listadmin");
			player.sendMessage(ChatColor.GRAY + "/" + label + " managestock <player> <page>");
			player.sendMessage(ChatColor.GRAY + "/" + label + " reload");
		}
	}

	private void count(Player player, String itemName) {
		if(Shop.getNumShops(player.getUniqueId()) < 1 && EventShop.noShopNoStock) {
			player.sendMessage(Messages.NO_SHOP_STOCK.toString());
			return;
		}
		Material material = Material.matchMaterial(itemName);
		if(material == null) {
			try {
				material = Material.matchMaterial(itemName.split("minecraft:")[1].toUpperCase());
			} catch(Exception ignored) { }
			if(material == null) {
				player.sendMessage(Messages.STOCK_COUNT_ERROR.toString());
				return;
			}
		}
		ItemStack item = new ItemStack(material);
		int max = iShop.config.getInt("stockPages");
		int itemAmountCount = 0;
		for(int i=0; i<max; i++) {
			Optional<StockShop> stockStore = StockShop.getStockShopByOwner(player.getUniqueId(),i);
			if(!stockStore.isPresent())
				continue;
			if(stockStore.get().getInventory().contains(item.getType()))
				for(int j=0; j<stockStore.get().getInventory().getSize()-1; j++)
					if(stockStore.get().getInventory().getItem(j) != null && stockStore.get().getInventory().getItem(j).getType().equals(item.getType()))
						itemAmountCount += stockStore.get().getInventory().getItem(j).getAmount();
		}
		if(itemAmountCount>0)
			player.sendMessage(Messages.STOCK_COUNT_AMOUNT.toString().replaceAll("%amount", String.valueOf(itemAmountCount)).replaceAll("%item", itemName));
		else
			player.sendMessage(Messages.STOCK_COUNT_EMPTY.toString().replaceAll("%item", itemName));
	}

	private void createStore(Player player) {
		if(iShop.config.getBoolean("usePermissions") && !player.hasPermission(Permission.SHOP_CREATE.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}
		if(!iShop.config.getBoolean("enableShopBlock")) {
			player.sendMessage(Messages.DISABLED_SHOP_BLOCK.toString());
			return;
		}
		Block block = player.getTargetBlockExact(5);
		if(block == null) {
			player.sendMessage(Messages.TARGET_MISMATCH.toString());
			return;
		}
		if(iShop.config.getBoolean("disableShopInWorld")) {
			List<String> disabledWorldsList = iShop.config.getStringList("disabledWorldList");
			for(String disabledWorlds:disabledWorldsList) {
				if(disabledWorlds != null && block.getWorld().getName().equals(disabledWorlds)) {
					player.sendMessage(Messages.SHOP_WORLD_DISABLED.toString());
					return;
				}
			}
		}
		String shopBlock = iShop.config.getString("shopBlock");
		Material match = Material.matchMaterial(shopBlock);
		if(match == null) {
			try {
				match = Material.matchMaterial(shopBlock.split("minecraft:")[1].toUpperCase());
			} catch(Exception ignored) { }
			if(match == null)
				match = Material.BARREL;
		}
		if(!EventShop.multipleShopBlocks) {
			if(!block.getType().equals(match)) {
				player.sendMessage(Messages.TARGET_MISMATCH.toString());
				return;
			}
		} else {
			boolean shopMatch = false;
			for(String shopBlocks:EventShop.multipleShopBlock) {
				Material shopListBlocks = Material.matchMaterial(shopBlocks);
				if(shopListBlocks != null && block.getType().equals(shopListBlocks)) {
					shopMatch = true;
					break;
				}
			}
			if(!block.getType().equals(match) && !shopMatch) {
				player.sendMessage(Messages.TARGET_MISMATCH.toString());
				return;
			}
		}
		boolean isShopLoc;
		if(iShop.wgLoader != null)
			isShopLoc = iShop.wgLoader.checkRegion(block);
		else
			isShopLoc = true;
		if(!isShopLoc) {
			player.sendMessage(Messages.WG_REGION.toString());
			return;
		}
		boolean allowShopCreateInClaim = false;
		if(iShop.gpLoader != null) {
			Claim claim = GriefPrevention.instance.dataStore.getClaimAt(block.getLocation(), false, false, null);
			if(claim == null || claim.allowAccess(player) == null || claim.allowEdit(player) == null || claim.allowContainers(player) == null)
				allowShopCreateInClaim = true;
		}
		else
			allowShopCreateInClaim = true;
		if(!allowShopCreateInClaim) {
			player.sendMessage(Messages.GP_CLAIM.toString());
			return;
		}
		Optional<Shop> shop = Shop.getShopByLocation(block.getLocation());
		if(shop.isPresent()) {
			player.sendMessage(Messages.EXISTING_SHOP.toString());
			return;
		}
		boolean limitShops;
		int numShops = Shop.getNumShops(player.getUniqueId());
		if(iShop.config.getBoolean("usePermissions")) {
			int maxShops = 0;
			String permPrefix = Permission.SHOP_LIMIT_PREFIX.toString();
			for(PermissionAttachmentInfo attInfo : player.getEffectivePermissions()) {
				String perm = attInfo.getPermission();
				if(perm.startsWith(permPrefix)) {
					int num;
					try {
						num = Integer.parseInt(perm.substring(perm.lastIndexOf(".")+1));
					} catch(Exception e) { num = 0; }
					if(num > maxShops)
						maxShops = num;
				}
			}
			limitShops = numShops >= maxShops;
		}
		else {
			int numConfig = iShop.config.getInt("defaultShopLimit");
			limitShops = numShops >= numConfig && numConfig >= 0;
		}
		if(player.hasPermission(Permission.SHOP_LIMIT_BYPASS.toString()))
			limitShops = false;
		if(limitShops) {
			player.sendMessage(Messages.SHOP_MAX.toString());
			return;
		}
		double cost = iShop.config.getDouble("createCost");
		Optional<Economy> economy = iShop.getEconomy();
		if(cost > 0 && economy.isPresent()) {
			OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
			EconomyResponse res = economy.get().withdrawPlayer(offPlayer, cost);
			if(!res.transactionSuccess()) {
				player.sendMessage(Messages.SHOP_CREATE_NO_MONEY.toString()+cost);
				return;
			}
		}
		Shop newShop = Shop.createShop(block.getLocation(), player.getUniqueId());
		player.sendMessage(Messages.SHOP_CREATED.toString());
		Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(iShop.getPlugin(), () -> {
			Optional<Shop> shops = Shop.getShopByLocation(block.getLocation());
			Shop.shopList.put(shops.get().shopId(), player.getUniqueId());
		}, 10);
		InvAdminShop inv = new InvAdminShop(newShop, player);
		inv.open(player, newShop.getOwner());
	}

	private void createShop(Player player, String playerShop) {
		if(!player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}
		if(!iShop.config.getBoolean("enableShopBlock")) {
			player.sendMessage(Messages.DISABLED_SHOP_BLOCK.toString());
			return;
		}
		Block block = player.getTargetBlockExact(5);
		if(block == null) {
			player.sendMessage(Messages.TARGET_MISMATCH.toString());
			return;
		}
		if(iShop.config.getBoolean("disableShopInWorld")) {
			List<String> disabledWorldsList = iShop.config.getStringList("disabledWorldList");
			for(String disabledWorlds:disabledWorldsList) {
				if(disabledWorlds != null && block.getWorld().getName().equals(disabledWorlds)) {
					player.sendMessage(Messages.SHOP_WORLD_DISABLED.toString());
					return;
				}
			}
		}
		String shopBlock = iShop.config.getString("shopBlock");
		Material match = Material.matchMaterial(shopBlock);
		if(match == null) {
			try {
				match = Material.matchMaterial(shopBlock.split("minecraft:")[1].toUpperCase());
			} catch(Exception ignored) { }
			if(match == null)
				match = Material.BARREL;
		}
		if(!EventShop.multipleShopBlocks) {
			if(!block.getType().equals(match)) {
				player.sendMessage(Messages.TARGET_MISMATCH.toString());
				return;
			}
		} else {
			boolean shopMatch = false;
			for(String shopBlocks:EventShop.multipleShopBlock) {
				Material shopListBlocks = Material.matchMaterial(shopBlocks);
				if(shopListBlocks != null && block.getType().equals(shopListBlocks)) {
					shopMatch = true;
					break;
				}
			}
			if(!block.getType().equals(match) && !shopMatch) {
				player.sendMessage(Messages.TARGET_MISMATCH.toString());
				return;
			}
		}
		UUID shopOwner;
		if(playerShop == null) {
			player.sendMessage(Messages.NO_PLAYER_FOUND.toString());
			return;
		} else {
			Player playerInGame = Bukkit.getPlayer(playerShop);
			if(playerInGame != null && playerInGame.isOnline())
				shopOwner = playerInGame.getUniqueId();
			else {
				try {
					shopOwner = getUUID(playerShop);
				} catch (Exception e) {
					UUID foundPlayerUUID = null;
					boolean foundPlayer = false;
					for(OfflinePlayer offlinePlayers : Bukkit.getOfflinePlayers())
						if(offlinePlayers.getName().equalsIgnoreCase(playerShop)) {
							foundPlayerUUID = offlinePlayers.getUniqueId();
							foundPlayer = true;
							break;
						}
					if(!foundPlayer) {
						player.sendMessage(Messages.NO_PLAYER_FOUND.toString());
						Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[iShop] " + Messages.NO_PLAYER_FOUND);
						return;
					}
					shopOwner = foundPlayerUUID;
				}
			}
		}
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(shopOwner);
		if(offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
			player.sendMessage(Messages.NO_PLAYER_FOUND.toString());
			return;
		}
		Optional<Shop> shop = Shop.getShopByLocation(block.getLocation());
		if(!shop.isPresent()) {
			Shop.createShop(block.getLocation(), shopOwner);
			player.sendMessage(Messages.PLAYER_SHOP_CREATED.toString().replaceAll("%p", playerShop));
			final UUID shopOwnerFinal = shopOwner;
			Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(iShop.getPlugin(), () -> {
				Optional<Shop> shops = Shop.getShopByLocation(block.getLocation());
				Shop.shopList.put(shops.get().shopId(), shopOwnerFinal);
			}, 10);
		} else { player.sendMessage(Messages.EXISTING_SHOP.toString()); }
	}

	private void adminShop(Player player) {
		if(!player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}
		if(!EventShop.adminShopEnabled) {
			player.sendMessage(Messages.ADMIN_SHOP_DISABLED.toString());
			return;
		}
		Block block = player.getTargetBlockExact(5);
		if(block == null) {
			player.sendMessage(Messages.TARGET_MISMATCH.toString());
			return;
		}
		String shopBlock = iShop.config.getString("shopBlock");
		Material match = Material.matchMaterial(shopBlock);
		if(match == null) {
			try {
				match = Material.matchMaterial(shopBlock.split("minecraft:")[1].toUpperCase());
			} catch(Exception ignored) { }
			if(match == null)
				match = Material.BARREL;
		}
		if(!EventShop.multipleShopBlocks) {
			if(!block.getType().equals(match)) {
				player.sendMessage(Messages.TARGET_MISMATCH.toString());
				return;
			}
		} else {
			boolean shopMatch = false;
			for(String shopBlocks:EventShop.multipleShopBlock) {
				Material shopListBlocks = Material.matchMaterial(shopBlocks);
				if(shopListBlocks != null && block.getType().equals(shopListBlocks)) {
					shopMatch = true;
					break;
				}
			}
			if(!block.getType().equals(match) && !shopMatch) {
				player.sendMessage(Messages.TARGET_MISMATCH.toString());
				return;
			}
		}
		Optional<Shop> shop = Shop.getShopByLocation(block.getLocation());
		if(shop.isPresent()) {
			player.sendMessage(Messages.EXISTING_SHOP.toString());
			return;
		}
		String adminHead;
		try { adminHead = iShop.config.getString("adminPlayerHeadShops"); }
			catch(Exception e) { adminHead = "00000000-0000-0000-0000-000000000000"; }
		Shop newShop = Shop.createShop(block.getLocation(), UUID.fromString(adminHead), true);
		player.sendMessage(Messages.SHOP_CREATED.toString());
		if(iShop.config.getBoolean("adminShopPublic")) {
			final String adminHeadFinal = adminHead;
			Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(iShop.getPlugin(), () -> {
				Optional<Shop> shops = Shop.getShopByLocation(block.getLocation());
				Shop.shopList.put(shops.get().shopId(), UUID.fromString(adminHeadFinal));
			}, 10);
		}
		InvAdminShop inv = new InvAdminShop(newShop, player);
		inv.open(player, newShop.getOwner());
	}

	private void deleteShop(Player player) {
		Block block = player.getTargetBlockExact(5);
		if(block == null) {
			player.sendMessage(Messages.TARGET_MISMATCH.toString());
			return;
		}
		Optional<Shop> shop = Shop.getShopByLocation(block.getLocation());
		if(!shop.isPresent()) {
			player.sendMessage(Messages.SHOP_NOT_FOUND.toString());
			return;
		}
		if(!shop.get().isOwner(player.getUniqueId()) && !player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.SHOP_NO_SELF.toString());
			return;
		}
		if(shop.get().isAdmin() && !player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}
		if(InvStock.inShopInv.containsValue(shop.get().getOwner())) {
			player.sendMessage(Messages.SHOP_BUSY.toString());
			return;
		}
		double cost = iShop.config.getDouble("returnAmount");
		Optional<Economy> economy = iShop.getEconomy();
		if(cost > 0 && economy.isPresent()) {
			OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(shop.get().getOwner());
			economy.get().depositPlayer(offPlayer, cost);
		}
		Shop.shopList.remove(shop.get().shopId());
		shop.get().deleteShop();
		player.sendMessage(Messages.SHOP_DELETED.toString());
	}

	private void deleteShopID(Player player, String shopId) {
		int sID;
		try {
			sID = Integer.parseInt(shopId);
		} catch (Exception e) { sID = -1; }
		if(sID < 0) {
			if(player != null)
				player.sendMessage(Messages.SHOP_ID_INTEGER.toString());
			else
				Bukkit.getConsoleSender().sendMessage(Messages.SHOP_ID_INTEGER.toString());
			return;
		}
		Optional<Shop> shop = Shop.getShopById(sID);
		if(!shop.isPresent()) {
			if(player != null)
				player.sendMessage(Messages.SHOP_NOT_FOUND.toString());
			else
				Bukkit.getConsoleSender().sendMessage(Messages.SHOP_NOT_FOUND.toString());
			return;
		}
		if(player != null && !shop.get().isOwner(player.getUniqueId()) && !player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.SHOP_NO_SELF.toString());
			return;
		}
		if(shop.get().isAdmin() && player != null && !player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}
		if(InvStock.inShopInv.containsValue(shop.get().getOwner())) {
			if(player != null)
				player.sendMessage(Messages.SHOP_BUSY.toString());
			else
				Bukkit.getConsoleSender().sendMessage(Messages.SHOP_BUSY.toString());
			return;
		}
		double cost = iShop.config.getDouble("returnAmount");
		Optional<Economy> economy = iShop.getEconomy();
		if(cost > 0 && economy.isPresent()) {
			OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(shop.get().getOwner());
			economy.get().depositPlayer(offPlayer, cost);
		}
		Shop.shopList.remove(shop.get().shopId());
		shop.get().deleteShop();
		if(player != null)
			player.sendMessage(Messages.SHOP_IDDELETED.toString().replaceAll("%id", shopId));
		else
			Bukkit.getConsoleSender().sendMessage(Messages.SHOP_IDDELETED.toString().replaceAll("%id", shopId));
	}

	private void listShops(Player player, String playerName) {
		if(playerName != null && !iShop.config.getBoolean("publicListCommand") && !player.hasPermission(Permission.SHOP_ADMIN.toString()) || !player.hasPermission(Permission.SHOP_LIST.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}
		UUID sOwner;
		if(playerName == null) {
			sOwner = player.getUniqueId();
			playerName = player.getDisplayName();
		} else {
			try {
				sOwner = getUUID(playerName);
			} catch (Exception e) {
				player.sendMessage(Messages.NO_PLAYER_SHOP.toString());
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[iShop] " + Messages.NO_PLAYER_SHOP);
				return;
			}
		}
		Shop.getShopList(player, sOwner, playerName);
	}

	private void listAdminShops(Player player) {
		if(!player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}
		Shop.getAdminShopList(player);
	}

	private void listAllShops(Player player) {
		if(!InvShop.listAllShops && !player.hasPermission(Permission.SHOP_ADMIN.toString()) || !player.hasPermission(Permission.SHOP_SHOPS.toString())) {
			player.sendMessage(Messages.SHOP_LIST_DISABLED.toString());
			return;
		}
		InvShopList inv = InvShopList.setShopTitle(Messages.SHOP_LIST_ALL.toString());
		inv.setPag(0);
		inv.open(player);
	}

	private void stockShop(Player player, String page) {
		if(!InvAdminShop.stockCommandEnabled && !player.hasPermission(Permission.SHOP_ADMIN.toString()) && !player.hasPermission(Permission.SHOP_STOCK.toString())) {
			player.sendMessage(Messages.STOCK_COMMAND_DISABLED.toString());
			return;
		}
		if(Shop.getNumShops(player.getUniqueId()) < 1 && iShop.config.getBoolean("mustOwnShopForStock")) {
			player.sendMessage(Messages.NO_SHOP_STOCK.toString());
			return;
		}
		if(EventShop.stockRangeLimit > 0 && iShop.config.getBoolean("stockRangeLimitUsingCommand") && !player.hasPermission(Permission.SHOP_ADMIN.toString()) && !player.hasPermission(Permission.SHOP_STOCK.toString()))
			if(!Shop.checkShopDistanceFromStockBlock(player.getLocation(), player.getUniqueId())) {
				player.sendMessage(Messages.SHOP_FAR.toString());
				return;
			}
		int openPage;
		try { openPage = Integer.parseInt(page); }
		catch(Exception e) { openPage = 1; }
		if(openPage < 1) {
			player.sendMessage(Messages.STOCK_INTEGER.toString());
			return;
		}
		openPage--;
		int stockPages = iShop.config.getInt("stockPages");
		if(openPage > 0 && openPage > stockPages-1)
			openPage = stockPages-1;
		if(InvStock.inShopInv.containsValue(player.getUniqueId())) {
			player.sendMessage(Messages.SHOP_BUSY.toString());
			return;
		} else { InvStock.inShopInv.put(player, player.getUniqueId()); }
		InvStock inv = InvStock.getInvStock(player.getUniqueId());
		inv.setPag(openPage);
		inv.open(player);
	}

	private void reloadShop(Player player) {
		if(player != null && !player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}
		iShop plugin = (iShop) Bukkit.getPluginManager().getPlugin("iShop");
		if(plugin != null)
			plugin.createConfig();
		if(player != null)
			player.sendMessage(ChatColor.GREEN + "[iShop] " + Messages.SHOP_RELOAD);
		else
			Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[iShop] " + Messages.SHOP_RELOAD);
		Bukkit.getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> {
			EventShop.adminShopEnabled = iShop.config.getBoolean("enableAdminShop");
			EventShop.noShopNoStock = iShop.config.getBoolean("mustOwnShopForStock");
			EventShop.placeFrameSign = iShop.config.getBoolean("placeItemFrameSigns");
			EventShop.protectShopFromExplosion = iShop.config.getBoolean("protectShopBlocksFromExplosions");
			EventShop.shopBlock = iShop.config.getString("shopBlock");
			EventShop.stockBlock = iShop.config.getString("stockBlock");
			EventShop.stockEnabled = iShop.config.getBoolean("enableStockBlock");
			EventShop.shopEnabled = iShop.config.getBoolean("enableShopBlock");
			EventShop.shopBlk = Material.matchMaterial(EventShop.shopBlock);
			EventShop.stockBlk = Material.matchMaterial(EventShop.stockBlock);
			EventShop.stockRangeLimit = iShop.config.getInt("stockRangeLimitFromShop");
			EventShop.soldJoinMessage = iShop.config.getBoolean("enableSoldNotificationOnJoin");
			EventShop.soldOnlyOnFirstConnect = iShop.config.getBoolean("onlyNotifySoldOnceUntilClear");
			EventShop.soldMessageDelayTime = iShop.config.getInt("soldNotificationsDelayTime");
			EventShop.multipleShopBlock = iShop.config.getStringList("shopBlockList");
			EventShop.multipleStockBlock = iShop.config.getStringList("stockBlockList");
			EventShop.multipleShopBlocks = iShop.config.getBoolean("multipleShopBlocks");
			EventShop.multipleStockBlocks = iShop.config.getBoolean("multipleStockBlocks");
			InvAdminShop.remoteManage = iShop.config.getBoolean("remoteManage");
			InvAdminShop.stockCommandEnabled = iShop.config.getBoolean("enableStockCommand");
			InvAdminShop.stockGUIShop = iShop.config.getBoolean("enableStockAccessFromShopGUI");
			InvCreateRow.disabledItemList = iShop.config.getStringList("disabledItemsList");
			InvCreateRow.itemsDisabled = iShop.config.getBoolean("disabledItems");
			InvCreateRow.strictStock = iShop.config.getBoolean("strictStock");
			InvShop.listAllShops = iShop.config.getBoolean("publicShopListCommand");
			Shop.showOwnedShops = iShop.config.getBoolean("publicShopListShowsOwned");
			Shop.shopEnabled = iShop.config.getBoolean("enableShopBlock");
			Shop.shopNotifications = iShop.config.getBoolean("enableShopNotifications");
			Shop.shopOutStock = iShop.config.getBoolean("enableOutOfStockMessages");
			Shop.particleEffects = iShop.config.getBoolean("showParticles");
			Shop.maxDays = iShop.config.getInt("maxInactiveDays");
			Shop.deletePlayerShop = iShop.config.getBoolean("deleteBlock");
			Shop.stockMessages = iShop.config.getBoolean("enableShopSoldMessage");
			Shop.exemptExpiringList = iShop.config.getStringList("exemptExpiringShops");
		});
	}

	private static UUID getUUID(String name) throws Exception {
		Scanner scanner = new Scanner(new URL("https://api.mojang.com/users/profiles/minecraft/" + name).openStream());
		String input = scanner.nextLine();
		scanner.close();
		JSONObject UUIDObject = (JSONObject) JSONValue.parseWithException(input);
		String uuidString = UUIDObject.get("id").toString();
		String uuidSeparation = uuidString.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5");
		return UUID.fromString(uuidSeparation);
	}

	private static void shopManage(Player player, String shopID) {
		if(!InvAdminShop.remoteManage && !player.hasPermission(Permission.SHOP_ADMIN.toString()) || !player.hasPermission(Permission.SHOP_REMOTEMANAGE.toString())) {
			player.sendMessage(Messages.SHOP_REMOTE.toString());
			return;
		}
		int shopId;
		try {
			shopId = Integer.parseInt(shopID);
		} catch (Exception e) { shopId = -1; }
		if(shopId < 0) {
			player.sendMessage(Messages.SHOP_NO_SELF.toString());
			return;
		}
		Optional<Shop> shop = Shop.getShopById(shopId);
		if(!shop.isPresent() || (!shop.get().isOwner(player.getUniqueId()) && !player.hasPermission(Permission.SHOP_ADMIN.toString()))) {
			player.sendMessage(Messages.SHOP_NO_SELF.toString());
			return;
		}
		if(shop.get().isAdmin()) {
			if(!EventShop.adminShopEnabled) {
				player.sendMessage(Messages.ADMIN_SHOP_DISABLED.toString());
				return;
			}
			if(!player.hasPermission(Permission.SHOP_ADMIN.toString())) {
				player.sendMessage(Messages.NO_PERMISSION.toString());
				return;
			}
		}
		if(InvStock.inShopInv.containsValue(shop.get().getOwner())) {
			player.sendMessage(Messages.SHOP_BUSY.toString());
			return;
		}
		InvAdminShop inv = new InvAdminShop(shop.get(), player);
		inv.open(player, shop.get().getOwner());
	}

	private static void viewShop(Player player, String shopId) {
		if(!iShop.config.getBoolean("remoteShopping") && !player.hasPermission(Permission.SHOP_ADMIN.toString()) || !player.hasPermission(Permission.SHOP_REMOTESHOPPING.toString())) {
			player.sendMessage(Messages.SHOP_NO_REMOTE.toString());
			return;
		}
		int sID;
		try {
			sID = Integer.parseInt(shopId);
		} catch (Exception e) { sID = -1; }
		if(sID < 0) {
			player.sendMessage(Messages.SHOP_ID_INTEGER.toString());
			return;
		}
		Optional<Shop> shop = Shop.getShopById(sID);
		if(!shop.isPresent()) {
			player.sendMessage(Messages.SHOP_NOT_FOUND.toString());
			return;
		}
		if(shop.get().getOwner().equals(player.getUniqueId()) && !InvAdminShop.remoteManage && !player.hasPermission(Permission.SHOP_ADMIN.toString()) || !player.hasPermission(Permission.SHOP_REMOTEMANAGE.toString())) {
			player.sendMessage(Messages.SHOP_REMOTE.toString());
			return;
		}
		if(shop.get().isAdmin() && !EventShop.adminShopEnabled) {
			player.sendMessage(Messages.ADMIN_SHOP_DISABLED.toString());
			return;
		}
		if(InvStock.inShopInv.containsValue(shop.get().getOwner())) {
			player.sendMessage(Messages.SHOP_BUSY.toString());
			return;
		}
		if((shop.get().isAdmin() && player.hasPermission(Permission.SHOP_ADMIN.toString())) || shop.get().isOwner(player.getUniqueId())) {
			InvAdminShop inv = new InvAdminShop(shop.get(), player);
			inv.open(player, shop.get().getOwner());
		} else {
			InvShop inv = new InvShop(shop.get());
			inv.open(player, shop.get().getOwner());
		}
	}

	private void manageStock(Player player, String stockOwner, String page) {
		if(!player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}
		UUID sOwner;
		if(stockOwner == null) {
			player.sendMessage(Messages.NO_PLAYER_FOUND.toString());
			return;
		} else {
			try {
				sOwner = getUUID(stockOwner);
			} catch (Exception e) {
				player.sendMessage(Messages.NO_PLAYER_FOUND.toString());
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[iShop] " + Messages.NO_PLAYER_FOUND);
				return;
			}
		}
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(sOwner);
		if(offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
			player.sendMessage(Messages.NO_PLAYER_FOUND.toString());
			return;
		}
		int pageNum;
		try { pageNum = Integer.parseInt(page); }
		catch(Exception e) { pageNum = 1; }
		if(pageNum < 1) {
			player.sendMessage(Messages.STOCK_INTEGER.toString());
			return;
		}
		pageNum--;
		int stockPages = iShop.config.getInt("stockPages");
		if(pageNum > 0 && pageNum > stockPages-1)
			pageNum = stockPages-1;
		final int openPage = pageNum;
		Bukkit.getScheduler().runTask(iShop.getPlugin(), () -> {
			if(InvStock.inShopInv.containsValue(sOwner)) {
				player.sendMessage(Messages.SHOP_BUSY.toString());
				return;
			} else
				InvStock.inShopInv.put(player, sOwner);
			InvStock inv = InvStock.getInvStock(sOwner);
			inv.setPag(openPage);
			inv.open(player);
		});
	}

	private void shopSold(Player player, String clearOrPageNumber) {
		if(Shop.stockMessages) {
			int pageNum;
			if(clearOrPageNumber == null)
				pageNum=1;
			else if(clearOrPageNumber.equals("clear") && Shop.shopMessages.containsKey(player.getUniqueId())) {
				Shop.shopMessages.remove(player.getUniqueId());
				EventShop.soldListSent.remove(player.getUniqueId());
				player.sendMessage(Messages.SOLD_CLEAR.toString());
				return;
			}
			else {
				try {
					pageNum = Integer.parseInt(clearOrPageNumber);
				} catch (Exception e) {
					pageNum=1;
				}
				if(pageNum<1) {
					player.sendMessage(Messages.SOLD_INTEGER_ERROR.toString());
					return;
				}
			}
			if(Shop.shopMessages.containsKey(player.getUniqueId())) {
				List<String> messages = Shop.shopMessages.get(player.getUniqueId());
				int msgSize = messages.size();
				int maxSoldPages = (int)Math.ceil(msgSize/5);
				if(pageNum>maxSoldPages)
					pageNum=maxSoldPages+1;
				player.sendMessage(Messages.SOLD_HEADER.toString().replaceAll("%p", String.valueOf(pageNum)));
				pageNum--;
				if(msgSize<6) {
					for(String msg:messages)
						player.sendMessage(msg);
					if(iShop.config.getBoolean("autoClearSoldListOnLast")) {
						Shop.shopMessages.remove(player.getUniqueId());
						EventShop.soldListSent.remove(player.getUniqueId());
					}
				} else {
					int index;
					if(pageNum>0)
						index=pageNum*5;
					else
						index=0;
					for(int i=0; i<5; i++)
						if(index<=msgSize-1) {
							player.sendMessage(messages.get(index));
							index++;
						}
					if(index<6) {
						int pageNext = pageNum+2;
						int currentPage = pageNum+1;
						String soldPages = Messages.SOLD_PAGES.toString().replaceAll("%p", String.valueOf(currentPage));
						TextComponent soldMsg = new TextComponent(soldPages);
						TextComponent pageNextText = new TextComponent(Messages.SOLD_PAGES_NEXT.toString());
						pageNextText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/shop sold " + pageNext));
						soldMsg.addExtra(pageNextText);
						player.spigot().sendMessage(soldMsg);
					} else if(index<msgSize-1) {
						int pageNext = pageNum+2;
						int currentPage = pageNum+1;
						String prevPage = "";
						TextComponent totalMsg = new TextComponent(prevPage);
						TextComponent pagePrevText = new TextComponent(Messages.SOLD_PAGES_PREVIOUS.toString());
						pagePrevText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/shop sold " + pageNum));
						String soldPages = Messages.SOLD_PAGES.toString().replaceAll("%p", String.valueOf(currentPage));
						TextComponent soldMsg = new TextComponent(soldPages);
						TextComponent pageNextText = new TextComponent(Messages.SOLD_PAGES_NEXT.toString());
						pageNextText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/shop sold " + pageNext));
						totalMsg.addExtra(pagePrevText);
						totalMsg.addExtra(soldMsg);
						totalMsg.addExtra(pageNextText);
						player.spigot().sendMessage(totalMsg);
					} else {
						if(iShop.config.getBoolean("autoClearSoldListOnLast")) {
							Shop.shopMessages.remove(player.getUniqueId());
							EventShop.soldListSent.remove(player.getUniqueId());
						} else {
							int currentPage = pageNum+1;
							String prevPage = "";
							TextComponent totalMsg = new TextComponent(prevPage);
							TextComponent pagePrevText = new TextComponent(Messages.SOLD_PAGES_PREVIOUS.toString());
							pagePrevText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/shop sold " + pageNum));
							String soldPages = Messages.SOLD_PAGES.toString().replaceAll("%p", String.valueOf(currentPage));
							TextComponent soldMsg = new TextComponent(soldPages);
							totalMsg.addExtra(pagePrevText);
							totalMsg.addExtra(soldMsg);
							player.spigot().sendMessage(totalMsg);
						}
					}
				}
			}
			else
				player.sendMessage(Messages.SOLD_NOTHING.toString());
		}
		else
			player.sendMessage(Messages.SOLD_COMMAND_DISABLED.toString());
	}

	private void outOfStock(Player player, String playerName) {
		if(playerName != null && !player.hasPermission(Permission.SHOP_ADMIN.toString())) {
			player.sendMessage(Messages.NO_PERMISSION.toString());
			return;
		}
		UUID sOwner;
		if(playerName == null) {
			sOwner = player.getUniqueId();
			playerName = player.getDisplayName();
		} else {
			try {
				sOwner = getUUID(playerName);
			} catch (Exception e) {
				player.sendMessage(Messages.NO_PLAYER_FOUND.toString());
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[iShop] " + Messages.NO_PLAYER_FOUND);
				return;
			}
		}
		player.sendMessage(Messages.SHOP_LIST_OUT.toString());
		Shop.getOutOfStock(player, sOwner, playerName);
	}
}
