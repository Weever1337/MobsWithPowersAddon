package net.weever.rotp_mwp.entity.goal;

import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.action.stand.StandEntityLightAttack;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.weever.rotp_mwp.util.AddonUtil;
import net.weever.rotp_mwp.util.CapabilityAdderForAll;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static net.weever.rotp_mwp.util.AddonUtil.getActionTarget;
import static net.weever.rotp_mwp.util.AddonUtil.getStandRange;

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
        return IStandPower.getStandPowerOptional(mobEntity).map(power -> power.getType() != null).orElse(false) && mobEntity.isAlive();
    }

    @Override
    public void tick() {
        updateStand();
        updateCooldowns();
        IStandPower.getStandPowerOptional(mobEntity).ifPresent(power -> {
            actionChangeTicks++;
            if (mobEntity.getTarget() != null && mobEntity.distanceTo(mobEntity.getTarget()) <= getStandRange(power.getType(), mobEntity.level)) {
                power.refreshHeldActionTickState(true);
                if (actionChangeTicks % 30 == 0) {
                    List<StandAction> actions = AddonUtil.getListOfUnlockedStandActions(power);
                    if (!actions.isEmpty()) {
                        List<StandAction> availableActions = actions.stream()
                                .filter(action -> !isOnCooldown(action) && action.checkConditions(mobEntity, power, getActionTarget(mobEntity)).isPositive() && action.getStaminaCost(power) <= power.getStamina() && !CapabilityAdderForAll.isBlockedAction(action, mobEntity.level))
                                .collect(Collectors.toList());

                        if (!availableActions.isEmpty()) {
                            if (!power.isActive()) {
                                power.toggleSummon();
                            }
                            StandAction randomAction = availableActions.get(random.nextInt(availableActions.size()));
                            if (randomAction.getStaminaCost(power) <= power.getStamina()) {
                                if (randomAction instanceof StandEntityLightAttack) {
                                    for (int i = 0; i < 2; i++) {
                                        power.clickAction(randomAction, false, getActionTarget(mobEntity), null);
                                    }
                                }
                                if (randomAction.getHoldDurationToFire(power) > 0) {
                                    power.setHeldAction(randomAction, getActionTarget(mobEntity));
                                } else {
                                    power.clickAction(randomAction, false, getActionTarget(mobEntity), null);
                                }
                                setCooldown(randomAction, randomAction.getCooldownTechnical(power));
                            }
                        } else {
                            if (power.isActive()) {
                                power.toggleSummon();
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

    @Override
    public void start() {
        return;
    }

    @Override
    public void stop() {
        return;
    }
}
