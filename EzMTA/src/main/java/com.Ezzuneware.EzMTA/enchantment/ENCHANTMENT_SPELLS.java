package com.Ezzuneware.EzMTA.enchantment;

public enum ENCHANTMENT_SPELLS {
    SAPPHIRE("Sapphire",14286861 ),
    EMERALD("Emerald",14286872 ),
    RUBY("Ruby", 14286885),
    DIAMOND("Diamond",14286894 ),
    DRAGONSTONE("Dragonstone",14286909 ),
    ONYX("Onyx",14286922 );

    public final String name;
    public final Integer widgetId;

    ENCHANTMENT_SPELLS(String name, Integer widgetId) {
        this.name = name;
        this.widgetId = widgetId;
    }
}
