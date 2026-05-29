package com.minedhype.ishop.inventories;

import com.minedhype.ishop.iShop;
import com.minedhype.ishop.StockShop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import com.minedhype.ishop.Messages;
import com.minedhype.ishop.gui.GUI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class InvStockList extends GUI {

    private int pag;
    private final ItemStack airItem = new ItemStack(Material.AIR, 0);
    private static final ArrayList<ItemStack> stocklist = new ArrayList<>();
    private static final ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD,1);
    private InvStockList(String stockTitle) { super(54, stockTitle); }
    public static InvStockList setStockTitle(String stockTitle) { return new InvStockList(stockTitle); }
    public void setPag(int pag) { this.pag = pag; }

    @Override
    public void onClick(InventoryClickEvent event) {
        super.onClick(event);
        if(event.getRawSlot() >= 45 && event.getRawSlot() < 54)
            return;
        if(!event.getAction().equals(InventoryAction.PLACE_ALL) && !event.getAction().equals(InventoryAction.PICKUP_ALL))
            return;
        if(event.getCurrentItem() == null || event.getCurrentItem().getType().isAir())
            return;
        if(event.isLeftClick() && Objects.requireNonNull(event.getCurrentItem().getItemMeta()).hasLore() && event.getCurrentItem().getType().equals(Material.PLAYER_HEAD)) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            player.closeInventory();
            player.performCommand("shop managestock " + event.getCurrentItem().getItemMeta().getDisplayName());
        }
    }

    private void PlayerStockList() {
        int index = pag * 45;
        for(int i = 0; i < 45; i++) {
            if(index <= stocklist.size()-1) {
                placeItem(i, stocklist.get(index));
                index++;
            } else
                placeItem(i, airItem);
        }
        int stockListPages = (int)Math.ceil(stocklist.size()-1)/44;
        for(int i=45; i<54; i++) {
            if(i == 47 && pag > 0)
                placeItem(i, GUI.createItem(Material.ARROW, Messages.SHOP_PAGE + " " + (pag)), p -> openPage(p, pag-1));
            else if(i == 51 && pag < stockListPages)
                placeItem(i, GUI.createItem(Material.ARROW, Messages.SHOP_PAGE + " " + (pag+2)), p -> openPage(p, pag+1));
            else
                placeItem(i, GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, ""));
        }
    }

    private static void getStockList() {
        for(UUID uuid : StockShop.stockList.keySet()) {
            if(uuid != null) {
                ItemStack item = new ItemStack(playerHead);
                SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                boolean playerNotFound = false;
                try {
                    skullMeta.setOwningPlayer(offlinePlayer);
                } catch (Exception e) {
                    playerNotFound = true;
                }
                if(offlinePlayer.getName() != null && !playerNotFound)
                    skullMeta.setDisplayName(offlinePlayer.getName());
                List<String> skullLore = new ArrayList<>();
                skullLore.add(uuid.toString());
                skullMeta.setLore(skullLore);
                item.setItemMeta(skullMeta);
                stocklist.add(item);
            }
        }
    }

    private void openPage(Player player, int pag) {
        for(int i=45; i<54; i++)
            placeItem(i, new ItemStack(airItem));
        player.closeInventory();
        this.pag = pag;
        this.open(player);
    }

    @Override
    public void open(Player player) {
        stocklist.clear();
        final Player openPlayer = player;
        Bukkit.getScheduler().runTaskAsynchronously(iShop.getPlugin(), () -> {
            getStockList();
            Bukkit.getScheduler().runTask(iShop.getPlugin(), () -> {
                PlayerStockList();
                super.open(openPlayer);
            });
        });
    }

    @Override
    public void onDrag(InventoryDragEvent event) {
        super.onDrag(event);
        event.setCancelled(true);
    }
}