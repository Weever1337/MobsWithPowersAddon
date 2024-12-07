package net.weever.rotp_mwp.actions;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.action.stand.StandEntityLightAttack;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.weever.rotp_mwp.util.AddonUtil;
import net.weever.rotp_mwp.util.CapabilityAdderForAll;
import net.weever.rotp_mwp.util.RainbowTextUtil;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static net.weever.rotp_mwp.util.AddonUtil.getActionTarget;

public class UseRandomAction extends StandAction {
    public UseRandomAction(StandAction.Builder builder) {
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

                IStandPower.getStandPowerOptional(livingEntity).ifPresent(standPower -> {
                    List<StandAction> actions = AddonUtil.getListOfUnlockedStandActions(standPower);
                    Random random = new Random();
                    if (!actions.isEmpty()) {
                        StandAction randomAction = actions.get(random.nextInt(actions.size()));
                        if (randomAction.getStaminaCost(standPower) <= standPower.getStamina()) {
                            if (randomAction.getHoldDurationToFire(standPower) > 0) {
                                standPower.setHeldAction(randomAction, getActionTarget(livingEntity));
                            } else {
                                standPower.clickAction(randomAction, false, getActionTarget(livingEntity), null);
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    public IFormattableTextComponent getTranslatedName(IStandPower power, String key) {
        return RainbowTextUtil.getRainbowText("Use Random Action", false);
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.ENTITY;
    }
}
