package com.piggyplugins;

import com.Ezzuneware.BJTest.BJTestPlugin;
import com.Ezzuneware.EzAshMiner.EzAshMinerPlugin;
import com.Ezzuneware.EzBankingUtility.EzBankingUtilityPlugin;
import com.Ezzuneware.EzBarbFish.EzBarbFishPlugin;
import com.Ezzuneware.EzEdgeSmelter.EzEdgeSmelterPlugin;
import com.Ezzuneware.EzFalconry.EzFalconryPlugin;
import com.Ezzuneware.EzGarbageCollector.EzGarbageCollectorPlugin;
import com.Ezzuneware.EzGemMineBot.EzGemMineBotPlugin;
import com.Ezzuneware.EzHerbi.HerbiAfkPlugin;
import com.Ezzuneware.EzHerbs.EzHerbsPlugin;
import com.Ezzuneware.EzMTA.EzMTAPlugin;
import com.Ezzuneware.EzMlmSlacker.EzMlmSlackerPlugin;
import com.Ezzuneware.EzMoonlightMothCollector.EzMoonlightMothCollectorPlugin;
import com.Ezzuneware.EzNaguaSlayer.EzNaguaSlayerPlugin;
import com.Ezzuneware.EzPitfallHunter.EzPitfallHunterPlugin;
import com.Ezzuneware.EzScurrius.EzScurriusPlugin;
import com.Ezzuneware.EzShopper.EzShopperPlugin;
import com.Ezzuneware.EzSplasher.EzSplasherPlugin;
import com.Ezzuneware.EzStall_stealer.EzStall_stealerPlugin;
import com.Ezzuneware.EzStarReminer.EzStarReminerPlugin;
import com.Ezzuneware.EzWintertodt.EzWintertodtPlugin;
import com.example.AutoTele.AutoTele;
import com.example.AutoTitheFarm.AutoTitheFarmPlugin;
import com.example.CalvarionHelper.CalvarionHelper;
import com.example.CluePuzzleSolver.PuzzleBoxSolver;
import com.example.E3t4g.et34g;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.LavaRunecrafter.LavaRunecrafterPlugin;
import com.example.NightmareHelper.NightmareHelperPlugin;
import com.example.PacketUtils.PacketUtilsPlugin;
import com.example.PathingTesting.PathingTesting;
import com.example.PrayerFlicker.EthanPrayerFlickerPlugin;
import com.example.RunEnabler.RunEnabler;
import com.example.UpkeepPlugin.UpkeepPlugin;
import com.example.dropparty.DropPartyPlugin;
import com.example.gauntletFlicker.gauntletFlicker;
import com.example.harpoon2ticker.SwordFish2Tick;
import com.example.superglass.SuperGlassMakerPlugin;
import com.piggyplugins.AutoAerial.AutoAerialPlugin;
import com.piggyplugins.AutoCombatv2.AutoCombatv2Plugin;
import com.piggyplugins.AutoJugHumidifier.AutoJugHumidifierPlugin;
import com.piggyplugins.AutoRifts.AutoRiftsPlugin;
import com.piggyplugins.AutoSmith.AutoSmith;
import com.piggyplugins.BobTheBaller.BobTheBallerPlugin;
import com.piggyplugins.BobTheBlower.BobTheBlowerPlugin;
import com.piggyplugins.BobTheBuilder.BobTheBuilderPlugin;
import com.piggyplugins.BobTheChef.BobTheChefPlugin;
import com.piggyplugins.BobTheCutter.BobTheCutterPlugin;
import com.piggyplugins.BobTheFarmer.BobTheFarmerPlugin;
import com.piggyplugins.BobTheHunter.BobTheHunterPlugin;
import com.piggyplugins.BobTheLazyNMZ.BobTheLazyNMZPlugin;
import com.piggyplugins.BobTheThief.BobTheThiefPlugin;
import com.piggyplugins.BobTheWizard.BobTheWizardPlugin;
import com.piggyplugins.CannonReloader.CannonReloaderPlugin;
import com.piggyplugins.Firemaking.FiremakingPlugin;
import com.piggyplugins.HerbCleaner.HerbCleanerPlugin;
import com.piggyplugins.ItemCombiner.ItemCombinerPlugin;
import com.piggyplugins.ItemDropper.ItemDropperPlugin;
import com.piggyplugins.JadAutoPrayers.JadAutoPrayersPlugin;
import com.piggyplugins.OneTickSwitcher.PvpHelperPlugin;
import com.piggyplugins.PiggyUtils.PiggyUtilsPlugin;
import com.piggyplugins.PowerSkiller.PowerSkillerPlugin;
import com.piggyplugins.PrayAgainstPlayer.PrayAgainstPlayerPlugin;
import com.piggyplugins.RooftopAgility.RooftopAgilityPlugin;
import com.piggyplugins.ShiftClickWalker.ShiftClickWalkerPlugin;
import com.piggyplugins.SixHourLog.SixHourLogPlugin;
import com.piggyplugins.SpeedDartMaker.SpeedDartMakerPlugin;
import com.piggyplugins.VardorvisHelper.VardorvisHelperPlugin;
import com.piggyplugins.strategyexample.StrategySmithPlugin;
import com.polyplugins.AutoBoner.AutoBonerPlugin;
import com.polyplugins.AutoRuneDragon.RuneDragonsPlugin;
import com.polyplugins.Butterfly.ButterflyPlugin;
import com.polyplugins.Chompy.AutoChompyPlugin;
import com.polyplugins.Dialogue.DialogueContinuerPlugin;
import com.polyplugins.KittenFeeder.KittenFeederPlugin;
import com.polyplugins.Trapper.AutoTrapperPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.ChinBreakHandler.ChinBreakHandlerPlugin;
import net.runelite.client.plugins.betterprofiles.BetterProfilesPlugin;

