package com.Ezzuneware.EzApi.Utility;

import com.Ezzuneware.EzApi.EzApi;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Inject;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

import java.util.*;
import java.util.regex.Pattern;

public class EzHelperClass {
    //
    public static List<String> CommaSeperatedStringToList(String css) {
        return Arrays.asList(css.split(","));
    }

    public static boolean ItemInCSS(String item, String css) {
        return CommaSeperatedStringToList(css).contains(item);
    }

    public static boolean ItemInCSSWildcard(String item, String css) {
        List<String> cssList = CommaSeperatedStringToList(css);
        for (String s : cssList) {
            if (WildcardMatcher.matches(s.toLowerCase(),item.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}

