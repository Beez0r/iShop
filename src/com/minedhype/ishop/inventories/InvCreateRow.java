package com.minedhype.ishop.inventories;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.minedhype.ishop.RowStore;
import com.minedhype.ishop.Messages;
import com.minedhype.ishop.Shop;
import com.minedhype.ishop.iShop;
import com.minedhype.ishop.gui.GUI;
import org.bukkit.inventory.meta.BlockStateMeta;
import java.util.List;

public class InvCreateRow extends GUI {
	private ItemStack itemIn;
	private ItemStack itemIn2;
	private ItemStack itemOut;
	private ItemStack itemOut2;
	private final ItemStack airItem = new ItemStack(Material.AIR, 0);
	public static Boolean itemsDisabled = iShop.config.getBoolean("disabledItems");
	public static List<String> disabledItemList = iShop.config.getStringList("disabledItemsList");
	
	public InvCreateRow(Shop shop, int index) {
		super(9*3, Messages.SHOP_TITLE_CREATESHOP.toString());
		for(int i=0; i<9*3; i++) {
			if(i == 1)
				placeItem(i, GUI.createItem(Material.OAK_SIGN, ChatColor.GREEN + Messages.SHOP_TITLE_SELL.toString()));
			else if(i == 2)
				placeItem(i, GUI.createItem(Material.OAK_SIGN, ChatColor.GREEN + Messages.SHOP_TITLE_SELL2.toString()));
			else if(i == 6)
				placeItem(i, GUI.createItem(Material.OAK_SIGN, ChatColor.RED + Messages.SHOP_TITLE_BUY.toString()));
			else if(i == 7)
				placeItem(i, GUI.createItem(Material.OAK_SIGN, ChatColor.RED + Messages.SHOP_TITLE_BUY2.toString()));
			else if(i == 10 || i == 11) { }
			else if(i == 13) {
				placeItem(i, GUI.createItem(Material.LIME_DYE, ChatColor.BOLD + Messages.SHOP_TITLE_CREATE.toString()), p -> {
					if(itemIn == null)
						itemIn = airItem;
					if(itemIn2 == null)
						itemIn2 = airItem;
					if(itemOut == null)
						itemOut = airItem;
					if(itemOut2 == null)
						itemOut2 = airItem;
					if(itemIn == airItem && itemIn2 == airItem && itemOut == airItem && itemOut2 == airItem)
						return;
					if((itemOut == airItem && itemOut2 == airItem) || (itemIn == airItem && itemIn2 == airItem))
						return;
					if(itemsDisabled) {
						for(String itemsList:disabledItemList) {
							Material disabledItemsList = Material.matchMaterial(itemsList);
							if(disabledItemsList != null) {
								ItemStack in1 = itemIn.clone();
								ItemStack in2 = itemIn2.clone();
								ItemStack out1 = itemOut.clone();
								ItemStack out2 = itemOut2.clone();
								if(in1.getType().equals(disabledItemsList) || in2.getType().equals(disabledItemsList) || out1.getType().equals(disabledItemsList) || out2.getType().equals(disabledItemsList))
									return;
								if(in1.getType().equals(Material.SHULKER_BOX) && in1.getItemMeta() instanceof BlockStateMeta) {
									BlockStateMeta itemMeta1 = (BlockStateMeta) in1.getItemMeta();
									ShulkerBox shulkerBox1 = (ShulkerBox) itemMeta1.getBlockState();
									if(shulkerBox1.getInventory().contains(disabledItemsList))
										return;
								}
								if(in2.getType().equals(Material.SHULKER_BOX) && in2.getItemMeta() instanceof BlockStateMeta) {
									BlockStateMeta itemMeta2 = (BlockStateMeta) in2.getItemMeta();
									ShulkerBox shulkerBox2 = (ShulkerBox) itemMeta2.getBlockState();
									if(shulkerBox2.getInventory().contains(disabledItemsList))
										return;
								}
								if(out1.getType().equals(Material.SHULKER_BOX) && out1.getItemMeta() instanceof BlockStateMeta) {
									BlockStateMeta itemMeta3 = (BlockStateMeta) out1.getItemMeta();
									ShulkerBox shulkerBox3 = (ShulkerBox) itemMeta3.getBlockState();
									if(shulkerBox3.getInventory().contains(disabledItemsList))
										return;
								}
								if(out2.getType().equals(Material.SHULKER_BOX) && out2.getItemMeta() instanceof BlockStateMeta) {
									BlockStateMeta itemMeta4 = (BlockStateMeta) out2.getItemMeta();
									ShulkerBox shulkerBox4 = (ShulkerBox) itemMeta4.getBlockState();
									if(shulkerBox4.getInventory().contains(disabledItemsList))
										return;
								}
							}
						}
					}
					shop.getRows()[index] = new RowStore(itemOut, itemOut2, itemIn, itemIn2, false);
					InvAdminShop inv = new InvAdminShop(shop, p.getPlayer());
					inv.open(p);
				});
			}
			else if(i == 15 || i == 16) { }
			else
				placeItem(i, GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, ""));
		}
	}
	
	public void onDrag(InventoryDragEvent event) {		
		super.onDrag(event);
		Inventory inv = event.getInventory();
		if(inv.getType().equals(InventoryType.CHEST) && event.getView().getTitle().contains(Messages.SHOP_TITLE_CREATESHOP.toString()))
			event.setCancelled(true);
	}
	
	@Override
	public void onClick(InventoryClickEvent event) {
		super.onClick(event);
		if(!event.getAction().equals(InventoryAction.PLACE_ALL) && !event.getAction().equals(InventoryAction.PICKUP_ALL))
			return;
		event.setCancelled(false);
		Inventory inv = event.getClickedInventory();
		if(inv.getType().equals(InventoryType.CHEST) && event.getView().getTitle().contains(Messages.SHOP_TITLE_CREATESHOP.toString())) {
			event.setCancelled(true);
			if(event.getRawSlot() == 10 || event.getRawSlot() == 11 || event.getRawSlot() == 15 || event.getRawSlot() == 16) {
				ItemStack item =  event.getCursor().clone();
				if(event.getClick().isRightClick())
					item.setAmount(1);
				placeItem(event.getRawSlot(), item);
				if(event.getRawSlot() == 10)
					itemOut = item;
				else if(event.getRawSlot() == 11)
					itemOut2 = item;
				else if(event.getRawSlot() == 15)
					itemIn = item;
				else if(event.getRawSlot() == 16)
					itemIn2 = item;
			}			
		}
	}
}