public class PluginTester {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(EthanApiPlugin.class, PacketUtilsPlugin.class,
                PiggyUtilsPlugin.class// Don't remove these
        /* Add your plugins in this method when running from the IDE.
           Make sure to include them as a dependency in the build.gradle via `testImplementation` */,
                StrategySmithPlugin.class, AutoBonerPlugin.class,
                AutoAerialPlugin.class,
                AutoChompyPlugin.class,
                AutoCombatv2Plugin.class,
                AutoJugHumidifierPlugin.class,
                AutoRiftsPlugin.class,
                RuneDragonsPlugin.class,
                AutoSmith.class,
                AutoTele.class,
                AutoTitheFarmPlugin.class,
                AutoTrapperPlugin.class,
                BetterProfilesPlugin.class,
                BJTestPlugin.class,
                BobTheBallerPlugin.class,
                BobTheBlowerPlugin.class,
                BobTheBuilderPlugin.class,
                BobTheChefPlugin.class,
                BobTheCutterPlugin.class,
                BobTheFarmerPlugin.class,
                BobTheHunterPlugin.class,
                BobTheLazyNMZPlugin.class,
                BobTheThiefPlugin.class,
                BobTheWizardPlugin.class,
                ButterflyPlugin.class,
                CalvarionHelper.class,
                CannonReloaderPlugin.class,
                ChinBreakHandlerPlugin.class,
                PuzzleBoxSolver.class,
                DialogueContinuerPlugin.class,
                DropPartyPlugin.class,
                et34g.class,
                EzAshMinerPlugin.class,
                EzBarbFishPlugin.class,
                EzEdgeSmelterPlugin.class,
                EzFalconryPlugin.class,
                EzGarbageCollectorPlugin.class,
                EzGemMineBotPlugin.class,
                HerbiAfkPlugin.class,
                EzHerbsPlugin.class,
                EzMlmSlackerPlugin.class,
                EzNaguaSlayerPlugin.class,
                EzShopperPlugin.class,
                EzSplasherPlugin.class,
                EzStall_stealerPlugin.class,
                EzStarReminerPlugin.class,
                EzWintertodtPlugin.class,
                FiremakingPlugin.class,
                gauntletFlicker.class,
                SwordFish2Tick.class,
                HerbCleanerPlugin.class,
                ItemCombinerPlugin.class,
                ItemDropperPlugin.class,
                JadAutoPrayersPlugin.class,
                KittenFeederPlugin.class,
                LavaRunecrafterPlugin.class,
                NightmareHelperPlugin.class,
                PvpHelperPlugin.class,
                PathingTesting.class,
                PiggyUtilsPlugin.class,
                PowerSkillerPlugin.class,
                PrayAgainstPlayerPlugin.class,
                EthanPrayerFlickerPlugin.class,
                RooftopAgilityPlugin.class,
                RunEnabler.class,
                ShiftClickWalkerPlugin.class,
                SixHourLogPlugin.class,
                SpeedDartMakerPlugin.class,
                StrategySmithPlugin.class,
                SuperGlassMakerPlugin.class,
                UpkeepPlugin.class,
                VardorvisHelperPlugin.class,
                EzBankingUtilityPlugin.class,
                EzPitfallHunterPlugin.class,
                EzMoonlightMothCollectorPlugin.class,
                EzMTAPlugin.class,
                EzScurriusPlugin.class

                );
        RuneLite.main(args);
    }
}