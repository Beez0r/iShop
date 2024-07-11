package com.minedhype.ishop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

public class TabComplete implements TabCompleter {

    private static final List<String> items = Arrays.stream(Material.values()).filter(Material::isItem).map(material -> material.getKey().getKey()).toList();
    private static final List<String> shopCommands = Arrays.asList("adminshop","copy","count","create","createshop","createlocation","delete","deleteid","deletelocation","find","findbook","list","listadmin","manage","managestock","move","out","reload","removeallshops","shops","sold","stock","view");
    public static final List<String> enchantments = Arrays.asList("aqua_affinity","bane_of_arthropods","binding_curse","blast_protection","breach","channeling","curse_of_binding","curse_of_vanishing","density","depth_strider","efficiency","feather_falling","fire_aspect","fire_protection","flame","fortune","frost_walker","impaling","infinity","knockback","looting","loyalty","luck_of_the_sea","lure","mending","multishot","piercing","power","projectile_protection","protection","punch","quick_charge","respiration","riptide","sharpness","silk_touch","smite","soul_speed","sweeping_edge","swift_sneak","thorns","unbreaking","vanishing_curse","wind_burst");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if(args[0].equalsIgnoreCase("find") || args[0].equalsIgnoreCase("count")) {
            if(args.length == 2)
                return StringUtil.copyPartialMatches(args[1].toLowerCase(), items, new ArrayList<>());
        return Collections.emptyList();
        }
        else if(args[0].equalsIgnoreCase("findbook")) {
            if(args.length == 2)
                return StringUtil.copyPartialMatches(args[1].toUpperCase(), enchantments, new ArrayList<>());
        return Collections.emptyList();
        }
        else {
            if(args.length < 2)
                return StringUtil.copyPartialMatches(args[0].toLowerCase(), shopCommands, new ArrayList<>());
            return Collections.emptyList();
        }
    }
}