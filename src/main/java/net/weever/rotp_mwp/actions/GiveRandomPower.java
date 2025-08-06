package net.weever.rotp_mwp.actions;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.init.ModItems;
import com.github.standobyte.jojo.item.StandDiscItem;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
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

public class GiveRandomPower extends StandAction {
    public GiveRandomPower(Builder builder) {
        super(builder);
    }

    @Override
    public ActionConditionResult checkRangeAndTarget(ActionTarget target, LivingEntity user, IStandPower power) {
        super.checkRangeAndTarget(target,user,power);
        Entity entity = target.getEntity();
        if (entity instanceof LivingEntity) {
            boolean hasPower = INonStandPower.getNonStandPowerOptional((LivingEntity) entity)
                    .map(mobPower -> mobPower.getType() != null)
                    .orElse(false);

            return hasPower ? ActionConditionResult.NEGATIVE : ActionConditionResult.POSITIVE;
        }
        return ActionConditionResult.NEGATIVE;
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (!world.isClientSide()){
            if (target.getEntity() instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) target.getEntity();
                Random random = new Random();
                INonStandPower.getNonStandPowerOptional(livingEntity).ifPresent(mobPower -> {
                    mobPower.givePower(AddonUtil.randomPower(livingEntity, random));
                });
            }
        }
    }

    @Override
    public IFormattableTextComponent getTranslatedName(IStandPower power, String key) {
        return RainbowTextUtil.getRainbowText("Give Random Power", false);
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.ENTITY;
    }

    @Override
    public @NotNull ResourceLocation getIconTexture(@Nullable IStandPower power) {
        ResourceLocation randomizedTexture = TextureUtil.getResourceLocation(ModItems.HAMON_MASTER_SPAWN_EGG.get());
        if (randomizedTexture != null) {
            return randomizedTexture;
        }
        return super.getIconTexture(power);
    }
}
