package net.weever.rotp_mwp.mixins;

import com.github.standobyte.jojo.potion.StandVirusEffect;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.weever.rotp_mwp.util.CapabilityAdderForAll;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StandVirusEffect.class)
public class StandVirusEffectMixin extends Effect {
    protected StandVirusEffectMixin(EffectType p_i50391_1_, int p_i50391_2_) {
        super(p_i50391_1_, p_i50391_2_);
    }

    @Inject(method = "applyEffectTick", at = @At("TAIL"))
    public void applyEffectTick(LivingEntity entity, int amplifier, CallbackInfo ci) {
        if (!entity.level.isClientSide() && !CapabilityAdderForAll.isBlockedEntity(entity)) {
            boolean stopEffect = entity.getHealth() <= 2.5f;
//            boolean hasEquipment = entity.hasItemInSlot(EquipmentSlotType.HEAD) || entity.hasItemInSlot(EquipmentSlotType.LEGS) || entity.hasItemInSlot(EquipmentSlotType.CHEST) || entity.hasItemInSlot(EquipmentSlotType.FEET) || entity.hasItemInSlot(EquipmentSlotType.MAINHAND) || entity.hasItemInSlot(EquipmentSlotType.OFFHAND)
            if (stopEffect) {
                entity.removeEffect(this);
            } else {
                float damage = 0.15F + (float) amplifier * 0.2F;
                damage *= Math.min(2f, entity.getHealth() - 0.001f);

                DamageUtil.hurtThroughInvulTicks(entity, DamageUtil.STAND_VIRUS, damage);
            }
        }
    }

    @Override
    public Effect getEffect() {
        return super.getEffect();
    }
}
