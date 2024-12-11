package com.Ezzuneware.EzBlastFurnace.methods;

import com.Ezzuneware.EzBlastFurnace.steps.MethodStep;
import com.Ezzuneware.EzBlastFurnace.utils.CoalPer;
import com.Ezzuneware.EzBlastFurnace.utils.Strings;
import net.runelite.api.ItemID;

public class SteelBarMethod extends MetalBarMethod
{
    @Override
    protected MethodStep[] withdrawOre()
    {
        return withdrawIronOre;
    }

    @Override
    public int oreItem()
    {
        return ItemID.IRON_ORE;
    }

    @Override
    protected int barItem()
    {
        return ItemID.STEEL_BAR;
    }

    @Override
    protected int coalPer()
    {
        return CoalPer.IRON.getValue();
    }

    @Override
    public String getName()
    {
        return Strings.STEEL;
    }
}
