package com.Ezzuneware.EzSlayerAssistant;

public enum teleItem {
    RING_OF_WEALTH("Ring of wealth","Rub", "Grand Exchange", 2),
    AMULET_OF_GLORY("Amulet of glory","Rub", "Edgeville", 2),
    RING_OF_DUELING("Ring of dueling","Rub", "Ferox enclave",3);

    public final String itemName;
    public final String action;
    public final String equipmentAction;
    public final Integer selection;

    teleItem(String itemName, String action, String equipmentAction, Integer selection){
        this.itemName = itemName;
        this.action = action;
        this.equipmentAction = equipmentAction;
        this.selection = selection;
    }
}
