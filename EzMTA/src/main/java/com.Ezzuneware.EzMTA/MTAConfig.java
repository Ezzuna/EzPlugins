/*
 * Copyright (c) 2018, Jasper Ketelaar <Jasper0781@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.Ezzuneware.EzMTA;

import com.Ezzuneware.EzMTA.enchantment.ENCHANTMENT_SPELLS;
import com.Ezzuneware.EzMTA.graveyard.foodType;
import net.runelite.client.config.*;

@ConfigGroup("mta")
public interface MTAConfig extends Config {
    @ConfigItem(
            keyName = "alchemy",
            name = "Enable alchemy room",
            description = "Configures whether or not the alchemy room overlay is enabled.",
            position = 0
    )
    default boolean alchemy() {
        return true;
    }

    @ConfigItem(
            keyName = "graveyard",
            name = "Enable graveyard room",
            description = "Configures whether or not the graveyard room overlay is enabled.",
            position = 1
    )
    default boolean graveyard() {
        return true;
    }

    @ConfigItem(
            keyName = "telekinetic",
            name = "Enable telekinetic room",
            description = "Configures whether or not the telekinetic room overlay is enabled.",
            position = 2
    )
    default boolean telekinetic() {
        return true;
    }

    @ConfigItem(
            keyName = "enchantment",
            name = "Enable enchantment room",
            description = "Configures whether or not the enchantment room overlay is enabled.",
            position = 3
    )
    default boolean enchantment() {
        return true;
    }

    @ConfigSection(
            name = "Game Tick Configuration",
            description = "Configure how to handles game tick delays, 1 game tick equates to roughly 600ms",
            position = 0,
            closedByDefault = true
    )
    String delayTickConfig = "delayTickConfig";

    @Range(
            max = 10
    )
    @ConfigItem(
            keyName = "tickDelayMin",
            name = "Game Tick Min",
            description = "",
            position = 2,
            section = delayTickConfig
    )
    default int tickDelayMin() {
        return 1;
    }

    @Range(
            max = 10
    )
    @ConfigItem(
            keyName = "tickDelayMax",
            name = "Game Tick Max",
            description = "",
            position = 3,
            section = delayTickConfig
    )
    default int tickDelayMax() {
        return 3;
    }

        @ConfigSection(
            name = "Graveyard",
            description = "",
            position = 3,
            closedByDefault = false
    )
    String GraveyardSection = "GraveyardSection";

    @ConfigItem(
            keyName = "graveyardFood",
            name = "Graveyard food",
            description = "Number of hitpoints to eat at.",
            section = GraveyardSection,
            position = 1
    )
    default foodType graveyardFood() {
        return foodType.Banana;
    }

        @ConfigItem(
            keyName = "graveyardEatAt",
            name = "Graveyard eat threshold",
            description = "Number of hitpoints to eat at.",
            section = GraveyardSection,
            position = 2
    )
    default int graveyardEatAt() {
        return 40;
    }

            @ConfigSection(
            name = "Enchanting",
            description = "",
            position = 4,
            closedByDefault = false
    )
    String EnchantingSection = "EnchantingSection";

        @ConfigItem(
            keyName = "enchantingSpell",
            name = "Enchant Spell",
            description = "",
            section = EnchantingSection,
            position = 1
    )
    default ENCHANTMENT_SPELLS enchantingSpell() {
        return ENCHANTMENT_SPELLS.SAPPHIRE;
    }
}
