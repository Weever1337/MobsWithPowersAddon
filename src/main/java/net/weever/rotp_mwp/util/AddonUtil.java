package net.weever.rotp_mwp.util;

import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.action.stand.TimeStop;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.StandUtil;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import com.github.standobyte.jojo.util.general.MathUtil;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.weever.rotp_mwp.Config;
import net.weever.rotp_mwp.MobsWithPowersAddon;
import net.weever.rotp_mwp.entity.goal.StandAI;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = MobsWithPowersAddon.MOD_ID)
public class AddonUtil {
    @SubscribeEvent
    public static void onLivingJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (CapabilityAdderForAll.isBlockedEntity(entity)) return;
        if (!entity.level.isClientSide() && entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            if (livingEntity instanceof MobEntity) {
                MobEntity mobEntity = (MobEntity) livingEntity;
                if (mobEntity.getRandom().nextFloat() < calculateFromPercentageToFloat(getPercentageOfGettingStand(entity.level.isClientSide()))) {
                    IStandPower.getStandPowerOptional(mobEntity).ifPresent(power -> {
                        if (!power.hasPower()) {
                            power.givePower(randomStand(mobEntity, mobEntity.getRandom()));
                            power.setResolveLevel(livingEntity.getRandom().nextInt(4));
                        }
                    });
                }
                if (!mobEntity.goalSelector.getRunningGoals().anyMatch(goal -> goal.getGoal() instanceof StandAI)) {
                    mobEntity.goalSelector.addGoal(1, new StandAI(mobEntity));
                }
                if (livingEntity instanceof CreatureEntity) {
                    CreatureEntity creatureEntity = (CreatureEntity) livingEntity;
                    if (!creatureEntity.targetSelector.getRunningGoals().anyMatch(goal -> goal.getGoal() instanceof HurtByTargetGoal)) {
                        creatureEntity.targetSelector.addGoal(1, new HurtByTargetGoal(creatureEntity));
                    }
                }
            }
        }
    }

    public static List<StandAction> getListOfUnlockedStandActions(IStandPower power) {
        List<StandAction> actions = new ArrayList<>();
        for (StandAction action : power.getAllUnlockedActions()) {
            if (action instanceof TimeStop) {
                if (power.getLearningProgressPoints(action) < ((TimeStop) action).getMaxTimeStopTicks(power)) {
                    power.addLearningProgressPoints(action, ((TimeStop) action).getMaxTimeStopTicks(power));
                }
            }
            actions.add(action);
        }
        return actions;
    }

    @Nullable
    public static StandType<?> randomStand(LivingEntity entity, Random random) {
        if (!entity.level.isClientSide()) {
            List<StandType<?>> stands = StandUtil.arrowStands(entity.level.isClientSide()).collect(Collectors.toList());

            if (stands.isEmpty()) {
                return null;
            }

            if (!Config.getCommonConfigInstance(entity.level.isClientSide()).useAddonStands.get()) {
                stands.removeIf(stand ->
                        !Objects.requireNonNull(stand.getRegistryName()).toString().startsWith("jojo")
                );
            }

            List<? extends String> blockedStands = getBlockedStandsForMobsList(entity.level.isClientSide());
            if (!blockedStands.isEmpty()) {
                stands.removeIf(stand ->
                        blockedStands.contains(Objects.requireNonNull(stand.getRegistryName()).toString())
                );
            }

            return MathUtil.getRandomWeightedDouble(stands, stand -> stand.getStats().getRandomWeight(), random).orElse(null);
        }
        return null;
    }

    public static List<? extends String> getBlockedEntitiesList(boolean clientSide) {
        return Config.getCommonConfigInstance(clientSide).blockedEntities.get();
    }

    public static List<? extends String> getBlockedStandsForMobsList(boolean clientSide) {
        return Config.getCommonConfigInstance(clientSide).blockedStandsForMobs.get();
    }

    public static int getPercentageOfGettingStand(boolean clientSide){
        return Config.getCommonConfigInstance(clientSide).percentageChanceToGettingAStandForMob.get();
    }

    public static float calculateFromPercentageToFloat(int percentage){
        return percentage / 100.0f; // wtf are you doing man
    }
}
