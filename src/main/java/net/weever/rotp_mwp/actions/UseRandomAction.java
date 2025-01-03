package net.weever.rotp_mwp.actions;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.init.power.stand.ModStandsInit;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.world.World;
import net.weever.rotp_mwp.util.AddonUtil;
import net.weever.rotp_mwp.util.RainbowTextUtil;
import net.weever.rotp_mwp.util.TextureUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

import static net.weever.rotp_mwp.util.AddonUtil.getActionTarget;

public class UseRandomAction extends StandAction {
    public UseRandomAction(StandAction.Builder builder) {
        super(builder);
    }

    @Override
    public ActionConditionResult checkRangeAndTarget(ActionTarget target, LivingEntity user, IStandPower power) {
        super.checkRangeAndTarget(target,user,power);
        Entity targetEntity = target.getEntity();
        if (targetEntity instanceof LivingEntity) {
            if (IStandPower.getStandPowerOptional((LivingEntity) targetEntity).map(mobPower -> mobPower.getType() != null).orElse(false)) {
                return ActionConditionResult.POSITIVE;
            }
        }
        return ActionConditionResult.NEGATIVE;
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (!world.isClientSide()){
            if (target.getEntity() instanceof LivingEntity) {
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

    @Override
    public @NotNull ResourceLocation getIconTexture(@Nullable IStandPower power) {
        ResourceLocation randomizedTexture = TextureUtil.getRandomActionTexture();
        if (randomizedTexture != null) {
            return randomizedTexture;
        }
        return super.getIconTexture(power);
    }
}
