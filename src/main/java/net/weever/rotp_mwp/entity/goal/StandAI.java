package net.weever.rotp_mwp.entity.goal;

import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.weever.rotp_mwp.Config;
import net.weever.rotp_mwp.MobsWithPowersAddon;
import net.weever.rotp_mwp.mechanics.combo.ComboManager;
import net.weever.rotp_mwp.mechanics.combo.ComboStep;
import net.weever.rotp_mwp.mechanics.combo.StandComboData;
import net.weever.rotp_mwp.util.AddonUtil;
import net.weever.rotp_mwp.util.CapabilityAdderForAll;

import java.util.*;
import java.util.stream.Collectors;

import static net.weever.rotp_mwp.util.AddonUtil.getActionTarget;
import static net.weever.rotp_mwp.util.AddonUtil.getStandRange;

public class StandAI extends Goal {
    private final MobEntity mobEntity;
    private final Map<String, Integer> cooldownMap = new HashMap<>();
    private final Random random = new Random();
    private int actionChangeTicks = 0;

    private String currentComboName = null;
    private int comboStep = 0;
    private int comboResetTicks = 0;
    private static final int MAX_COMBO_TICK_DELAY = 60;

    public StandAI(MobEntity mobEntity) {
        this.mobEntity = mobEntity;
    }

    @Override
    public boolean canUse() {
        return IStandPower.getStandPowerOptional(mobEntity).map(power -> power.getType() != null).orElse(false) && mobEntity.isAlive();
    }

    @Override
    public void tick() {
        updateCooldowns();
        IStandPower.getStandPowerOptional(mobEntity).ifPresent(this::runAI);
    }

    private void runAI(IStandPower power) {
        power.tick();
        power.postTick();

        if (comboResetTicks > 0) {
            comboResetTicks--;
        } else {
            resetCombo();
        }

        LivingEntity target = mobEntity.getTarget();
        if (target == null || !target.isAlive() || mobEntity.distanceToSqr(target) > getStandRange(power.getType(), mobEntity.level) * getStandRange(power.getType(), mobEntity.level)) {
            if (power.getHeldAction(true) != null) power.stopHeldAction(true);
            if (power.isActive()) power.toggleSummon();
            resetCombo();
            return;
        }

        actionChangeTicks++;
        if (actionChangeTicks % 20 != 0) return;

        power.refreshHeldActionTickState(true);
        if (!power.isActive()) power.toggleSummon();

        Map<String, StandAction> allActions = AddonUtil.getListOfUnlockedStandActions(power).stream()
                .collect(Collectors.toMap(a -> a.getRegistryName().toString(), a -> a, (a1, a2) -> a1));

        if (allActions.isEmpty()) return;

        String standId = power.getType().getRegistryName().toString();
        StandComboData comboData = ComboManager.getDataForStand(standId);

        if (comboData != null && comboData.getBlockAction() != null) {
            String blockActionName = comboData.getBlockAction();
            StandAction blockAction = allActions.get(blockActionName);

            boolean wasRecentlyHurt = mobEntity.getLastHurtByMob() != null && mobEntity.tickCount - mobEntity.getLastHurtByMobTimestamp() < 40;
            boolean shouldPreemptivelyBlock = random.nextFloat() < 0.15f;

            if (blockAction != null && (wasRecentlyHurt || shouldPreemptivelyBlock)) {
                if (isActionAvailable(power, blockAction, null, mobEntity.distanceTo(target))) {
                    performAction(power, blockAction);
                    return;
                }
            }
        }

        if (comboData == null || !Config.getCommonConfigInstance(mobEntity.level.isClientSide).useWIPComboSystem.get()) {
            performRandomAvailableAction(power, allActions, target);
        } else {
            performComboLogic(power, comboData, allActions, target);
        }
    }

    private void performComboLogic(IStandPower power, StandComboData comboData, Map<String, StandAction> allActions, LivingEntity target) {
        if (currentComboName != null) {
            tryToContinueCombo(power, comboData, allActions, target);
        } else {
            tryToStartNewCombo(power, comboData, allActions, target);
        }
    }

    private void tryToContinueCombo(IStandPower power, StandComboData comboData, Map<String, StandAction> allActions, LivingEntity target) {
        List<ComboStep> currentStepList = comboData.getComboSteps(currentComboName);
        if (currentStepList == null || comboStep >= currentStepList.size()) {
            resetCombo();
            return;
        }

        ComboStep nextStep = currentStepList.get(comboStep);
        StandAction action = allActions.get(nextStep.getAction());

        if (isActionAvailable(power, action, nextStep, mobEntity.distanceTo(target))) {
            performAction(power, action);
            comboStep++;
            comboResetTicks = MAX_COMBO_TICK_DELAY;
        } else {
            resetCombo();
        }
    }

