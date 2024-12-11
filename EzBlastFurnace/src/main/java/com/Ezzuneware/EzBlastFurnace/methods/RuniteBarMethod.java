package com.Ezzuneware.EzBlastFurnace.methods;

import com.Ezzuneware.EzBlastFurnace.steps.MethodStep;
import com.Ezzuneware.EzBlastFurnace.utils.CoalPer;
import com.Ezzuneware.EzBlastFurnace.utils.Strings;
import net.runelite.api.ItemID;

public class RuniteBarMethod extends MetalBarMethod
{
    @Override
    protected MethodStep[] withdrawOre()
    {
        return withdrawRuniteOre;
    }

    @Override
    public int oreItem()
    {
        return ItemID.RUNITE_ORE;
    }

    @Override
    protected int barItem()
    {
        return ItemID.RUNITE_BAR;
    }

    @Override
    protected int coalPer()
    {
        return CoalPer.RUNITE.getValue();
    }

    @Override
    public String getName()
    {
        return Strings.RUNITE;
    }
}
