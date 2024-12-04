package net.weever.rotp_mwp.entity.goal;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.action.stand.StandEntityLightAttack;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.mod.JojoModUtil;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.RayTraceResult;
import net.weever.rotp_mwp.util.AddonUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class StandAI extends Goal {
    private final MobEntity mobEntity;
    private final Map<StandAction, Integer> cooldownMap = new HashMap<>();
    private final Random random = new Random();
    private int actionChangeTicks = 0;

    public StandAI(MobEntity mobEntity) {
        this.mobEntity = mobEntity;
    }

    @Override
    public boolean canUse() {
        return IStandPower.getStandPowerOptional(mobEntity).map(power -> power.getType() != null).orElse(false);
    }

    @Override
    public void tick() {
        updateStand();
        updateCooldowns();
        IStandPower.getStandPowerOptional(mobEntity).ifPresent(power -> {
            actionChangeTicks++;
            if (mobEntity.getTarget() != null && mobEntity.distanceTo(mobEntity.getTarget()) <= 12.5f && mobEntity.getTarget().isAlive()) {
                power.refreshHeldActionTickState(true);
                if (!power.isActive()) {
                    power.toggleSummon();
                }
                if (actionChangeTicks % 30 == 0) {
                    List<StandAction> actions = AddonUtil.getListOfUnlockedStandActions(power);
                    if (!actions.isEmpty()) {
                        List<StandAction> availableActions = actions.stream()
                                .filter(action -> !isOnCooldown(action))
                                .collect(Collectors.toList());

                        if (!availableActions.isEmpty()) {
                            StandAction randomAction = availableActions.get(random.nextInt(availableActions.size()));
                            if (randomAction.getStaminaCost(power) <= power.getStamina()) {
                                if (randomAction instanceof StandEntityLightAttack) {
                                    for (int i = 0; i < 5; i++) {
                                        power.clickAction(randomAction, false, getActionTarget(), null);
                                    }
                                }
                                if (randomAction.getHoldDurationToFire(power) > 0) {
                                    power.setHeldAction(randomAction, getActionTarget());
                                } else {
                                    power.clickAction(randomAction, false, getActionTarget(), null);
                                }
                                setCooldown(randomAction, randomAction.getCooldownTechnical(power));
                            }
                        }
                    }
                }
            } else {
                if (power.getHeldAction(true) != null) {
                    power.stopHeldAction(true);
                }
                if (power.isActive()) {
                    power.toggleSummon();
                }
            }
        });
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    private boolean isOnCooldown(StandAction action) {
        return cooldownMap.getOrDefault(action, 0) > 0;
    }

    private void setCooldown(StandAction action, int cooldownTicks) {
        cooldownMap.put(action, cooldownTicks);
    }

    private void updateStand() {
        IStandPower.getStandPowerOptional(mobEntity).ifPresent(stand -> {
            stand.tick();
            stand.postTick();
        });
    }

    private void updateCooldowns() {
        cooldownMap.forEach((action, cooldown) -> {
            if (cooldown > 0) {
                cooldownMap.put(action, cooldown - 1);
            }
        });
    }

    private ActionTarget getActionTarget() {
        RayTraceResult rayTrace = JojoModUtil.rayTrace(mobEntity.getEyePosition(1.0F), mobEntity.getLookAngle(), 3,
                mobEntity.level, mobEntity, e -> !(e.is(mobEntity)), 0, 0);
        ActionTarget target = ActionTarget.fromRayTraceResult(rayTrace);
        target.resolveEntityId(mobEntity.level);
        return target;
    }
}
