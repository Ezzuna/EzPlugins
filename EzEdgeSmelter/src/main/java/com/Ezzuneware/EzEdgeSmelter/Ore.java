package com.Ezzuneware.EzEdgeSmelter;

import lombok.Getter;

public enum Ore {
    SILVER("Silver ore", "Silver bar", 17694737);



    @Getter
    private String name;
        @Getter
    private String product;
    @Getter
    private Integer id;

    Ore(String name, String product, Integer id) {
        this.name = name;
        this.product = product;
        this.id = id;
    }

}
