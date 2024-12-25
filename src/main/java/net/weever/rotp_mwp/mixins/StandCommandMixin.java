package net.weever.rotp_mwp.mixins;

import com.github.standobyte.jojo.command.JojoCommandsCommand;
import com.github.standobyte.jojo.command.StandCommand;
import com.github.standobyte.jojo.command.argument.StandArgument;
import com.github.standobyte.jojo.init.power.JojoCustomRegistries;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.weever.rotp_mwp.util.AddonUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(StandCommand.class)
public class StandCommandMixin {
    @Unique
    private static final DynamicCommandExceptionType GIVE_SINGLE_EXCEPTION_ALREADY_HAS = new DynamicCommandExceptionType(
            player -> new TranslationTextComponent("commands.stand.give.failed.single.has", player));
    @Unique
    private static final DynamicCommandExceptionType GIVE_MULTIPLE_EXCEPTION_ALREADY_HAVE = new DynamicCommandExceptionType(
            count -> new TranslationTextComponent("commands.stand.give.failed.multiple.have", count));
    @Unique
    private static final DynamicCommandExceptionType GIVE_SINGLE_EXCEPTION_RANDOM = new DynamicCommandExceptionType(
            player -> new TranslationTextComponent("commands.stand.give.failed.single.random", player));
    @Unique
    private static final DynamicCommandExceptionType GIVE_MULTIPLE_EXCEPTION_RANDOM = new DynamicCommandExceptionType(
            count -> new TranslationTextComponent("commands.stand.give.failed.multiple.random", count));
    @Unique
    private static final DynamicCommandExceptionType QUERY_SINGLE_FAILED_EXCEPTION = new DynamicCommandExceptionType(
            player -> new TranslationTextComponent("commands.stand.query.failed.single", player));
    @Unique
    private static final DynamicCommandExceptionType QUERY_MULTIPLE_FAILED_EXCEPTION = new DynamicCommandExceptionType(
            count -> new TranslationTextComponent("commands.stand.query.failed.multiple", count));

