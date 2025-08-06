package net.weever.rotp_mwp.entity.goal;

import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.init.power.non_stand.hamon.ModHamonActions;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.weever.rotp_mwp.mechanics.combo.ComboManager;
import net.weever.rotp_mwp.mechanics.combo.ComboStep;
import net.weever.rotp_mwp.mechanics.combo.MaintenanceStep;
import net.weever.rotp_mwp.mechanics.combo.NonStandComboData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static net.weever.rotp_mwp.util.AddonUtil.getActionTarget;

public class NonStandAI extends Goal {
    private final MobEntity mobEntity;
    private final Map<String, Integer> cooldownMap = new HashMap<>();
    private final Random random = new Random();
    private int actionChangeTicks = 0;

    private String currentComboName = null;
    private int comboStep = 0;
    private int comboResetTicks = 0;
    private static final int MAX_COMBO_TICK_DELAY = 60;
    private static final Logger LOGGER = LogManager.getLogger();

    public NonStandAI(MobEntity mobEntity) {
        this.mobEntity = mobEntity;
    }

    @Override
    public boolean canUse() {
        return INonStandPower.getNonStandPowerOptional(mobEntity).map(power -> power.getType() != null).orElse(false) && mobEntity.isAlive();
    }

    @Override
    public void tick() {
        updateCooldowns();
        INonStandPower.getNonStandPowerOptional(mobEntity).ifPresent(this::runAI);
    }

    private void runAI(INonStandPower power) {
        if (power == null || power.getType() == null) return;

        power.tick();
        power.postTick();

        if (comboResetTicks > 0) comboResetTicks--;
        else resetCombo();

        LivingEntity target = mobEntity.getTarget();
        if (target == null || !target.isAlive()) {
            if (power.getHeldAction(true) != null) power.stopHeldAction(true);
            resetCombo();
            return;
        }

        actionChangeTicks++;
        if (actionChangeTicks % 20 != 0) return;
        power.refreshHeldActionTickState(true);

        Map<String, Action<INonStandPower>> allActions = power.getType().getUnlockedDefaultActions(power).stream()
                .collect(Collectors.toMap(a -> a.getRegistryName().toString(), a -> a, (a1, a2) -> a1));
        if (allActions.isEmpty()) return;

        String powerId = power.getType().getRegistryName().toString();
        NonStandComboData comboData = ComboManager.getDataForNonStand(powerId);

        if (comboData == null) {
            performRandomAvailableAction(power, allActions, target);
        } else {
            if (tryPerformMaintenance(power, comboData, allActions, target)) {
                return;
            }
            performComboLogic(power, comboData, allActions, target);
        }
    }

    private boolean tryPerformMaintenance(INonStandPower power, NonStandComboData data, Map<String, Action<INonStandPower>> allActions, LivingEntity target) {
        MaintenanceStep maintenance = data.getMaintenance();
        if (maintenance == null || maintenance.getAction() == null || maintenance.getCondition() == null) {
            return false;
        }

        boolean conditionMet = checkMaintenanceCondition(power, maintenance.getCondition());
        if (conditionMet) {
            Action<INonStandPower> action = allActions.get(maintenance.getAction());
            if (isActionAvailable(power, action, null, mobEntity.distanceTo(target))) {
                performAction(power, action);
                return true;
            }
        }
        return false;
    }

    private boolean checkMaintenanceCondition(INonStandPower power, String condition) {
        try {
            String[] parts = condition.replace(" ", "").split("<|>");
            String resourceType = parts[0];
            double percentage = Double.parseDouble(parts[1].replace("%", ""));

            float current, max;

            if (resourceType.equalsIgnoreCase("energy")) {
                Optional<HamonData> hamonOp = power.getTypeSpecificData(ModPowers.HAMON.get());
                if (!hamonOp.isPresent()) return false;
                HamonData hamon = hamonOp.get();
                current = hamon.getEnergyRatio();
                max = hamon.getMaxEnergy();
            } else if (resourceType.equalsIgnoreCase("blood")) {
                current = power.getEnergy();
                max = power.getMaxEnergy();
            } else {
                return false;
            }

            double currentPercentage = (current / max) * 100.0;
            return condition.contains("<") ? currentPercentage < percentage : currentPercentage > percentage;

        } catch (Exception e) {
            LOGGER.warn("Invalid maintenance condition format in non-stand combo JSON: " + condition, e);
            return false;
        }
    }

