package com.minedhype.ishop;

import org.bukkit.ChatColor;

public enum Messages {
	ADMIN_SHOP_DISABLED("adminShopDisabled"),
	ADMIN_SHOP_NUMBER("adminShopNumber"),
	DISABLED_SHOP_BLOCK("disabledShopBlock"),
	EXISTING_SHOP("existingShop"),
	NOT_A_PLAYER("notPlayer"),
	NO_PERMISSION("noPermissions"),
	NO_PLAYER_FOUND("noPlayerFound"),
	NO_PLAYER_SHOP("noPlayerShop"),
	NO_SHOP_BLOCK("noShopBlock"),
	NO_SHOP_STOCK("noShopStock"),
	NO_STOCK_BLOCK("noStockBlock"),
	PLAYER_INV_FULL("playerInventoryFull"),
	PLAYER_SHOP_CREATED("playerShopCreated"),
	SHOP_BUSY("shopBusy"),
	SHOP_CLICK_MANAGE("clickManage"),
	SHOP_CLICK_SHOP("clickShop"),
	SHOP_CREATED("shopCreated"),
	SHOP_CREATE_NO_MONEY("noMoney"),
	SHOP_DELETED("shopDeleted"),
	SHOP_FAR("outOfStockRange"),
	SHOP_FOUND("foundShops"),
	SHOP_IDDELETED("shopIDDeleted"),
	SHOP_ID_INTEGER("shopIntegerError"),
	SHOP_LIST_ADMINSHOPS("listAdminShop"),
	SHOP_LIST_ALL("shopListTitle"),
	SHOP_LIST_DISABLED("shopListDisabled"),
	SHOP_LIST_OUT("listOutOfStock"),
	SHOP_LOCATION("location"),
	SHOP_MAX("shopLimit"),
	SHOP_NOT_FOUND("noShopFound"),
	SHOP_NOT_OUT("notOutOfStock"),
	SHOP_NO_ADMINSHOPS_FOUND("noAdminShopsFound"),
	SHOP_NO_ITEMS("noItems"),
	SHOP_NO_REMOTE("noRemoteShops"),
	SHOP_NO_SELF("shopNotOwned"),
	SHOP_NO_STOCK("noStock"),
	SHOP_NO_STOCK_BUTTON("noStockButton"),
	SHOP_NO_STOCK_SHELF("noStockNotify"),
	SHOP_NUMBER("shopNumber"),
	SHOP_OUT("outOfStock"),
	SHOP_PAGE("page"),
	SHOP_PURCHASE("buy"),
	SHOP_RELOAD("reload"),
	SHOP_REMOTE("noRemoteManage"),
	SHOP_SELL("sell"),
	SHOP_TITLE_ADMIN_SHOP("adminShop"),
	SHOP_TITLE_BROADCAST_OFF("broadcastOff"),
	SHOP_TITLE_BROADCAST_ON("broadcastOn"),
	SHOP_TITLE_BUY("buyTitle"),
	SHOP_TITLE_BUY2("buyTitle2"),
	SHOP_TITLE_BUYACTION("buyAction"),
	SHOP_TITLE_CREATE("createTitle"),
	SHOP_TITLE_CREATESHOP("createShopTitle"),
	SHOP_TITLE_DELETE("deleteTitle"),
	SHOP_TITLE_NORMAL_SHOP("normalShop"),
	SHOP_TITLE_SELL("sellTitle"),
	SHOP_TITLE_SELL2("sellTitle2"),
	SHOP_TITLE_STOCK("stockTitle"),
	SHOP_WORLD_DISABLED("disabledWorld"),
	SOLD_CLEAR("soldClear"),
	SOLD_COMMAND_DISABLED("soldCommandDisabled"),
	SOLD_HEADER("soldHeader"),
	SOLD_INTEGER_ERROR("soldIntegerError"),
	SOLD_JOIN_NOTIFY("soldJoinNotify"),
	SOLD_NOTHING("soldNothing"),
	SOLD_PAGES("soldPages"),
	SOLD_PAGES_NEXT("soldPagesNext"),
	SOLD_PAGES_PREVIOUS("soldPagesPrevious"),
	STOCK_COMMAND_DISABLED("stockCommandDisabled"),
	STOCK_INTEGER("stockIntegerError"),
	TARGET_MISMATCH("targetMismatch");

	private final String msg;

	Messages(String msg) {
		this.msg = msg;
	}

	@Override
	public String toString() {
		String translate = iShop.config.getString(msg);
		if(translate == null)
			translate = "&cError retrieving config message!";
		return ChatColor.translateAlternateColorCodes('&', translate);
	}
}
