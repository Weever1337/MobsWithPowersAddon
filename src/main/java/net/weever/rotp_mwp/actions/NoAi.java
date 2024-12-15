package net.weever.rotp_mwp.actions;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.init.ModStatusEffects;
import com.github.standobyte.jojo.init.power.stand.ModStandsInit;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.item.Items;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.world.World;
import net.weever.rotp_mwp.util.RainbowTextUtil;
import net.weever.rotp_mwp.util.TextureUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NoAi extends StandAction {
    public NoAi(StandAction.Builder builder) {
        super(builder);
    }

    private boolean noAI;

    @Override
    public ActionConditionResult checkRangeAndTarget(ActionTarget target, LivingEntity user, IStandPower power) {
        super.checkRangeAndTarget(target,user,power);
        Entity targetEntity = target.getEntity();
        if (targetEntity instanceof MobEntity) {
            noAI = ((MobEntity) targetEntity).isNoAi();
            return ActionConditionResult.POSITIVE;
        }
        noAI = false;
        return ActionConditionResult.NEGATIVE;
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (!world.isClientSide()){
            if (target.getEntity() instanceof MobEntity) {
                ((MobEntity) target.getEntity()).setNoAi(!((MobEntity) target.getEntity()).isNoAi());
            }
        }
    }

    @Override
    public IFormattableTextComponent getTranslatedName(IStandPower power, String key) {
        return RainbowTextUtil.getRainbowText("No Ai", false);
    }

    @Override
    public boolean greenSelection(IStandPower power, ActionConditionResult conditionCheck) {
        return conditionCheck.isPositive() && noAI;
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.ENTITY;
    }

    @Override
    public @NotNull ResourceLocation getIconTexture(@Nullable IStandPower power) {
        ResourceLocation randomizedTexture = TextureUtil.getResourceLocation(Effects.BLINDNESS);
        if (randomizedTexture != null) {
            return randomizedTexture;
        }
        return super.getIconTexture(power);
    }
}
