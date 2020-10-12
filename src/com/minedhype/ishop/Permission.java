package com.minedhype.ishop;

public enum Permission {
	SHOP_ADMIN("ishop.admin"),
	SHOP_CREATE("ishop.create"),
	SHOP_LIMIT_BYPASS("ishop.create.limit.bypass"),
	SHOP_LIMIT_PREFIX("ishop.create.limit."),
	SHOP_STOCK("ishop.stock");

	private final String perm;
	Permission(String perms) {
		this.perm = perms;
	}

	@Override
	public String toString() {
		return perm;
	}
}
