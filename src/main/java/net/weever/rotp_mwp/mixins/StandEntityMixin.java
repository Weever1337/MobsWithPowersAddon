package net.weever.rotp_mwp.mixins;

import com.github.standobyte.jojo.client.render.entity.renderer.stand.StandEntityRenderer;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.ForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(StandEntity.class)
public abstract class StandEntityMixin {
    @Shadow @Nullable public abstract LivingEntity getUser();

    @Shadow public abstract boolean isBaby();

    @Unique
    private static final UUID BABY_ATTACK_DAMAGE_UUID = UUID.fromString("093864b1-a43a-459a-935d-627c78c630ed");
    @Unique
    private static final UUID BABY_ATTACK_SPEED_UUID = UUID.fromString("fa8875c2-a6a9-497a-9abd-564a58b5676b");
    @Unique
    private static final UUID BABY_REACH_DISTANCE_UUID = UUID.fromString("02438d49-4957-4501-a94b-21436baa9a14");

    @Inject(method = "isBaby", at = @At("HEAD"), cancellable = true)
    private void isBaby(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity user = getUser();
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
        if (isBaby()) {
            applyBabyAttributes((LivingEntity) (Object) this);
        }
    }

    @Unique
    private void applyBabyAttributes(LivingEntity entity) {
        applyModifier(entity, Attributes.ATTACK_DAMAGE, BABY_ATTACK_DAMAGE_UUID, -2.0, AttributeModifier.Operation.ADDITION);
        applyModifier(entity, Attributes.ATTACK_SPEED, BABY_ATTACK_SPEED_UUID, -2.5, AttributeModifier.Operation.ADDITION);
        applyModifier(entity, ForgeMod.REACH_DISTANCE.get(), BABY_REACH_DISTANCE_UUID, -2.0, AttributeModifier.Operation.ADDITION);
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
