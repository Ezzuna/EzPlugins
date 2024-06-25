package com.Ezzuneware.EzApi.Combat;

import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.RuneLite;

public class EzPlayerManagement {

    static Client client = RuneLite.getInjector().getInstance(Client.class);

    public static boolean attempt_logout() {
        Widget logoutButton = client.getWidget(182, 8);
        logoutButton = logoutButton != null ? logoutButton : client.getWidget(69, 25);
        if (logoutButton == null) return false;
        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetAction(logoutButton, "Logout");
        return true;
    }

    public static boolean IsInCombat() {
        return client.getLocalPlayer().isInteracting() &&
                client.getLocalPlayer().getInteracting() != null;
    }
}
