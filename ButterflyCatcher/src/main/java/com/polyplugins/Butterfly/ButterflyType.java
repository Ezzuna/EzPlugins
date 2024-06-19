package com.polyplugins.Butterfly;

import lombok.Getter;

@Getter
public enum ButterflyType {

    RUBY_HARVEST("Ruby harvest", ""),
    SAPPHIRE_GLACIALIS("Sapphire glacialis", ""),
    SNOWY_KNIGHT("Snowy knight", ""),
    BLACK_WARLOCK("Black warlock", ""),
    SUNLIGHT_MOTH("Sunlight Moth", "Sunlight moth"),
    MIDNIGHT_MOTH("Midnight Moth", "Midnight moth");

    private final String name;
    private final String alt_name;

    ButterflyType(String name, String altName) {
        this.name = name;
        this.alt_name = altName;
    }


}
