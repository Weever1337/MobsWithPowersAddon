package net.weever.rotp_mwp.mixins;

import com.github.standobyte.jojo.command.StandCommand;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

@Mixin(StandCommand.class)
public class StandCommandMixin {
    private static final DynamicCommandExceptionType GIVE_SINGLE_EXCEPTION_ALREADY_HAS = new DynamicCommandExceptionType(
            player -> new TranslationTextComponent("commands.stand.give.failed.single.has", player));
    private static final DynamicCommandExceptionType GIVE_MULTIPLE_EXCEPTION_ALREADY_HAVE = new DynamicCommandExceptionType(
            count -> new TranslationTextComponent("commands.stand.give.failed.multiple.have", count));
    private static final DynamicCommandExceptionType GIVE_SINGLE_EXCEPTION_RANDOM = new DynamicCommandExceptionType(
            player -> new TranslationTextComponent("commands.stand.give.failed.single.random", player));
    private static final DynamicCommandExceptionType GIVE_MULTIPLE_EXCEPTION_RANDOM = new DynamicCommandExceptionType(
            count -> new TranslationTextComponent("commands.stand.give.failed.multiple.random", count));
    private static final DynamicCommandExceptionType QUERY_SINGLE_FAILED_EXCEPTION = new DynamicCommandExceptionType(
            player -> new TranslationTextComponent("commands.stand.query.failed.single", player));
    private static final DynamicCommandExceptionType QUERY_MULTIPLE_FAILED_EXCEPTION = new DynamicCommandExceptionType(
            count -> new TranslationTextComponent("commands.stand.query.failed.multiple", count));

    @Redirect(
            method = "register",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/command/arguments/EntityArgument;players()Lnet/minecraft/command/arguments/EntityArgument;")
    )
    private static EntityArgument redirectPlayersToEntities() {
        return EntityArgument.entities();
    }

    @Redirect(
            method = "register",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/command/arguments/EntityArgument;player()Lnet/minecraft/command/arguments/EntityArgument;")
    )
    private static EntityArgument redirectPlayersToEntity() {
        return EntityArgument.entity();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private static int giveStands(CommandSource source, Collection<LivingEntity> targets, StandType<?> standType, boolean replace) throws CommandSyntaxException {
        int i = 0;
        for (LivingEntity entity : targets) {
            IStandPower power = IStandPower.getStandPowerOptional(entity).orElse(null);
            if (power != null) {
                if (replace) {
                    power.clear();
                }
                if (power.givePower(standType)) {
                    i++;
                } else if (targets.size() == 1) {
                    throw GIVE_SINGLE_EXCEPTION_ALREADY_HAS.create(targets.iterator().next().getName());
                }
            }
        }
        if (i == 0) {
            if (targets.size() == 1) {
                throw GIVE_SINGLE_EXCEPTION_ALREADY_HAS.create(targets.iterator().next().getName());
            } else {
                throw GIVE_MULTIPLE_EXCEPTION_ALREADY_HAVE.create(targets.size());
            }
        } else {
            if (targets.size() == 1) {
                source.sendSuccess(new TranslationTextComponent(
                        "commands.stand.give.success.single",
                        standType.getName(), targets.iterator().next().getDisplayName()), true);
            } else {
                source.sendSuccess(new TranslationTextComponent(
                        "commands.stand.give.success.multiple",
                        standType.getName(), i), true);
            }
            return i;
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private static int removeStands(CommandSource source, Collection<LivingEntity> targets) throws CommandSyntaxException {
        int i = 0;
        StandType<?> removedStand = null;
        for (LivingEntity entity : targets) {
            IStandPower power = IStandPower.getStandPowerOptional(entity).orElse(null);
            if (power != null) {
                removedStand = power.getType();
                power.clear();
                power.fullStandClear();
                i++;
            }
        }
        if (i == 0) {
            if (targets.size() == 1) {
                throw QUERY_SINGLE_FAILED_EXCEPTION.create(targets.iterator().next().getName());
            } else {
                throw QUERY_MULTIPLE_FAILED_EXCEPTION.create(targets.size());
            }
        } else {
            if (targets.size() == 1) {
                ITextComponent message;
                if (removedStand != null) {
                    message = new TranslationTextComponent("commands.stand.remove.success.single",
                            removedStand.getName(), targets.iterator().next().getDisplayName());
                } else {
                    message = new TranslationTextComponent("commands.stand.remove.success.single.no_stand",
                            targets.iterator().next().getDisplayName());
                }
                source.sendSuccess(message, true);
            } else {
                source.sendSuccess(new TranslationTextComponent("commands.stand.remove.success.multiple", i), true);
            }
            return i;
        }
    }
}
