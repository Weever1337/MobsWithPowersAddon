package net.weever.rotp_mwp.entity.goal;

import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.action.stand.StandEntityLightAttack;
import com.github.standobyte.jojo.init.power.stand.ModStandsInit;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.Util;
import net.weever.rotp_mwp.util.AddonUtil;

import java.util.*;
import java.util.stream.Collectors;

import static net.weever.rotp_mwp.util.AddonUtil.getActionTarget;

public class StandAI extends Goal {
    private final MobEntity mobEntity;
    private final Map<StandAction, Integer> cooldownMap = new HashMap<>();
    private final Random random = new Random();
    private int actionChangeTicks = 0;
    private final ArrayList<Action<?>> blockedActions = Util.make(new ArrayList<>(), list -> {
        list.add(ModStandsInit.STAR_PLATINUM_ZOOM.get());
        list.add(ModStandsInit.STAR_PLATINUM_INHALE.get());
        list.add(ModStandsInit.MAGICIANS_RED_DETECTOR.get());
    });

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
            if (mobEntity.getTarget() != null && mobEntity.distanceTo(mobEntity.getTarget()) <= 12.5f) {
                power.refreshHeldActionTickState(true);
                if (actionChangeTicks % 30 == 0) {
                    List<StandAction> actions = AddonUtil.getListOfUnlockedStandActions(power);
                    if (!actions.isEmpty()) {
                        List<StandAction> availableActions = actions.stream()
                                .filter(action -> !isOnCooldown(action) && action.checkConditions(mobEntity, power, getActionTarget(mobEntity)).isPositive() && action.getStaminaCost(power) <= power.getStamina() && !blockedActions.contains(action))
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
}
