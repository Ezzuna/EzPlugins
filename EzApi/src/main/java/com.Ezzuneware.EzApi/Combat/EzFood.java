package com.Ezzuneware.EzApi.Combat;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public enum EzFood {

    COOKED_KARAMBWAN("Cooked karamwan", 16),
    LOBSTER("Lobster", 12),
    TROUT("Trout", 7);

    private static final Set<EzFood> ALL = EnumSet.allOf(EzFood.class);

    public String foodName;
    public Integer healAmount;

    EzFood(String foodName, Integer healAmount) {
        this.foodName = foodName;
        this.healAmount = healAmount;
    }

    public static EzFood byVal(String val) {

        return EzFood.stream()
                .filter(x -> Objects.equals(x.foodName, val))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("illegal value"));

    }

    public static Stream<EzFood> stream() {
        return Stream.of(values());
    }

}
