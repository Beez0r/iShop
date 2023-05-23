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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if(args[0].equalsIgnoreCase("find") || args[0].equalsIgnoreCase("count")) {
            if(args.length == 2)
                return StringUtil.copyPartialMatches(args[1].toLowerCase(), items, new ArrayList<>());
        return Collections.emptyList();
        }
        return null;
    }
}
