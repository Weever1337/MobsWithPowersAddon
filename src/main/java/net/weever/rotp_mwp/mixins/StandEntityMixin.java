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
    private static final float DEFAULT_HEIGHT = 1.5f;

    @Inject(method = "isBaby", at = @At("HEAD"), cancellable = true)
    private void isBaby(CallbackInfoReturnable<Boolean> cir) {
        StandEntity standEntity = (StandEntity) (Object) this;
        LivingEntity user = standEntity.getUser();
        if (user != null) {
            float userHeight = (float) (user.getBoundingBox().maxY - user.getBoundingBox().minY);

            float okHeight = 1.5f;

            if (userHeight < okHeight || user.isBaby()) {
                cir.setReturnValue(true);
                return;
            }
        }
        cir.setReturnValue(false);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        StandEntity standEntity = (StandEntity) (Object) this;
        if (standEntity.isBaby()) {
            LivingEntity user = standEntity.getUser();
            if (user == null) {
                applyBabyAttributes(standEntity, 1);
                return;
            }
            float userHeight = (float) (user.getBoundingBox().maxY - user.getBoundingBox().minY);
            float scaleFactor = userHeight / DEFAULT_HEIGHT;

            applyBabyAttributes(standEntity, scaleFactor);
        }
    }

    @Inject(method = "defaultRotation", at = @At(value = "HEAD"), cancellable = true)
    private void defaultRotation(CallbackInfo ci) {
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

    @Unique
    private void applyBabyAttributes(LivingEntity entity, float scaleFactor) {
        applyModifier(entity, Attributes.ATTACK_DAMAGE, BABY_ATTACK_DAMAGE_UUID, -2.0 * scaleFactor, AttributeModifier.Operation.ADDITION);
        applyModifier(entity, Attributes.ATTACK_SPEED, BABY_ATTACK_SPEED_UUID, -2.5 * scaleFactor, AttributeModifier.Operation.ADDITION);
        applyModifier(entity, ForgeMod.REACH_DISTANCE.get(), BABY_REACH_DISTANCE_UUID, -2.0 * scaleFactor, AttributeModifier.Operation.ADDITION);
    }

    @Unique
    private void applyModifier(LivingEntity entity, Attribute attribute, UUID uuid, double value, AttributeModifier.Operation operation) {
        ModifiableAttributeInstance attributeInstance = entity.getAttribute(attribute);
        if (attributeInstance != null) {
            attributeInstance.removeModifier(uuid);
            attributeInstance.addTransientModifier(new AttributeModifier(uuid, "Baby attribute modifier xd", value, operation));
        }
    }
}
