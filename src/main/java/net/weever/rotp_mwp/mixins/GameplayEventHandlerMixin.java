package net.weever.rotp_mwp.mixins;

import com.github.standobyte.jojo.JojoModConfig;
import com.github.standobyte.jojo.init.ModItems;
import com.github.standobyte.jojo.item.StandDiscItem;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.GameplayEventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.weever.rotp_mwp.Config;
import net.weever.rotp_mwp.util.CapabilityAdderForAll;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameplayEventHandler.class)
public class GameplayEventHandlerMixin {
    @Inject(method = "addEntityDrops", at = @At("HEAD"), cancellable = true)
    private static void addEntityDrops(LivingDropsEvent event, CallbackInfo ci) {
        LivingEntity livingEntity = event.getEntityLiving();
        Config.Common config = Config.getCommonConfigInstance(livingEntity.level.isClientSide());
        JojoModConfig.Common jojoConfig = JojoModConfig.getCommonConfigInstance(livingEntity.level.isClientSide());
        if (!CapabilityAdderForAll.isBlockedEntity(livingEntity) && config.dropStandDiscFromMobs.get() && !jojoConfig.keepStandOnDeath.get()){
            IStandPower.getStandPowerOptional(livingEntity).ifPresent(power -> {
                if (power.hasPower()) {
                    ItemStack disc = StandDiscItem.withStand(new ItemStack(ModItems.STAND_DISC.get()), power.getStandInstance().get());
                    event.getDrops().add(new ItemEntity(livingEntity.level, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), disc));
                }
            });
            ci.cancel();
        }
    }
}
