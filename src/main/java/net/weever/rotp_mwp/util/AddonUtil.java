package net.weever.rotp_mwp.util;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.action.stand.TimeStop;
import com.github.standobyte.jojo.entity.mob.rps.RockPaperScissorsKidEntity;
import com.github.standobyte.jojo.init.power.JojoCustomRegistries;
import com.github.standobyte.jojo.power.impl.nonstand.type.NonStandPowerType;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.StandUtil;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import com.github.standobyte.jojo.util.general.MathUtil;
import com.github.standobyte.jojo.util.mod.JojoModUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.weever.rotp_mwp.Config;
import net.weever.rotp_mwp.MobsWithPowersAddon;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Mod.EventBusSubscriber(modid = MobsWithPowersAddon.MOD_ID)
public class AddonUtil {
    private static final String PROCESSED_TAG = MobsWithPowersAddon.MOD_ID + ":processed";

    private static boolean isGECreated(Entity entity) {
        CompoundNBT nbt = new CompoundNBT();
        entity.saveWithoutId(nbt);
        if (nbt.contains("DeathLootTable", 8)) {
            return nbt.getString("DeathLootTable").equals("empty");
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMobSpawn(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();

        if (entity.level.isClientSide() || !(entity instanceof MobEntity)
                || CapabilityAdderForAll.isBlockedEntity(entity)
                || entity.getPersistentData().getBoolean(PROCESSED_TAG)
                || isGECreated(entity)) {
            return;
        }

        MobEntity mobEntity = (MobEntity) entity;
        Random random = new Random();

        if (random.nextFloat() < calculateFromPercentageToFloat(getPercentageOfGettingStand(false))) {
            IStandPower.getStandPowerOptional(mobEntity).ifPresent(power -> {
                if (!power.hasPower()) {
                    if (mobEntity instanceof VillagerEntity && mobEntity.isBaby() && Config.getCommonConfigInstance(false).spawnBoy2Man.get()) {
                        RockPaperScissorsKidEntity.turnFromArrow(mobEntity);
                    } else {
                        power.givePower(randomStand(mobEntity, random));
                    }
                    power.setResolveLevel(random.nextInt(4));
                }
            });
        }
        entity.getPersistentData().putBoolean(PROCESSED_TAG, true);
    }

    public static List<StandAction> getListOfUnlockedStandActions(IStandPower power) {
        List<StandAction> actions = new ArrayList<>();
        Random random = power.getUser().level.random;

        for (StandAction action : power.getAllUnlockedActions()) {
            if (action instanceof TimeStop) {
                int maxTicks = ((TimeStop) action).getMaxTimeStopTicks(power);
                if (maxTicks > 0) {
                    int randomTicks = random.nextInt(maxTicks + 1);
                    power.setLearningProgressPoints(action, randomTicks);
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

    @Nullable
    public static NonStandPowerType<?> randomPower(LivingEntity entity, Random random) {
        if (!entity.level.isClientSide()) {
            Collection<NonStandPowerType<?>> powers = JojoCustomRegistries.NON_STAND_POWERS.getRegistry().getValues();

            if (powers.isEmpty()) {
                return null;
            }

            return getRandomElement(powers, random);
        }
        return null;
    }

    public static List<? extends String> getEntityList(boolean clientSide) {
        return Config.getCommonConfigInstance(clientSide).entityList.get();
    }

    public static List<? extends String> getBlockedStandsForMobsList(boolean clientSide) {
        return Config.getCommonConfigInstance(clientSide).blockedStandsForMobs.get();
    }

    public static List<? extends String> getBlockedStandActionsList(boolean clientSide) {
        return Config.getCommonConfigInstance(clientSide).blockedStandActionsForMobs.get();
    }

    public static List<? extends String> getLongRangeStandsList(boolean clientSide) {
        return Config.getCommonConfigInstance(clientSide).longRangeStands.get();
    }

    public static int getPercentageOfGettingStand(boolean clientSide) {
        return Config.getCommonConfigInstance(clientSide).percentageChanceToGettingAStandForMob.get();
    }

    public static float calculateFromPercentageToFloat(int percentage) {
        return percentage / 100.0f;
    }

    public static ActionTarget getActionTarget(LivingEntity livingEntity) {
        RayTraceResult rayTrace = JojoModUtil.rayTrace(livingEntity.getEyePosition(1.0F), livingEntity.getLookAngle(), 3,
                livingEntity.level, livingEntity, e -> !(e.is(livingEntity)), 0, 0);
        ActionTarget target = ActionTarget.fromRayTraceResult(rayTrace);
        target.resolveEntityId(livingEntity.level);
        return target;
    }

    public static float getStandRange(StandType<?> standType, World level) {
        if (standType == null) return 0;
        if (CapabilityAdderForAll.isLongRangeStand(standType, level)) {
            return 32;
        }
        return 12.5f;
    }

    public static <T> T getRandomElement(Iterable<T> items, Random random) {
        List<T> itemList = StreamSupport.stream(items.spliterator(), false)
                .collect(Collectors.toList());
        return itemList.get(random.nextInt(itemList.size()));
    }
}