    private void performComboLogic(INonStandPower power, NonStandComboData comboData, Map<String, Action<INonStandPower>> allActions, LivingEntity target) {
        if (currentComboName != null) {
            tryToContinueCombo(power, comboData, allActions, target);
        } else {
            tryToStartNewCombo(power, comboData, allActions, target);
        }
    }

    private void tryToContinueCombo(INonStandPower power, NonStandComboData comboData, Map<String, Action<INonStandPower>> allActions, LivingEntity target) {
        List<ComboStep> currentStepList = comboData.getComboSteps(currentComboName);
        if (currentStepList == null || comboStep >= currentStepList.size()) {
            resetCombo();
            return;
        }

        ComboStep nextStep = currentStepList.get(comboStep);
        Action<INonStandPower> action = allActions.get(nextStep.getAction());

        if (isActionAvailable(power, action, nextStep, mobEntity.distanceTo(target))) {
            performAction(power, action);
            comboStep++;
            comboResetTicks = MAX_COMBO_TICK_DELAY;
        } else {
            resetCombo();
        }
    }

    private void tryToStartNewCombo(INonStandPower power, NonStandComboData comboData, Map<String, Action<INonStandPower>> allActions, LivingEntity target) {
        List<String> availableComboNames = new ArrayList<>(comboData.getComboNames());
        Collections.shuffle(availableComboNames);

        for (String comboName : availableComboNames) {
            List<ComboStep> stepList = comboData.getComboSteps(comboName);
            if (stepList != null && !stepList.isEmpty()) {
                ComboStep firstStep = stepList.get(0);
                Action<INonStandPower> firstAction = allActions.get(firstStep.getAction());
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

    private void performRandomAvailableAction(INonStandPower power, Map<String, Action<INonStandPower>> allActions, LivingEntity target) {
        List<Action<INonStandPower>> availableActions = allActions.values().stream()
                .filter(action -> isActionAvailable(power, action, null, mobEntity.distanceTo(target)))
                .collect(Collectors.toList());

        if (!availableActions.isEmpty()) {
            Action<INonStandPower> randomAction = availableActions.get(random.nextInt(availableActions.size()));
            performAction(power, randomAction);
        }
    }

    private void performAction(INonStandPower power, Action<INonStandPower> action) {
        if (action.getHoldDurationToFire(power) > 0) {
            power.setHeldAction(action, getActionTarget(mobEntity));
        } else {
            power.clickAction(action, false, getActionTarget(mobEntity), null);
        }
        setCooldown(action.getRegistryName().toString(), action.getCooldownTechnical(power));
    }

    private boolean isActionAvailable(INonStandPower power, Action<INonStandPower> action, ComboStep step, double distanceToTarget) {
        if (action == null) return false;

        boolean isAvailable = !isOnCooldown(action.getRegistryName().toString())
                && action.checkConditions(mobEntity, power, getActionTarget(mobEntity)).isPositive();

        if (!isAvailable) return false;

        if (step != null) {
            if (step.getRequiredStamina() != null && power.getType() == ModHamonActions.HAMON.get()) {
                if (power.getEnergy() < step.getRequiredStamina()) return false;
            }
            if (step.getDistance() != null && !checkDistance(step.getDistance(), distanceToTarget)) {
                return false;
            }
        }

        return true;
    }

    private boolean checkDistance(String condition, double actualDistance) {
        if (condition == null || condition.isEmpty()) return true;
        try {
            if (condition.startsWith(">=")) return actualDistance >= Double.parseDouble(condition.substring(2));
            if (condition.startsWith("<=")) return actualDistance <= Double.parseDouble(condition.substring(2));
            if (condition.startsWith(">")) return actualDistance > Double.parseDouble(condition.substring(1));
            if (condition.startsWith("<")) return actualDistance < Double.parseDouble(condition.substring(1));
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid distance format: " + condition);
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
        cooldownMap.forEach((id, cd) -> {
            if (cd > 0) cooldownMap.put(id, cd - 1);
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

    @Override
    public void start() {
    }
}