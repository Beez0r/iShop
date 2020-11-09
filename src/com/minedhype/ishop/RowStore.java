package com.minedhype.ishop;

import java.sql.PreparedStatement;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class RowStore {
	private final ItemStack itemOut;
	private final ItemStack itemOut2;
	private final ItemStack itemIn;
	private final ItemStack itemIn2;
	private final ItemStack airItem = new ItemStack(Material.AIR, 0);
	public boolean broadcast;
	
	public RowStore(ItemStack itemOut, ItemStack itemOut2, ItemStack itemIn, ItemStack itemIn2, boolean broadcast) {
		this.itemOut = itemOut;
		this.itemOut2 = itemOut2;
		this.itemIn = itemIn;
		this.itemIn2 = itemIn2;
		this.broadcast = broadcast;
	}
	
	public void saveData(int idTienda) {
		PreparedStatement stmt = null;
		try {
			stmt = iShop.getConnection().prepareStatement("INSERT INTO zooMercaTiendasFilas (itemIn, itemIn2, itemOut, itemOut2, idTienda, broadcast) VALUES (?,?,?,?,?,?);");
			YamlConfiguration configIn1 = new YamlConfiguration();
			if(itemIn != null) {
				itemIn.serialize().forEach(configIn1::set);
				String itemInRaw = configIn1.saveToString();
				stmt.setString(1, itemInRaw);
			} else {
				airItem.serialize().forEach(configIn1::set);
				String itemInRaw = configIn1.saveToString();
				stmt.setString(1, itemInRaw);
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
			if(itemOut != null) {
				itemOut.serialize().forEach(configOut1::set);
				String itemOutRaw = configOut1.saveToString();
				stmt.setString(3, itemOutRaw);
			} else {
				airItem.serialize().forEach(configOut1::set);
				String itemOutRaw = configOut1.saveToString();
				stmt.setString(3, itemOutRaw);
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
			stmt.setInt(5, idTienda);
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
	public ItemStack getItemIn() {
		return itemIn;
	}
	public ItemStack getItemIn2() {
		return itemIn2;
	}
	public ItemStack getItemOut() {
		return itemOut;
	}
	public ItemStack getItemOut2() {
		return itemOut2;
	}
}
