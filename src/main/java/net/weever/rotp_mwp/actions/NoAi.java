package net.weever.rotp_mwp.actions;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.world.World;
import net.weever.rotp_mwp.util.CapabilityAdderForAll;
import net.weever.rotp_mwp.util.RainbowTextUtil;

public class NoAi extends StandAction {
    public NoAi(StandAction.Builder builder) {
        super(builder);
    }

    @Override
    public ActionConditionResult checkTarget(ActionTarget target, LivingEntity user, IStandPower power) {
        Entity targetEntity = target.getEntity();
        if (targetEntity instanceof LivingEntity) {
            if (CapabilityAdderForAll.isBlockedEntity(targetEntity)) {
                return ActionConditionResult.NEGATIVE;
            }
            return ActionConditionResult.POSITIVE;
        }
        return ActionConditionResult.NEGATIVE;
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (!world.isClientSide()){
            if (target.getEntity() instanceof LivingEntity && !CapabilityAdderForAll.isBlockedEntity(target.getEntity())) {
                LivingEntity livingEntity = (LivingEntity) target.getEntity();
                if (livingEntity instanceof MobEntity){
                    ((MobEntity) livingEntity).setNoAi(!((MobEntity) livingEntity).isNoAi());
                }
            }
        }
    }

    @Override
    public IFormattableTextComponent getTranslatedName(IStandPower power, String key) {
        return RainbowTextUtil.getRainbowText("No Ai", false);
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.ENTITY;
    }
}
