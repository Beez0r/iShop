package com.minedhype.ishop;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import com.minedhype.ishop.gui.GUIEvent;
import com.minedhype.ishop.MetricsLite;
import net.milkbowl.vault.economy.Economy;

public class iShop extends JavaPlugin {
	File configFile;
	public static FileConfiguration config;
	public static WorldGuardLoader wgLoader = null;
	private static String chainConnect;
	private static Economy economy = null;
	private static Connection connection = null;

	@Override
	public void onLoad() {
		Plugin wgCheck = Bukkit.getPluginManager().getPlugin("WorldGuard");
		if(wgCheck != null)
			wgLoader = new WorldGuardLoader();
	}

	@Override
	public void onEnable() {
		chainConnect = "jdbc:sqlite:"+getDataFolder().getAbsolutePath()+"/shops.db";
		this.setupEconomy();
		this.createConfig();

		if(config.getString("shopBlock") == null) {
			config.set("shopBlock", "minecraft:barrel");
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[iShop] " + Messages.NO_SHOP_BLOCK.toString());
		}
		if(config.getString("stockBlock") == null) {
			config.set("stockBlock", "minecraft:composter");
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[iShop] " + Messages.NO_STOCK_BLOCK.toString());
		}

		getServer().getPluginManager().registerEvents(new EventShop(), this);
		getServer().getPluginManager().registerEvents(new GUIEvent(), this);
		getCommand("ishop").setExecutor(new CommandShop());

		try {
			connection = DriverManager.getConnection(chainConnect);
			this.createTables();
			Shop.loadData();
		} catch(Exception e) { e.printStackTrace(); }

		Bukkit.getScheduler().runTaskTimerAsynchronously(this, Shop::expiredShops, 10, 3000);
		Bukkit.getScheduler().runTaskTimerAsynchronously(this, Shop::saveData, 500, 6000);
		Bukkit.getScheduler().runTaskTimerAsynchronously(this, Shop::tickShops, 100, 50);
		Bukkit.getScheduler().runTaskLaterAsynchronously(this, Shop::getPlayersShopList, 60);
		MetricsLite metrics = new MetricsLite(this, 9189);
		new UpdateChecker(this, 84555).getVersion(version -> {
			if(!this.getDescription().getVersion().equalsIgnoreCase(version))
				getServer().getConsoleSender().sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[iShop] There is a new update available! - https://www.spigotmc.org/resources/ishop.84555/");
		});
	}

	@Override
	public void onDisable() {
		Shop.saveData();

		if(connection != null) {
			try {
				connection.close();
			} catch (SQLException e) { e.printStackTrace(); }
		}
	}

	private void createTables() {
		PreparedStatement[] stmts = new PreparedStatement[] {};
		try {
			stmts = new PreparedStatement[] {
					connection.prepareStatement("CREATE TABLE IF NOT EXISTS zooMercaTiendas(id INTEGER PRIMARY KEY autoincrement, location varchar(64), owner varchar(64));"),
					connection.prepareStatement("CREATE TABLE IF NOT EXISTS zooMercaTiendasFilas(itemIn text, itemOut text, idTienda INTEGER);"),
					connection.prepareStatement("CREATE TABLE IF NOT EXISTS zooMercaStocks(owner varchar(64), items JSON);")
			};
		} catch(Exception e) { e.printStackTrace(); }

		for(PreparedStatement stmt : stmts) {
			try {
				stmt.execute();
				stmt.close();
			} catch(Exception e) { e.printStackTrace(); }
		}

		List<PreparedStatement> stmtsPatches = new ArrayList<>();
		try {
			stmtsPatches.add(connection.prepareStatement("ALTER TABLE zooMercaTiendasFilas ADD COLUMN itemIn2 text NULL DEFAULT 'v: 2580\ntype: AIR\namount: 0' "));
			stmtsPatches.add(connection.prepareStatement("ALTER TABLE zooMercaTiendasFilas ADD COLUMN itemOut2 text NULL DEFAULT 'v: 2580\ntype: AIR\namount: 0' "));
			stmtsPatches.add(connection.prepareStatement("ALTER TABLE zooMercaTiendasFilas ADD COLUMN broadcast BOOLEAN DEFAULT 0"));
			stmtsPatches.add(connection.prepareStatement("ALTER TABLE zooMercaStocks ADD COLUMN pag INTEGER DEFAULT 0"));
			stmtsPatches.add(connection.prepareStatement("ALTER TABLE zooMercaTiendas ADD COLUMN admin BOOLEAN DEFAULT FALSE;"));
		} catch(Exception ignored) { }

		for(PreparedStatement stmtsPatch : stmtsPatches) {
			try {
				stmtsPatch.execute();
				stmtsPatch.close();
			} catch (Exception ignored) { }
		}
	}

