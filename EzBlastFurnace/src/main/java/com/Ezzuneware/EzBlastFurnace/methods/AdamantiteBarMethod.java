package com.Ezzuneware.EzBlastFurnace.methods;

import com.Ezzuneware.EzBlastFurnace.steps.MethodStep;
import com.Ezzuneware.EzBlastFurnace.utils.CoalPer;
import com.Ezzuneware.EzBlastFurnace.utils.Strings;
import net.runelite.api.ItemID;

public class AdamantiteBarMethod extends MetalBarMethod
{
    @Override
    protected MethodStep[] withdrawOre()
    {
        return withdrawAdamantiteOre;
    }

    @Override
    public int oreItem()
    {
        return ItemID.ADAMANTITE_ORE;
    }

    @Override
    protected int barItem()
    {
        return ItemID.ADAMANTITE_BAR;
    }

    @Override
    protected int coalPer()
    {
        return CoalPer.ADAMANTITE.getValue();
    }

    @Override
    public String getName()
    {
        return Strings.ADAMANTITE;
    }
}
