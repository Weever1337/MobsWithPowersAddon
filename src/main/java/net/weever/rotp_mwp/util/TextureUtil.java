package net.weever.rotp_mwp.util;

import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.init.power.JojoCustomRegistries;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.particles.ParticleType;
import net.minecraft.potion.Effect;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TextureUtil {
    @Nullable
    public static ResourceLocation getResourceLocation(@Nullable String modid, String folder, String name) {
        if (modid == null) modid = "minecraft";
        return new ResourceLocation(modid, "textures/" + folder + "/" + name + ".png");
    }

    @Nullable
    public static ResourceLocation getResourceLocation(Item item) {
        if (item != null && item.getRegistryName() != null) {
            return getResourceLocation(item.getRegistryName().getNamespace(), "item", item.getRegistryName().getPath());
        }
        return null;
    }

    @Nullable
    public static ResourceLocation getResourceLocation(Effect effect) {
        if (effect != null && effect.getRegistryName() != null) {
            return getResourceLocation(effect.getRegistryName().getNamespace(), "mob_effect", effect.getRegistryName().getPath());
        }
        return null;
    }

    @Nullable
    public static ResourceLocation getResourceLocation(Block block) {
        if (block != null && block.getRegistryName() != null) {
            return getResourceLocation(block.getRegistryName().getNamespace(), "block", block.getRegistryName().getPath());
        }
        return null;
    }

    @Nullable
    public static ResourceLocation getResourceLocation(ParticleType particle) {
        if (particle != null && particle.getRegistryName() != null) {
            return getResourceLocation(particle.getRegistryName().getNamespace(), "particle", particle.getRegistryName().getPath());
        }
        return null;
    }

    @Nullable
    public static ResourceLocation getResourceLocation(Action action) {
        if (action != null && action.getRegistryName() != null) {
            return getResourceLocation(action.getRegistryName().getNamespace(), "action", action.getRegistryName().getPath());
        }
        return null;
    }

    @Nullable
    public static ResourceLocation getResourceLocation(StandType standType) {
        if (standType != null && standType.getRegistryName() != null) {
            return getResourceLocation(standType.getRegistryName().getNamespace(), "power", standType.getRegistryName().getPath());
        }
        return null;
    }

    @Nullable
    public static ResourceLocation getRandomActionTexture() {
        List<StandAction> STAND_ACTIONS = Arrays.stream(JojoCustomRegistries.ACTIONS.getRegistry().getValues().toArray(new Action[0]))
                .filter(action -> action instanceof StandAction)
                .map(action -> (StandAction) action)
                .collect(Collectors.toList());
        long millis = System.currentTimeMillis();
        int index = (int) ((millis / 1000) % STAND_ACTIONS.size());

        return getResourceLocation(STAND_ACTIONS.get(index));
    }

    @Nullable
    public static ResourceLocation getRandomStandTexture() {
        StandType[] STANDS = JojoCustomRegistries.STANDS.getRegistry().getValues().stream().toArray(StandType[]::new);
        long millis = System.currentTimeMillis();
        int index = (int) ((millis / 1000) % STANDS.length);

        return getResourceLocation(STANDS[index]);
    }
}