	public static Connection getConnection() {
		checkConnection();
		return connection;
	}

	public static void checkConnection() {
		try {
			if(connection == null || connection.isClosed() || !connection.isValid(0))
				connection = DriverManager.getConnection(chainConnect);
		} catch(Exception e) { e.printStackTrace(); }
	}

	private void setupEconomy() {
		if(getServer().getPluginManager().getPlugin("Vault") == null)
			return;

		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if(rsp == null)
			return;

		economy = rsp.getProvider();
	}

	public static Optional<Economy> getEconomy() {
		return Optional.ofNullable(economy);
	}

	public void createConfig() {
		this.configFile = new File(getDataFolder(), "config.yml");
		if(!this.configFile.exists()) {
			this.configFile.getParentFile().mkdirs();
			saveResource("config.yml", false);
		}

		config = new YamlConfiguration();
		try {
			config.load(this.configFile);
			String ver = config.getDouble("configVersion")+"";
			switch(ver) {
				case "1.0":
					config.set("adminShopDisabled", "&cAdmin shops have been disabled!");
					config.set("clickManage", "&6MANAGE");
					config.set("clickShop", "&6SHOP");
					config.set("listAdminShop", "&6Listing all found admin shops:");
					config.set("manageText", "&6Manage this shop!");
					config.set("noAdminShopsFound", "&cNo admin shops have been found!");
					config.set("noShopBlock", "&cshopBlock cannot be empty! Reverting to default minecraft:barrel");
					config.set("noStockBlock", "&cstockBlock cannot be empty! Reverting to default minecraft:composter");
					config.set("notPlayer", "&cOnly players in the game can use shop commands!");
					config.set("shopText", "&6Remotely shop here!");
				case "1.1":
					config.set("buyTitle","PRICE TO BUY ITEMS [SLOT 1]");
					config.set("buyTitle2","PRICE TO BUY ITEMS [SLOT 2]");
					config.set("sellTitle","ITEMS FOR SALE [SLOT 1]");
					config.set("sellTitle2","ITEMS FOR SALE [SLOT 2]");
					config.set("foundShops", "&6Found&a %shops &6shops(s) for player:&a %p");
					config.set("location", "&6Shop&a %id &6location XYZ: ");
					config.set("enableShopNotifications", true);
					config.set("enableOutOfStockMessages", true);
					config.set("mustOwnShopForStock", true);
				case "2.0":
					config.set("publicShopListCommand", true);
					config.set("shopListTitle", "Shops List");
					config.set("shopNumber", " shop #");
					config.set("normalShop", "%player%'s Shop #");
					config.set("adminShop", "Admin Shop #");
				case "2.1":
				case "2.2":
					config.set("publicShopListShowsOwned", true);
					config.set("shopListDisabled", "&cShops list has been disabled!");
				case "2.3":
					config.set("enableStockAccessFromShopGUI", true);
					config.set("configVersion", 2.4);
					config.save(configFile);
				case "2.4":
					break;
			}
		} catch(IOException | InvalidConfigurationException e) { e.printStackTrace(); }
	}
	public static iShop getPlugin() {
		return iShop.getPlugin(iShop.class);
	}
}