    /**
     * @author weeverok
     * @reason cuz mob with power
     */
    @Overwrite()
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("stand").requires(ctx -> ctx.hasPermission(2))
                .then(Commands.literal("give").then(Commands.argument("targets", EntityArgument.entities()).then(Commands.argument("stand", new StandArgument())
                        .executes(ctx -> giveStands(ctx.getSource(), EntityArgument.getEntities(ctx, "targets"), StandArgument.getStandType(ctx, "stand"), false))
                        .then(Commands.argument("replace", BoolArgumentType.bool())
                                .executes(ctx -> giveStands(ctx.getSource(), EntityArgument.getEntities(ctx, "targets"), StandArgument.getStandType(ctx, "stand"), BoolArgumentType.getBool(ctx, "replace")))))))
                .then(Commands.literal("random").then(Commands.argument("targets", EntityArgument.entities()) // /stand random <player(s)>
                        .executes(ctx -> giveRandomStands(ctx.getSource(), EntityArgument.getEntities(ctx, "targets"), false))
                        .then(Commands.argument("replace", BoolArgumentType.bool())
                                .executes(ctx -> giveRandomStands(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), BoolArgumentType.getBool(ctx, "replace"))))))
                .then(Commands.literal("clear").then(Commands.argument("targets", EntityArgument.entities())
                        .executes(ctx -> removeStands(ctx.getSource(), EntityArgument.getEntities(ctx, "targets")))))
                .then(Commands.literal("type").then(Commands.argument("targets", EntityArgument.entity())
                        .executes(ctx -> queryStand(ctx.getSource(), EntityArgument.getEntity(ctx, "targets")))))
        );
        JojoCommandsCommand.addCommand("stand");
    }

    @Unique
    private static int giveStands(CommandSource source, Collection<? extends Entity> targets, StandType<?> standType, boolean replace) throws CommandSyntaxException {
        int i = 0;
        List<LivingEntity> listOfEntities = targets.stream().filter(entity -> entity instanceof LivingEntity).map(entity -> (LivingEntity) entity).collect(Collectors.toList());

        if (!listOfEntities.isEmpty()) {
            for (LivingEntity entity : listOfEntities) {
                IStandPower power = IStandPower.getStandPowerOptional(entity).orElse(null);
                if (power != null) {
                    if (replace) {
                        power.clear();
                    }
                    if (power.givePower(standType)) {
                        i++;
                    } else if (listOfEntities.size() == 1) {
                        throw GIVE_SINGLE_EXCEPTION_ALREADY_HAS.create(listOfEntities.iterator().next().getName());
                    }
                }
            }
            if (i == 0) {
                if (listOfEntities.size() == 1) {
                    throw GIVE_SINGLE_EXCEPTION_ALREADY_HAS.create(listOfEntities.iterator().next().getName());
                } else {
                    throw GIVE_MULTIPLE_EXCEPTION_ALREADY_HAVE.create(listOfEntities.size());
                }
            } else {
                if (listOfEntities.size() == 1) {
                    source.sendSuccess(new TranslationTextComponent(
                            "commands.stand.give.success.single",
                            standType.getName(), listOfEntities.iterator().next().getDisplayName()), true);
                } else {
                    source.sendSuccess(new TranslationTextComponent(
                            "commands.stand.give.success.multiple",
                            standType.getName(), i), true);
                }
                return i;
            }
        }
        return i;
    }

    @Unique
    private static int giveRandomStands(CommandSource source, Collection<? extends Entity> targets, boolean replace) throws CommandSyntaxException {
        int i = 0;
        List<LivingEntity> listOfEntities = targets.stream().filter(entity -> entity instanceof LivingEntity).map(entity -> (LivingEntity) entity).collect(Collectors.toList());
        StandType<?> stand = null;
        if (!listOfEntities.isEmpty()) {
            for (LivingEntity player : listOfEntities) {
                IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
                if (power != null) {
                    stand = AddonUtil.randomStand(player, player.getRandom());
                    if (stand == null) {
                        if (listOfEntities.size() == 1) {
                            throw GIVE_SINGLE_EXCEPTION_RANDOM.create(listOfEntities.iterator().next().getName());
                        }
                        else {
                            throw GIVE_MULTIPLE_EXCEPTION_RANDOM.create(listOfEntities.size() - i);
                        }
                    }
                    if (replace) {
                        power.clear();
                    }
                    if (power.givePower(stand)) {
                        i++;
                    }
                    else if (listOfEntities.size() == 1) {
                        throw GIVE_SINGLE_EXCEPTION_ALREADY_HAS.create(listOfEntities.iterator().next().getName());
                    }
                }
            }
        }
        if (i == 0) {
            if (listOfEntities.size() == 1) {
                throw GIVE_SINGLE_EXCEPTION_ALREADY_HAS.create(listOfEntities.iterator().next().getName());
            } else {
                throw GIVE_MULTIPLE_EXCEPTION_ALREADY_HAVE.create(listOfEntities.size());
            }
        }
        else {
            if (listOfEntities.size() == 1) {
                source.sendSuccess(new TranslationTextComponent("commands.stand.give.success.single.random",
                        listOfEntities.iterator().next().getDisplayName()), true);
            }
            else {
                source.sendSuccess(new TranslationTextComponent(
                        "commands.stand.give.success.multiple.random", i), true);
            }
            return i;
        }
    }

    @Unique
    private static int removeStands(CommandSource source, Collection<? extends Entity> targets) throws CommandSyntaxException {
        int i = 0;
        StandType<?> removedStand = null;
        List<LivingEntity> listOfEntities = targets.stream().filter(entity -> entity instanceof LivingEntity).map(entity -> (LivingEntity) entity).collect(Collectors.toList());
        for (LivingEntity entity : listOfEntities) {
            IStandPower power = IStandPower.getStandPowerOptional(entity).orElse(null);
            if (power != null) {
                removedStand = power.getType();
                power.clear();
                power.fullStandClear();
                i++;
            }
        }
        if (i == 0) {
            if (listOfEntities.size() == 1) {
                throw QUERY_SINGLE_FAILED_EXCEPTION.create(listOfEntities.iterator().next().getName());
            } else {
                throw QUERY_MULTIPLE_FAILED_EXCEPTION.create(listOfEntities.size());
            }
        } else {
            if (listOfEntities.size() == 1) {
                ITextComponent message;
                if (removedStand != null) {
                    message = new TranslationTextComponent("commands.stand.remove.success.single",
                            removedStand.getName(), listOfEntities.iterator().next().getDisplayName());
                } else {
                    message = new TranslationTextComponent("commands.stand.remove.success.single.no_stand",
                            listOfEntities.iterator().next().getDisplayName());
                }
                source.sendSuccess(message, true);
            } else {
                source.sendSuccess(new TranslationTextComponent("commands.stand.remove.success.multiple", i), true);
            }
            return i;
        }
    }

    @Unique
    private static int queryStand(CommandSource source, Entity entity) throws CommandSyntaxException {
        if (entity instanceof LivingEntity) {
            IStandPower power = IStandPower.getStandPowerOptional((LivingEntity) entity).orElse(null);
            if (power != null) {
                if (power.hasPower()) {
                    StandType<?> type = power.getType();
                    source.sendSuccess(new TranslationTextComponent("commands.stand.query.success", entity.getDisplayName(), type.getName()), false);
                    return JojoCustomRegistries.STANDS.getNumericId(type.getRegistryName());
                }
            }
        }
        throw QUERY_SINGLE_FAILED_EXCEPTION.create(entity.getName());
    }
}
