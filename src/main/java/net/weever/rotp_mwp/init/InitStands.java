package net.weever.rotp_mwp.init;

import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.init.ModItems;
import com.github.standobyte.jojo.item.StandDiscItem;
import com.github.standobyte.jojo.power.impl.stand.StandUtil;
import com.github.standobyte.jojo.power.impl.stand.stats.StandStats;
import com.github.standobyte.jojo.power.impl.stand.type.NoManifestationStandType;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import com.github.standobyte.jojo.util.mod.StoryPart;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.weever.rotp_mwp.MobsWithPowersAddon;
import net.weever.rotp_mwp.actions.*;
import net.weever.rotp_mwp.power.impl.stand.type.DebugStandType;
import net.weever.rotp_mwp.util.RainbowTextUtil;

public class InitStands {
    @SuppressWarnings("unchecked")
    public static final DeferredRegister<Action<?>> ACTIONS = DeferredRegister.create(
            (Class<Action<?>>) ((Class<?>) Action.class), MobsWithPowersAddon.MOD_ID);
    @SuppressWarnings("unchecked")
    public static final DeferredRegister<StandType<?>> STANDS = DeferredRegister.create(
            (Class<StandType<?>>) ((Class<?>) StandType.class), MobsWithPowersAddon.MOD_ID);

    private static final ITextComponent DEBUG = RainbowTextUtil.getRainbowText("Debug Stand", false);

    public static final RegistryObject<GiveRandomStand> GIVE_RANDOM_STAND = ACTIONS.register("give_random_stand",
            () -> new GiveRandomStand(new GiveRandomStand.Builder()));

    public static final RegistryObject<RemoveStand> REMOVE_STAND = ACTIONS.register("remove_stand",
            () -> new RemoveStand(new RemoveStand.Builder().shiftVariationOf(GIVE_RANDOM_STAND)));

    public static final RegistryObject<NoAi> NO_AI = ACTIONS.register("no_ai",
            () -> new NoAi(new NoAi.Builder()));

    public static final RegistryObject<UseRandomAction> USE_RANDOM_ACTION = ACTIONS.register("use_random_action",
            () -> new UseRandomAction(new UseRandomAction.Builder()));

    public static final RegistryObject<ToggleSummon> TOGGLE_SUMMON = ACTIONS.register("toggle_summon",
            () -> new ToggleSummon(new ToggleSummon.Builder()));

    public static final RegistryObject<AggroOneToTwo> AGGRO_ONE_TO_TWO = ACTIONS.register("aggro_one_to_two",
            () -> new AggroOneToTwo(new AggroOneToTwo.Builder()));

    public static final RegistryObject<StandType<StandStats>> DEBUG_STAND = STANDS.register("debug_stand",
            () ->
                    new DebugStandType.Builder<>()
                            .color(0xFFFFFF)
                            .storyPartName(DEBUG)
                            .defaultStats(StandStats.class, new StandStats.Builder()
                                    .power(0)
                                    .speed(0)
                                    .range(0)
                                    .durability(0)
                                    .precision(0)
                            )
                            .leftClickHotbar(
                                    TOGGLE_SUMMON.get(),
                                    AGGRO_ONE_TO_TWO.get()
                            )
                            .rightClickHotbar(
                                    GIVE_RANDOM_STAND.get(),
                                    NO_AI.get(),
                                    USE_RANDOM_ACTION.get()
                            )
                            .setSurvivalGameplayPool(StandType.StandSurvivalGameplayPool.OTHER)
                            .build()
    );
}
