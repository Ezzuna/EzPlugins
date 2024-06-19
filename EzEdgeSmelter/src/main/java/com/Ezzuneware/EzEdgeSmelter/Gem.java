package com.Ezzuneware.EzEdgeSmelter;

import lombok.Getter;

public enum Gem {
    NONE("none", -1, "none"),
    OPAL("Opal", 1609, "Silver bar"),
    JADE("Jade", 1611 , "Silver bar"),
    RedTopaz("Red topaz", 1613, "Silver bar");



    @Getter
    private String name;

    @Getter
    private Integer itemId;
    @Getter
    private String barCompanion;



    Gem(String name, Integer itemId, String barCompanion) {
        this.name = name;
        this.itemId = itemId;
        this.barCompanion = barCompanion;
    }


}
