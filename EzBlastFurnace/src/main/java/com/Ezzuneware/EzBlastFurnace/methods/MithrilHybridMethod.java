package com.Ezzuneware.EzBlastFurnace.methods;

import com.Ezzuneware.EzBlastFurnace.steps.MethodStep;
import com.Ezzuneware.EzBlastFurnace.utils.CoalPer;
import com.Ezzuneware.EzBlastFurnace.utils.Strings;
import net.runelite.api.ItemID;

public class MithrilHybridMethod extends GoldHybridMethod
{
    @Override
    protected MethodStep[] withdrawOre()
    {
        return withdrawMithrilOre;
    }

    @Override
    public int oreItem()
    {
        return ItemID.MITHRIL_ORE;
    }

    @Override
    protected int barItem()
    {
        return ItemID.MITHRIL_BAR;
    }

    @Override
    protected int coalPer()
    {
        return CoalPer.MITHRIL.getValue();
    }

    @Override
    public String getName()
    {
        return Strings.MITHRILHYBRID;
    }
}