    private void tryToStartNewCombo(IStandPower power, StandComboData comboData, Map<String, StandAction> allActions, LivingEntity target) {
        int resolve = power.getResolveLevel();
        List<String> availableComboNames = comboData.getAvailableCombos(resolve);

        if (availableComboNames.isEmpty()) {
            performRandomAvailableAction(power, allActions, target);
            return;
        }

        Collections.shuffle(availableComboNames);

        for (String comboName : availableComboNames) {
            List<ComboStep> stepList = comboData.getComboSteps(comboName);
            if (stepList != null && !stepList.isEmpty()) {
                ComboStep firstStep = stepList.get(0);
                StandAction firstAction = allActions.get(firstStep.getAction());
                if (isActionAvailable(power, firstAction, firstStep, mobEntity.distanceTo(target))) {
                    currentComboName = comboName;
                    comboStep = 0;
                    tryToContinueCombo(power, comboData, allActions, target);
                    return;
                }
            }
        }

        performRandomAvailableAction(power, allActions, target);
    }

    private void performRandomAvailableAction(IStandPower power, Map<String, StandAction> allActions, LivingEntity target) {
        List<StandAction> availableActions = allActions.values().stream()
                .filter(action -> isActionAvailable(power, action, null, mobEntity.distanceTo(target)))
                .collect(Collectors.toList());

        if (!availableActions.isEmpty()) {
            StandAction randomAction = availableActions.get(random.nextInt(availableActions.size()));
            performAction(power, randomAction);
        }
    }

    private void performAction(IStandPower power, StandAction action) {
        if (action.getHoldDurationToFire(power) > 0) {
            power.setHeldAction(action, getActionTarget(mobEntity));
        } else {
            power.clickAction(action, false, getActionTarget(mobEntity), null);
        }
        setCooldown(action.getRegistryName().toString(), action.getCooldownTechnical(power));
    }

    private boolean isActionAvailable(IStandPower power, StandAction action, ComboStep step, double distanceToTarget) {
        if (action == null) return false;

        boolean isAvailable = !isOnCooldown(action.getRegistryName().toString())
                && action.checkConditions(mobEntity, power, getActionTarget(mobEntity)).isPositive()
                && action.getStaminaCost(power) <= power.getStamina()
                && !CapabilityAdderForAll.isBlockedAction(action, mobEntity.level);

        if (!isAvailable) return false;

        if (step != null) {
            if (step.getRequiredStamina() != null && power.getStamina() < step.getRequiredStamina()) {
                return false;
            }
            return step.getDistance() == null || checkDistance(step.getDistance(), distanceToTarget);
        }

        return true;
    }

    private boolean checkDistance(String condition, double actualDistance) {
        if (condition == null || condition.isEmpty()) return true;

        try {
            if (condition.startsWith(">=")) {
                return actualDistance >= Double.parseDouble(condition.substring(2));
            }
            if (condition.startsWith("<=")) {
                return actualDistance <= Double.parseDouble(condition.substring(2));
            }
            if (condition.startsWith(">")) {
                return actualDistance > Double.parseDouble(condition.substring(1));
            }
            if (condition.startsWith("<")) {
                return actualDistance < Double.parseDouble(condition.substring(1));
            }
        } catch (NumberFormatException e) {
            MobsWithPowersAddon.getLogger().warn("Invalid distance format in combo JSON: {}", condition);
            return false;
        }
        return true;
    }

    private void resetCombo() {
        this.currentComboName = null;
        this.comboStep = 0;
        this.comboResetTicks = 0;
    }

    private boolean isOnCooldown(String actionId) {
        return cooldownMap.getOrDefault(actionId, 0) > 0;
    }

    private void setCooldown(String actionId, int cooldownTicks) {
        cooldownMap.put(actionId, cooldownTicks + 10);
    }

    private void updateCooldowns() {
        cooldownMap.forEach((action, cooldown) -> {
            if (cooldown > 0) {
                cooldownMap.put(action, cooldown - 1);
            }
        });
    }

    @Override
    public boolean canContinueToUse() {
        return mobEntity.getTarget() != null && mobEntity.isAlive();
    }

    @Override
    public void stop() {
        resetCombo();
    }
}