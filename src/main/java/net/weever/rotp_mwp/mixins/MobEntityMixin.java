package net.weever.rotp_mwp.mixins;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.world.World;
import net.weever.rotp_mwp.entity.goal.NonStandAI;
import net.weever.rotp_mwp.entity.goal.StandAI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void rotp_mwp$addCustomGoals(EntityType<? extends MobEntity> entityType, World world, CallbackInfo ci) {
        MobEntity mobEntity = (MobEntity) (Object) this;

        if (mobEntity.goalSelector != null && mobEntity.goalSelector.getRunningGoals().noneMatch(goal -> goal.getGoal() instanceof StandAI)) {
            mobEntity.goalSelector.addGoal(1, new StandAI(mobEntity));
        }

        if (mobEntity.goalSelector != null && mobEntity.goalSelector.getRunningGoals().noneMatch(goal -> goal.getGoal() instanceof NonStandAI)) {
            mobEntity.goalSelector.addGoal(1, new NonStandAI(mobEntity));
        }

        if (mobEntity instanceof CreatureEntity) {
            CreatureEntity creatureEntity = (CreatureEntity) mobEntity;
            if (creatureEntity.targetSelector != null && creatureEntity.targetSelector.getRunningGoals().noneMatch(goal -> goal.getGoal() instanceof HurtByTargetGoal)) {
                creatureEntity.targetSelector.addGoal(1, new HurtByTargetGoal(creatureEntity));
            }
        }
    }
}