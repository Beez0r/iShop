package com.minedhype.ishop.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.md_5.bungee.api.ChatColor;
import static com.minedhype.ishop.inventories.InvStock.inShopInv;

public abstract class GUI {
	public static final Map<String, GUI> inventoriesOpen = new HashMap<>();
	private final Inventory inventory;
	private final Map<Integer, GUIAction> actions;
	
	public GUI(int invDone, String invNumber) {
		inventory = Bukkit.createInventory(null, invDone, invNumber);
		actions = new HashMap<>();
	}
	
	public void onClick(InventoryClickEvent e) {
		e.setCancelled(true);
	}
	
	public void onDrag(InventoryDragEvent e) { }

	public void onClose(InventoryCloseEvent e) { }
	
	public static ItemStack createItem(Material material, String number, short data) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + number);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
		meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
		item.setItemMeta(meta);
		
		return item;
	}
	
	public static ItemStack createItem(Material material, String number) {
		return createItem(material, number, (short) 0);
	}
	
	public void placeItem(int slot, ItemStack item, GUIAction action) {
		inventory.setItem(slot, item);
		if(action != null)
			actions.put(slot, action);
		else
			actions.remove(slot);
	}
	
	public void placeItem(int slot, ItemStack item) {
		placeItem(slot, item, null);
	}
	
	public void open(Player player, UUID uuid) {
		player.openInventory(inventory);
		inventoriesOpen.put(player.getName(), this);
		if(uuid != null)
			inShopInv.putIfAbsent(player, uuid);
		else
			inShopInv.putIfAbsent(player, player.getUniqueId());
	}

	public void open(Player player) { open(player, null); }
	
	public Inventory getInventory() {
		return inventory;
	}

	public Map<Integer, GUIAction> getActions() {
		return actions;
	}
}
