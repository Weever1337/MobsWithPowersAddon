package net.weever.rotp_mwp.mixins;

import com.github.standobyte.jojo.item.StandArrowItem;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.weever.rotp_mwp.util.AddonUtil;
import net.weever.rotp_mwp.util.CapabilityAdderForAll;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(StandArrowItem.class)
public class StandArrowMixin {
    @Inject(method = "onPiercedByArrow",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;hasEffect(Lnet/minecraft/potion/Effect;)Z",
                    ordinal = 0,
                    shift = At.Shift.AFTER),
            cancellable = true)
    private static void mws$onPiercedByArrow(Entity target, ItemStack stack, World world, Optional<Entity> arrowShooter, CallbackInfoReturnable<Boolean> cir) {
        if (target instanceof MobEntity && !CapabilityAdderForAll.isBlockedEntity(target)) {
            MobEntity mobEntity = (MobEntity) target;
            AtomicBoolean returnValue = new AtomicBoolean(false);
            IStandPower.getStandPowerOptional(mobEntity).ifPresent(power -> {
                if (power.hasPower()) {
                    returnValue.set(false);
                } else {
                    StandType<?> standToGive = AddonUtil.randomStand(mobEntity, mobEntity.getRandom());
                    if (standToGive == null) {
                        returnValue.set(false);
                    } else {
                        mobEntity.hurt(DamageUtil.STAND_VIRUS, Math.min(5, mobEntity.getHealth()));
                        returnValue.set(power.givePower(standToGive));
                    }
                }
            });

            cir.setReturnValue(returnValue.get());
        }
    }
}

