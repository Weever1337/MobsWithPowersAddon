package net.weever.rotp_mwp.actions;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.init.ModParticles;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.world.World;
import net.weever.rotp_mwp.util.RainbowTextUtil;
import net.weever.rotp_mwp.util.TextureUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToggleSummon extends StandAction {
    public ToggleSummon(StandAction.Builder builder) {
        super(builder);
    }

    @Nullable
    private StandType targetStandType;

    @Override
    public ActionConditionResult checkRangeAndTarget(ActionTarget target, LivingEntity user, IStandPower power) {
        super.checkRangeAndTarget(target,user,power);
        Entity targetEntity = target.getEntity();
        if (targetEntity instanceof LivingEntity) {
            if (IStandPower.getStandPowerOptional((LivingEntity) targetEntity).map(mobPower -> mobPower.getType() != null).orElse(false)) {
                targetStandType = IStandPower.getStandPowerOptional((LivingEntity) targetEntity).map(IStandPower::getType).orElse(null);
                return ActionConditionResult.POSITIVE;
            }
            targetStandType = null;
            return ActionConditionResult.NEGATIVE;
        }
        targetStandType = null;
        return ActionConditionResult.NEGATIVE;
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (!world.isClientSide()){
            if (target.getEntity() instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) target.getEntity();
                IStandPower.getStandPowerOptional(livingEntity).ifPresent(IStandPower::toggleSummon);
            }
        }
    }

    @Override
    public IFormattableTextComponent getTranslatedName(IStandPower power, String key) {
        return RainbowTextUtil.getRainbowText("Toggle Summon", false);
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.ENTITY;
    }

    @Override
    public @NotNull ResourceLocation getIconTexture(@Nullable IStandPower power) {
        if (targetStandType != null) {
            ResourceLocation standTypeTexture = TextureUtil.getResourceLocation(targetStandType);
            if (standTypeTexture != null) {
                return standTypeTexture;
            }
        } else {
            ResourceLocation randomizedTexture = TextureUtil.getResourceLocation(ModParticles.METEORITE_VIRUS.get());
            if (randomizedTexture != null) {
                return randomizedTexture;
            }
        }
        return super.getIconTexture(power);
    }
}
