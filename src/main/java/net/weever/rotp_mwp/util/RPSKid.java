//package net.weever.rotp_mwp.util;
//
//import com.github.standobyte.jojo.init.ModEntityTypes;
//import net.minecraft.entity.EntityClassification;
//import net.minecraft.entity.EntitySpawnPlacementRegistry;
//import net.minecraft.entity.EntityType;
//import net.minecraft.entity.SpawnReason;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.world.IWorld;
//import net.minecraft.world.biome.MobSpawnInfo;
//import net.minecraft.world.gen.Heightmap;
//import net.minecraftforge.event.world.BiomeLoadingEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//import net.weever.rotp_mwp.Config;
//import net.weever.rotp_mwp.MobsWithPowersAddon;
//
//import java.util.Random;
//
//@Mod.EventBusSubscriber(modid = MobsWithPowersAddon.MOD_ID)// wtf, idk how to spawn entities, sorry...
//public class RPSKid {
//    @SubscribeEvent
//    public static void onBiomeLoading(BiomeLoadingEvent event) {
//        event.getSpawns().getSpawner(EntityClassification.CREATURE).add(
//                new MobSpawnInfo.Spawners(
//                        ModEntityTypes.ROCK_PAPER_SCISSORS_KID.get(),
//                        1,
//                        1,
//                        1
//                )
//        );
//    }
//
//    public static void setupSpawnPlacement() {
//        EntitySpawnPlacementRegistry.register(
//                ModEntityTypes.ROCK_PAPER_SCISSORS_KID.get(),
//                EntitySpawnPlacementRegistry.PlacementType.ON_GROUND,
//                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
//                RPSKid::canSpawnHere
//        );
//    }
//
//    public static boolean canSpawnHere(EntityType<?> entityType, IWorld world, SpawnReason reason, BlockPos pos, Random random) {
//        float randomValue = Config.getCommonConfigInstance(world.isClientSide()).spawnBoy2Man.get() ? 1f : 1f;
//        return random.nextFloat() < randomValue;
//    }
//}
