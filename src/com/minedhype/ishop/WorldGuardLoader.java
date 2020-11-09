package com.minedhype.ishop;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.block.Block;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

public class WorldGuardLoader {
	private StateFlag FLAG_TRADE = null;
	
	public WorldGuardLoader() {
		FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();	
		try {
			StateFlag flag = new StateFlag("ishop", false);
			registry.register(flag);
			FLAG_TRADE = flag;
		} catch(FlagConflictException e) { e.printStackTrace(); }
	}
	
	public boolean checkRegion(Block block) {
		AtomicBoolean isShopLoc = new AtomicBoolean(false);
		if(FLAG_TRADE != null && iShop.config.getBoolean("enableWorldGuardFlag")) {
			RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
			RegionManager regions = container.get(new BukkitWorld(block.getWorld()));
			if(regions != null) {
				regions.getRegions().forEach((id, region) -> {
					if(region.getId().equalsIgnoreCase(ProtectedRegion.GLOBAL_REGION) || region.contains(block.getX(), block.getY(), block.getZ())) {
						if((region.getFlag(iShop.wgLoader.getFlagTrade().get()) != null) && Objects.equals(region.getFlag(iShop.wgLoader.getFlagTrade().get()), StateFlag.State.ALLOW))
							isShopLoc.set(true);
					}
				});
			}
		} else
			isShopLoc.set(true);
		return isShopLoc.get();
	}
	public Optional<StateFlag> getFlagTrade() {
		return Optional.ofNullable(FLAG_TRADE);
	}
}
