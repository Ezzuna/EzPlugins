package com.Ezzuneware.EzApi.Looting;

import com.Ezzuneware.EzApi.Combat.EzInventory;
import com.Ezzuneware.EzApi.EzApi;
import com.Ezzuneware.EzApi.Utility.EzHelperClass;
import com.example.EthanApiPlugin.Collections.ETileItem;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.TileItems;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.TileItem;
import net.runelite.api.widgets.Widget;
import net.runelite.client.RuneLite;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;

import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Scanner;
import java.util.regex.Pattern;

import static com.example.PacketUtils.PacketReflection.client;

@Slf4j
public class ezLooter {
    @Inject
    EzApi ezApi;
    static ItemManager itemManager = RuneLite.getInjector().getInstance(ItemManager.class);

    public static boolean GetLootIsInLootList(ItemStack item, String css) {

        Scanner sc = new Scanner(itemManager.getItemComposition(item.getId()).getName());

        for (String word : EzHelperClass.CommaSeperatedStringToList(css)) {
            Pattern filerRegx = Pattern.compile(word);

            Optional<String> result = sc.findAll(filerRegx)
                    .map(mr -> mr.group(1))
                    .filter(Objects::nonNull)
                    .findFirst();

            if (result.isPresent())
                return true;
        }
        return false;
    }


    public static boolean LootNearestItemInQueue(Queue<ItemStack> lootQueue, boolean lootingBagPresent, String alchCss, String foodCss) {
        boolean returned = false;
        if (!lootQueue.isEmpty()) {
            ItemStack itemStack = lootQueue.peek();
//            WorldPoint stackLocation = WorldPoint.fromLocal(client, itemStack.getLocation);
            Optional<ETileItem> dropItem = TileItems.search().withId(itemStack.getId()).withinDistanceToPoint(6, client.getLocalPlayer().getWorldLocation()).first();

            if (dropItem.isPresent()) {
                TileItem tile = dropItem.get().getTileItem();
                ItemComposition comp = itemManager.getItemComposition(itemStack.getId());
                log.info("Looting: " + comp.getName());
                if (comp.isStackable() || comp.getNote() != -1) {
                    log.info("stackable loot " + comp.getName());
                    returned = true;
                    if (lootingBagPresent && Objects.equals(comp.getName(), "Coins")) {
                        EzInventory.AlterLootingBag(false);
                    } else if (lootingBagPresent && !comp.getName().contains("ashes"))
                        LootInCSSWithLootingBagManip(comp.getName(), alchCss, foodCss);
                    dropItem.get().interact(false);


                }
                if (!Inventory.full()) {
                    if (lootingBagPresent && Objects.equals(comp.getName(), "Coins")) {
                        EzInventory.AlterLootingBag(false);
                    } else if (lootingBagPresent && !comp.getName().contains("ashes"))
                        LootInCSSWithLootingBagManip(comp.getName(), alchCss, foodCss);
                    dropItem.get().interact(false);
                    returned = true;

                } else {
                    EthanApiPlugin.sendClientMessage("Inventory full, stopping. May handle in future update");
                }
            }
            lootQueue.remove();
            return returned;
        }
        return false;

    }

    public static boolean LootInCSSWithLootingBagManip(String itemName, String alchCss, String foodCss) {

        Optional<Widget> item = Inventory.search().matchesWildCardNoCase(itemName).first();
        if (item.isPresent()) {
            if (EzHelperClass.ItemInCSSWildcard(itemName, alchCss)) {
                EzInventory.AlterLootingBag(true);
            } else if (EzHelperClass.ItemInCSSWildcard(itemName, foodCss)) {
                EzInventory.AlterLootingBag(true);
            } else {
                EzInventory.AlterLootingBag(false);
            }
            return true;
        }

        return false;
    }


}

