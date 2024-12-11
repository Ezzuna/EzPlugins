package com.Ezzuneware.EzBlastFurnace.steps;

import lombok.Getter;

@Getter
public class ObjectStep extends MethodStep
{
    private final int objectId;

    public ObjectStep(String tooltip, int objectId)
    {
        super(tooltip);
        this.objectId = objectId;
    }
}
