package com.Ezzuneware.EzBlastFurnace.steps;

import lombok.Getter;

@Getter
public class BankItemStep extends MethodStep
{
    private final int[] itemIds;

    public BankItemStep(String tooltip, int ...itemIds)
    {
        super(tooltip);
        this.itemIds = itemIds;
    }
}
