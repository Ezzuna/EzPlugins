package com.Ezzuneware.EzEdgeSmelter;

import lombok.Getter;

public enum Product {
    BAR("bar", -1,-1,-1, -1),
    BRACELET("bracelet", 393235 , 393236, 393237, 11065);



    @Getter
    private String name;

    @Getter
    private Integer opalId;
    @Getter
    private Integer jadeId;
    @Getter
    private Integer topazId;
        @Getter
    private Integer mouldId;


    Product(String name, Integer opalId, Integer jadeId, Integer topazId, Integer mouldId) {
        this.name = name;
        this.opalId = opalId;
        this.jadeId = jadeId;
        this.topazId = topazId;
        this.mouldId = mouldId;
    }

    public Integer GetIdForType(Integer type){
        switch (type){
            case 0: return opalId;
            case 1: return jadeId;
            case 2: return topazId;

        }

        return -1;
    }

}
