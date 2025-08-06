package net.weever.rotp_mwp.mechanics.virus;

import com.github.standobyte.jojo.entity.mob.rps.RockPaperScissorsKidEntity;
import com.github.standobyte.jojo.item.StandArrowItem;
import com.github.standobyte.jojo.potion.StandVirusEffect;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.weever.rotp_mwp.Config;
import net.weever.rotp_mwp.util.AddonUtil;
import net.weever.rotp_mwp.util.CapabilityAdderForAll;

import static net.weever.rotp_mwp.util.AddonUtil.calculateFromPercentageToFloat;
import static net.weever.rotp_mwp.util.AddonUtil.getPercentageOfGettingStand;

public class GenericMobStandGiver extends StandVirusEffect.MobStandGiver {
    public GenericMobStandGiver() {
        super(() -> EntityType.PIG, () -> null);
        this.stands.clear();
    }

    @Override
    public boolean entityMatches(LivingEntity entity) {
        return entity instanceof MobEntity && !CapabilityAdderForAll.isBlockedEntity(entity);
    }

    @Override
    protected float getSurviveChance(float virusEffectLvl) {
        float baseChance = calculateFromPercentageToFloat(getPercentageOfGettingStand(false));
        return Math.max(0, baseChance - (0.15f * virusEffectLvl));
    }

    @Override
    public boolean giveStand(LivingEntity entity) {
        if (!(entity instanceof MobEntity) || IStandPower.getStandPowerOptional(entity).map(IStandPower::hasPower).orElse(true)) {
            return false;
        }

        MobEntity mobEntity = (MobEntity) entity;

        if (mobEntity instanceof VillagerEntity && mobEntity.isBaby() && Config.getCommonConfigInstance(false).spawnBoy2Man.get()) {
            RockPaperScissorsKidEntity.turnFromArrow(mobEntity);
            return true;
        }

        return IStandPower.getStandPowerOptional(mobEntity).map(power -> {
            StandType<?> standToGive = AddonUtil.randomStand(mobEntity, mobEntity.getRandom());
            if (standToGive != null) {
                return StandArrowItem.giveStandFromArrow(mobEntity, power, standToGive);
            }
            return false;
        }).orElse(false);
    }
}