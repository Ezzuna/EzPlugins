package com.Ezzuneware.EzApi.Combat;

import com.Ezzuneware.EzApi.EzApi;
import com.Ezzuneware.EzApi.Utility.EzHelperClass;
import com.example.EthanApiPlugin.Collections.Equipment;
import com.example.EthanApiPlugin.Collections.EquipmentItemWidget;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;


import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public class EzInventory {

    @Inject
    EzApi ezApi;
    @Inject
    private Client client;
    @Inject
    private ItemManager itemManager;

    public static boolean EatFirstFoodInCSS(String css) {
        for (String foodName : EzHelperClass.CommaSeperatedStringToList(css)) {
            Optional<Widget> food = Inventory.search().matchesWildCardNoCase(foodName).first();
            if (food.isPresent()) {
                EatFood(food.get());
                return true;
            }
        }
        return false;
    }

    public static boolean AlchFirstItemInCSS(String css) {
        for (String itemName : EzHelperClass.CommaSeperatedStringToList(css)) {
            Optional<Widget> item = Inventory.search().matchesWildCardNoCase(itemName).first();
            if (item.isPresent()) {
                AlchItem(item.get());
                return true;
            }
        }
        return false;
    }



    public static boolean AlterLootingBag(boolean open) {
        Optional<Widget> closedBag = Inventory.search().withId(11941).first();
        if (closedBag.isPresent()) {
            InventoryInteraction.useItem(closedBag.get(), "Open");
            return true;
        }
        Optional<Widget> openBag = Inventory.search().withId(22586).first();
        if (openBag.isPresent()) {
            InventoryInteraction.useItem(openBag.get(), "Close");
            return true;
        }

        return false;
    }

    public static void AlchItem(Widget item) {
        Optional<Widget> alchSpell = Widgets.search().withId(14286892).first();
        if (alchSpell.isEmpty())
            return;
        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetOnWidget(alchSpell.get(), item);
    }


    public static void EatFood(String foodName) {
        Optional<Widget> food = Inventory.search().matchesWildCardNoCase(foodName).first();
        if (food.isPresent()) {
            MousePackets.queueClickPacket();
            InventoryInteraction.useItem(food.get(), "Eat");
        }
    }

    public static void EatFood(Widget food) {
        MousePackets.queueClickPacket();
        InventoryInteraction.useItem(food, "Eat");
    }

    public static Boolean CheckForTele(EzTeleItem tItem) {
        Optional<EquipmentItemWidget> equipedItem = Equipment.search().matchesWildCardNoCase(tItem.itemName + "*").first();
        Optional<Widget> inventoryItem = Inventory.search().matchesWildCardNoCase(tItem.itemName + "*").first();
        return (equipedItem.isPresent() || inventoryItem.isPresent());
    }


    public static Boolean DoTeleFromTeleItem(EzTeleItem tItem) {
        Optional<EquipmentItemWidget> equipedItem = Equipment.search().matchesWildCardNoCase(tItem.itemName + "*").first();
        Optional<Widget> inventoryItem = Inventory.search().matchesWildCardNoCase(tItem.itemName + "*").first();
        boolean teleported = false;

        if (equipedItem.isPresent()) {
            teleported = true;
            MousePackets.queueClickPacket();
            InventoryInteraction.useItem(equipedItem.get(), tItem.equipmentAction);
        } else if (inventoryItem.isPresent()) {
            teleported = true;
            InventoryInteraction.useItem(inventoryItem.get(), tItem.action);
            MousePackets.queueClickPacket();
            WidgetPackets.queueResumePause(14352385, tItem.selection);
        }
        return teleported;
    }


    public static Integer GetFoodCount(String foodString) {
        int foodCount = 0;
        for (String foodName : EzHelperClass.CommaSeperatedStringToList(foodString)) {
            List<Widget> food = Inventory.search().matchesWildCardNoCase(foodName).result();
            foodCount += food.size();
        }
        return foodCount;
    }

    public static Integer GetFoodHealthValue(String foodString) {
        int foodCount = 0;
        for (String foodName : EzHelperClass.CommaSeperatedStringToList(foodString)) {
            List<Widget> food = Inventory.search().matchesWildCardNoCase(foodName).result();
            for (Widget fud : food) {
                System.out.println(fud.getName());  //might need to strip characters
                EzFood foodi = EzFood.byVal(fud.getName());
                foodCount += foodi.healAmount;
            }
        }
        return foodCount;
    }
}
