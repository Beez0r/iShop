package com.minedhype.ishop;

import java.sql.PreparedStatement;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class RowStore {
	private final ItemStack itemOut;
	private final ItemStack itemIn;
	public boolean broadcast;
	
	public RowStore(ItemStack itemOut, ItemStack itemIn, boolean broadcast) {
		this.itemOut = itemOut;
		this.itemIn = itemIn;
		this.broadcast = broadcast;
	}
	
	public void saveData(int idTienda) {
		PreparedStatement stmt = null;
		try {
			stmt = iShop.getConnection().prepareStatement("INSERT INTO zooMercaTiendasFilas (itemIn, itemOut, idTienda, broadcast) VALUES (?,?,?,?);");
			
			YamlConfiguration configIn = new YamlConfiguration();
			itemIn.serialize().forEach(configIn::set);
			String itemInRaw = configIn.saveToString();
			stmt.setString(1, itemInRaw);
			
			YamlConfiguration configOut = new YamlConfiguration();
			itemOut.serialize().forEach(configOut::set);
			String itemOutRaw = configOut.saveToString();
			stmt.setString(2, itemOutRaw);
			stmt.setInt(3, idTienda);
			stmt.setBoolean(4, broadcast);
			stmt.execute();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(stmt != null)
					stmt.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void toggleBroadcast() {
		this.broadcast = !this.broadcast;
	}
	
	public ItemStack getItemOut() {
		return itemOut;
	}
	
	public ItemStack getItemIn() {
		return itemIn;
	}
}
