package com.minedhype.ishop;

import java.sql.PreparedStatement;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
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
			stmt = iShop.getConnection().prepareStatement("INSERT INTO zooMercaTiendasFilas (itemInNew, itemIn2New, itemOutNew, itemOut2New, idTienda, broadcast) VALUES (?,?,?,?,?,?);");
			ItemStack[] saveAirItem = new ItemStack[]{airItem};
			final Inventory invIn = Bukkit.createInventory(null,9);
			final Inventory invIn2 = Bukkit.createInventory(null,9);
			final Inventory invOut = Bukkit.createInventory(null,9);
			final Inventory invOut2 = Bukkit.createInventory(null,9);
			if(itemIn != null) {
				ItemStack[] item = new ItemStack[]{itemIn};
				invIn.addItem(item[0]);
			} else
				invIn.addItem(saveAirItem[0]);
			stmt.setBytes(1, iShop.encodeByte(invIn.getContents()));
			if(itemIn2 != null) {
				ItemStack[] item = new ItemStack[]{itemIn2};
				invIn2.addItem(item[0]);
			} else
				invIn2.addItem(saveAirItem[0]);
			stmt.setBytes(2, iShop.encodeByte(invIn2.getContents()));
			if(itemOut != null) {
				ItemStack[] item = new ItemStack[]{itemOut};
				invOut.addItem(item[0]);
			} else
				invOut.addItem(saveAirItem[0]);
			stmt.setBytes(3, iShop.encodeByte(invOut.getContents()));
			if(itemOut2 != null) {
				ItemStack[] item = new ItemStack[]{itemOut2};
				invOut2.addItem(item[0]);
			} else
				invOut2.addItem(saveAirItem[0]);
			stmt.setBytes(4, iShop.encodeByte(invOut2.getContents()));
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
		if(itemIn == null)
			return airItem;
		return itemIn;
	}
	public ItemStack getItemIn2() {
		if(itemIn2 == null)
			return airItem;
		return itemIn2;
	}
	public ItemStack getItemOut() {
		if(itemOut == null)
			return airItem;
		return itemOut;
	}
	public ItemStack getItemOut2() {
		if(itemOut2 == null)
			return airItem;
		return itemOut2;
	}
}
