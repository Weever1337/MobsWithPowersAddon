package net.weever.rotp_mwp.mixins;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraftforge.common.ForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(StandEntity.class)
public abstract class StandEntityMixin {
    @Unique
    private static final UUID BABY_ATTACK_DAMAGE_UUID = UUID.fromString("093864b1-a43a-459a-935d-627c78c630ed");
    @Unique
    private static final UUID BABY_ATTACK_SPEED_UUID = UUID.fromString("fa8875c2-a6a9-497a-9abd-564a58b5676b");
    @Unique
    private static final UUID BABY_REACH_DISTANCE_UUID = UUID.fromString("02438d49-4957-4501-a94b-21436baa9a14");

    @Unique
    private static final String MODIFIER_NAME = "Baby Stand debuff";
    @Unique
    private static final double DAMAGE_MODIFIER = -2.0;
    @Unique
    private static final double SPEED_MODIFIER = -2.5;
    @Unique
    private static final double REACH_MODIFIER = -2.0;

    @Unique
    private static final float BABY_HEIGHT_THRESHOLD = 1.5f;

    @Unique
    private boolean rotp_mwp$isBabyAttributeApplied = false;

    @Inject(method = "isBaby", at = @At("HEAD"), cancellable = true)
    private void rotp_mwp$isBaby(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity user = ((StandEntity) (Object) this).getUser();
        if (user == null) {
            cir.setReturnValue(false);
            return;
        }

        if (user.isBaby() || user.getBbHeight() < BABY_HEIGHT_THRESHOLD) {
            cir.setReturnValue(true);
            return;
        }

        cir.setReturnValue(false);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void rotp_mwp$tick(CallbackInfo ci) {
        StandEntity standEntity = (StandEntity) (Object) this;
        boolean shouldBeBaby = standEntity.isBaby();

        if (shouldBeBaby && !this.rotp_mwp$isBabyAttributeApplied) {
            float scaleFactor = 1.0f;
            if (standEntity.getUser() != null) {
                scaleFactor = Math.max(0.1f, standEntity.getUser().getBbHeight() / BABY_HEIGHT_THRESHOLD);
            }
            applyBabyAttributes(standEntity, scaleFactor);
            this.rotp_mwp$isBabyAttributeApplied = true;
        } else if (!shouldBeBaby && this.rotp_mwp$isBabyAttributeApplied) {
            removeBabyAttributes(standEntity);
            this.rotp_mwp$isBabyAttributeApplied = false;
        }
    }

    @Inject(method = "defaultRotation", at = @At("HEAD"), cancellable = true)
    private void rotp_mwp$defaultRotation(CallbackInfo ci) {
        StandEntity standEntity = (StandEntity) (Object) this;
        LivingEntity user = standEntity.getUser();
        if (user != null && !standEntity.isManuallyControlled() && !standEntity.isRemotePositionFixed()) {
            if (user instanceof MobEntity) {
                MobEntity mobUser = (MobEntity) user;
                LivingEntity target = mobUser.getTarget();

                if (target != null) {
                    double deltaX = target.getX() - standEntity.getX();
                    double deltaY = target.getEyeY() - standEntity.getEyeY();
                    double deltaZ = target.getZ() - standEntity.getZ();

                    float yRot = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F);
                    float xRot = (float) -Math.toDegrees(Math.atan2(deltaY, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)));

                    standEntity.setRot(yRot, xRot);
                }
            } else {
                standEntity.setRot(user.yRot, user.xRot);
            }
        }

        standEntity.setYHeadRot(standEntity.yRot);
        ci.cancel();
    }

//    @Inject(method = "getOffsetFromUser", at = @At("RETURN"), cancellable = true)
//    private void rotp_mwp$adjustOffsetForTargeting(CallbackInfoReturnable<StandRelativeOffset> cir) {
//        StandEntity standEntity = (StandEntity) (Object) this;
//        if (standEntity.level.isClientSide() || !standEntity.isFollowingUser()) {
//            return;
//        }
//        StandRelativeOffset originalOffset = cir.getReturnValue();
//        if (originalOffset == null) {
//            return;
//        }
//        LivingEntity user = standEntity.getUser();
//        if (user instanceof MobEntity) {
//            LivingEntity target = ((MobEntity) user).getTarget();
//            if (target != null) {
//                double standReach = standEntity.getAttributeValue(ForgeMod.REACH_DISTANCE.get());
//                double distanceToTarget = standEntity.distanceTo(target);
//                if (distanceToTarget > standReach - 0.5) {
//                    double requiredAdjustment = distanceToTarget - (standReach - 1.0);
//                    StandRelativeOffset newOffset = StandRelativeOffset.withYOffset(
//                            originalOffset.getLeft(),
//                            originalOffset.y,
//                            originalOffset.getForward() + requiredAdjustment
//                    );
//                    cir.setReturnValue(newOffset);
//                }
//            }
//        }
//    }

    @Unique
    private void applyBabyAttributes(LivingEntity entity, float scaleFactor) {
        applyModifier(entity, Attributes.ATTACK_DAMAGE, BABY_ATTACK_DAMAGE_UUID, DAMAGE_MODIFIER * scaleFactor, AttributeModifier.Operation.ADDITION);
        applyModifier(entity, Attributes.ATTACK_SPEED, BABY_ATTACK_SPEED_UUID, SPEED_MODIFIER * scaleFactor, AttributeModifier.Operation.ADDITION);
        applyModifier(entity, ForgeMod.REACH_DISTANCE.get(), BABY_REACH_DISTANCE_UUID, REACH_MODIFIER * scaleFactor, AttributeModifier.Operation.ADDITION);
    }

    @Unique
    private void removeBabyAttributes(LivingEntity entity) {
        removeModifier(entity, Attributes.ATTACK_DAMAGE, BABY_ATTACK_DAMAGE_UUID);
        removeModifier(entity, Attributes.ATTACK_SPEED, BABY_ATTACK_SPEED_UUID);
        removeModifier(entity, ForgeMod.REACH_DISTANCE.get(), BABY_REACH_DISTANCE_UUID);
    }

    @Unique
    private void applyModifier(LivingEntity entity, Attribute attribute, UUID uuid, double value, AttributeModifier.Operation operation) {
        ModifiableAttributeInstance attributeInstance = entity.getAttribute(attribute);
        if (attributeInstance != null) {
            attributeInstance.removeModifier(uuid);
            attributeInstance.addTransientModifier(new AttributeModifier(uuid, MODIFIER_NAME, value, operation));
        }
    }

    @Unique
    private void removeModifier(LivingEntity entity, Attribute attribute, UUID uuid) {
        ModifiableAttributeInstance attributeInstance = entity.getAttribute(attribute);
        if (attributeInstance != null) {
            attributeInstance.removeModifier(uuid);
        }
    }
}