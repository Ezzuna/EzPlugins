package com.Ezzuneware.EzEdgeSmelter;

import com.example.PacketUtils.WidgetInfoExtended;
import lombok.Getter;

public enum SmeltingItem {

    SILVER_BAR(17694737);


    @Getter
    private final int widgetId;


    SmeltingItem(int widgetId) {
        this.widgetId = widgetId;
    }
}
