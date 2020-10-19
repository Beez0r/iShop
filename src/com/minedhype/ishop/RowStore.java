package com.minedhype.ishop;

import java.sql.PreparedStatement;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class RowStore {
	private final ItemStack itemOut1;
	private final ItemStack itemOut2;
	private final ItemStack itemIn1;
	private final ItemStack itemIn2;
	private final ItemStack airItem = new ItemStack(Material.AIR);
	public boolean broadcast;
	
	public RowStore(ItemStack itemOut1, ItemStack itemOut2, ItemStack itemIn1, ItemStack itemIn2, boolean broadcast) {
		this.itemOut1 = itemOut1;
		this.itemOut2 = itemOut2;
		this.itemIn1 = itemIn1;
		this.itemIn2 = itemIn2;
		this.broadcast = broadcast;
	}
	
	public void saveData(int idShop) {
		PreparedStatement stmt = null;
		try {
			stmt = iShop.getConnection().prepareStatement("INSERT INTO ishopShopsRows (itemIn1, itemIn2, itemOut1, itemOut2, idShop, broadcast) VALUES (?,?,?,?,?,?);");

			YamlConfiguration configIn1 = new YamlConfiguration();
			if(itemIn1 != null) {
				itemIn1.serialize().forEach(configIn1::set);
				String itemIn1Raw = configIn1.saveToString();
				stmt.setString(1, itemIn1Raw);
			} else {
				airItem.serialize().forEach(configIn1::set);
				String itemIn1Raw = configIn1.saveToString();
				stmt.setString(1, itemIn1Raw);
			}

			YamlConfiguration configIn2 = new YamlConfiguration();
			if(itemIn2 != null) {
				itemIn2.serialize().forEach(configIn2::set);
				String itemIn2Raw = configIn2.saveToString();
				stmt.setString(2, itemIn2Raw);
			} else {
				airItem.serialize().forEach(configIn2::set);
				String itemIn2Raw = configIn2.saveToString();
				stmt.setString(2, itemIn2Raw);
			}

			YamlConfiguration configOut1 = new YamlConfiguration();
			if(itemOut1 != null) {
				itemOut1.serialize().forEach(configOut1::set);
				String itemOut1Raw = configOut1.saveToString();
				stmt.setString(3, itemOut1Raw);
			} else {
				airItem.serialize().forEach(configOut1::set);
				String itemOut1Raw = configOut1.saveToString();
				stmt.setString(3, itemOut1Raw);
			}

			YamlConfiguration configOut2 = new YamlConfiguration();
			if(itemOut2 != null) {
				itemOut2.serialize().forEach(configOut2::set);
				String itemOut2Raw = configOut2.saveToString();
				stmt.setString(4, itemOut2Raw);
			} else {
				airItem.serialize().forEach(configOut2::set);
				String itemOut2Raw = configOut2.saveToString();
				stmt.setString(4, itemOut2Raw);
			}

			stmt.setInt(5, idShop);
			stmt.setBoolean(6, broadcast);
			stmt.execute();
		} catch (Exception e) { e.printStackTrace(); }
			finally {
			try {
				if(stmt != null)
					stmt.close();
			} catch (Exception e) { e.printStackTrace(); }
		}
	}
	
	public void toggleBroadcast() {
		this.broadcast = !this.broadcast;
	}

	public ItemStack getItemIn1() {
		return itemIn1;
	}
	public ItemStack getItemIn2() {
		return itemIn2;
	}
	public ItemStack getItemOut1() {
		return itemOut1;
	}
	public ItemStack getItemOut2() {
		return itemOut2;
	}
}
