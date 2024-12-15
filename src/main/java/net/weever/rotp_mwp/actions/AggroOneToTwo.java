package net.weever.rotp_mwp.actions;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.world.World;
import net.weever.rotp_mwp.util.RainbowTextUtil;
import net.weever.rotp_mwp.util.TextureUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AggroOneToTwo extends StandAction {
    public AggroOneToTwo(StandAction.Builder builder) {
        super(builder);
    }

    public MobEntity firstTarget;

    @Override
    public ActionConditionResult checkRangeAndTarget(ActionTarget target, LivingEntity user, IStandPower power) {
        super.checkRangeAndTarget(target,user,power);
        Entity targetEntity = target.getEntity();
        if (targetEntity instanceof LivingEntity) {
            return ActionConditionResult.POSITIVE;
        }
        return ActionConditionResult.NEGATIVE;
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (!world.isClientSide()){
            if (target.getEntity() instanceof MobEntity) {
                if (firstTarget == null){
                    firstTarget = (MobEntity) target.getEntity();
                } else {
                    if (firstTarget.isAlive()) {
                        firstTarget.setTarget((LivingEntity) target.getEntity());
                    }
                    firstTarget = null;
                }
            }
        }
    }

    @Override
    public IFormattableTextComponent getTranslatedName(IStandPower power, String key) {
        if (firstTarget == null) {
            return RainbowTextUtil.getRainbowText("Aggro One To Two Entity", false);
        } else {
            return RainbowTextUtil.getRainbowText("Aggro " + firstTarget.getName().getString() + " To Two Entity", false);
        }
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.ENTITY;
    }

    @Override
    public @NotNull ResourceLocation getIconTexture(@Nullable IStandPower power) {
        ResourceLocation randomizedTexture = TextureUtil.getResourceLocation(Items.IRON_SWORD);
        if (randomizedTexture != null) {
            return randomizedTexture;
        }
        return super.getIconTexture(power);
    }
}
