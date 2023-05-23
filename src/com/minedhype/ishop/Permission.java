package com.minedhype.ishop;

public enum Permission {
	SHOP_ADMIN("ishop.admin"),
	SHOP_BYPASS_FIND_CD("ishop.bypassfindcooldown"),
	SHOP_CREATE("ishop.create"),
	SHOP_FIND("ishop.find"),
	SHOP_LIMIT_BYPASS("ishop.create.limit.bypass"),
	SHOP_LIMIT_PREFIX("ishop.create.limit."),
	SHOP_LIST("ishop.list"),
	SHOP_MOVE("ishop.move"),
	SHOP_REMOTEMANAGE("ishop.remotemanage"),
	SHOP_REMOTESHOPPING("ishop.remoteshopping"),
	SHOP_SHOPS("ishop.shops"),
	SHOP_STOCK("ishop.stock"),
	SHOP_STOCK_PREFIX("ishop.pages.");

	private final String perm;
	Permission(String perms) {
		this.perm = perms;
	}

	@Override
	public String toString() {
		return perm;
	}
}
