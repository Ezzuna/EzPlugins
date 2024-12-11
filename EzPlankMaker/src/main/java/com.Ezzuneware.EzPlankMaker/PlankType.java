package com.Ezzuneware.EzPlankMaker;

import net.runelite.api.ItemID;

public enum PlankType {

    WOODEN_PLANK(ItemID.LOGS, ItemID.PLANK),
    OAK_PLANK(ItemID.OAK_LOGS, ItemID.OAK_PLANK),
    TEAK_PLANK(ItemID.TEAK_LOGS, ItemID.TEAK_PLANK),
    MAHOG_PLANK(ItemID.MAHOGANY_LOGS, ItemID.MAHOGANY_PLANK);

    public final int logId;
    public final int plankId;

    PlankType(int logId, int plankId){
        this.logId = logId;
        this.plankId = plankId;
    }
}
