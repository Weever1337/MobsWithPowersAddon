package net.weever.rotp_mwp.util;

import com.github.standobyte.jojo.JojoMod;
import com.github.standobyte.jojo.capability.entity.power.NonStandCapProvider;
import com.github.standobyte.jojo.capability.entity.power.StandCapProvider;
import com.github.standobyte.jojo.entity.mob.IMobPowerUser;
import com.github.standobyte.jojo.entity.mob.IMobStandUser;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.weever.rotp_mwp.Config;
import net.weever.rotp_mwp.MobsWithPowersAddon;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.weever.rotp_mwp.util.AddonUtil.getBlockedEntitiesList;

@Mod.EventBusSubscriber(modid = MobsWithPowersAddon.MOD_ID)
public class CapabilityAdderForAll {
    public static final List<Class<?>> blockedEntities = Util.make(new ArrayList<>(), list -> {
        list.add(PlayerEntity.class);
        list.add(StandEntity.class);
        list.add(EnderDragonEntity.class);
        list.add(IMobPowerUser.class);
        list.add(IMobStandUser.class);
    });
    private static final ResourceLocation STAND_CAP = new ResourceLocation(JojoMod.MOD_ID, "stand");
    private static final ResourceLocation NON_STAND_CAP = new ResourceLocation(JojoMod.MOD_ID, "non_stand");

    @SubscribeEvent
    public static void onAttachCapabilitiesEntity(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof LivingEntity && !isBlockedEntity(entity)) {
            LivingEntity living = (LivingEntity) entity;
            event.addCapability(STAND_CAP, new StandCapProvider(living));
            event.addCapability(NON_STAND_CAP, new NonStandCapProvider(living));
        }
    }

    public static boolean isBlockedEntity(Entity entity) {
        if (Config.getCommonConfigInstance(entity.level.isClientSide()).smallAnarchyWithStands.get()){
            blockedEntities.remove(StandEntity.class);
        }
        String uniqueId = Objects.requireNonNull(entity.getType().getRegistryName()).toString();
        return blockedEntities.stream().anyMatch(clazz -> clazz.isAssignableFrom(entity.getClass())) || getBlockedEntitiesList(entity.level.isClientSide()).contains(uniqueId);
    }
}
