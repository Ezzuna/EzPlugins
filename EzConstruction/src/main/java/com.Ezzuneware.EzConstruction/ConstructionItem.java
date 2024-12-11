package com.Ezzuneware.EzConstruction;

public enum ConstructionItem {

    LARDER("Larder spot", 15403, "arder");


    public final String name;
    public final int buildSpot;
    public final String wildcardToSearchFor;

    ConstructionItem(String name, int buildSpot, String wildcardToSearchFor) {
        this.name = name;
        this.buildSpot = buildSpot;
        this.wildcardToSearchFor = wildcardToSearchFor;
    }
}
