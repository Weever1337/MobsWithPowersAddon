package net.weever.rotp_mwp.actions;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.init.ModItems;
import com.github.standobyte.jojo.item.StandDiscItem;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.StandInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.world.World;
import net.weever.rotp_mwp.util.AddonUtil;
import net.weever.rotp_mwp.util.RainbowTextUtil;
import net.weever.rotp_mwp.util.TextureUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class GiveRandomStand extends StandAction {
    public GiveRandomStand(StandAction.Builder builder) {
        super(builder);
    }

    @Nullable
    private StandInstance standInstanceFromDisc;

    @Override
    public ActionConditionResult checkRangeAndTarget(ActionTarget target, LivingEntity user, IStandPower power) {
        super.checkRangeAndTarget(target,user,power);
        Entity entity = target.getEntity();
        if (entity instanceof LivingEntity) {
            ItemStack mainHandItem = user.getMainHandItem();
            ItemStack offHandItem = user.getOffhandItem();

            if (mainHandItem.getItem() instanceof StandDiscItem) {
                standInstanceFromDisc = StandDiscItem.getStandFromStack(mainHandItem);
            } else if (offHandItem.getItem() instanceof StandDiscItem) {
                standInstanceFromDisc = StandDiscItem.getStandFromStack(offHandItem);
            } else {
                standInstanceFromDisc = null;
            }

            boolean hasStandPower = IStandPower.getStandPowerOptional((LivingEntity) entity)
                    .map(mobPower -> mobPower.getType() != null)
                    .orElse(false);

            return hasStandPower ? ActionConditionResult.NEGATIVE : ActionConditionResult.POSITIVE;
        }
        return ActionConditionResult.NEGATIVE;
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (!world.isClientSide()){
            if (target.getEntity() instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) target.getEntity();
                Random random = new Random();
                IStandPower.getStandPowerOptional(livingEntity).ifPresent(standPower -> {
                    if (standInstanceFromDisc != null) {
                        standPower.givePower(standInstanceFromDisc.getType());
                    } else {
                        standPower.givePower(AddonUtil.randomStand(livingEntity, random));
                    }
                    standPower.setResolveLevel(4);
                });
            }
        }
    }

    @Override
    public IFormattableTextComponent getTranslatedName(IStandPower power, String key) {
        if (standInstanceFromDisc != null) {
            return RainbowTextUtil.getRainbowText("Give " + standInstanceFromDisc.getName().getString(), false);
        }
        return RainbowTextUtil.getRainbowText("Give Random Stand", false);
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.ENTITY;
    }

    @Override
    public @NotNull ResourceLocation getIconTexture(@Nullable IStandPower power) {
        ResourceLocation randomizedTexture = TextureUtil.getResourceLocation(ModItems.STAND_ARROW.get());
        if (randomizedTexture != null) {
            return randomizedTexture;
        }
        return super.getIconTexture(power);
    }
}
