package net.weever.rotp_mwp;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.InMemoryCommentedFormat;
import com.github.standobyte.jojo.client.ClientUtil;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.weever.rotp_mwp.network.AddonPackets;
import net.weever.rotp_mwp.network.packets.CommonConfigPacket;
import net.weever.rotp_mwp.network.packets.ResetSyncedCommonConfigPacket;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = MobsWithPowersAddon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    static final ForgeConfigSpec commonSpec;
    private static final Common COMMON_FROM_FILE;
    private static final Common COMMON_SYNCED_TO_CLIENT;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        commonSpec = specPair.getRight();
        COMMON_FROM_FILE = specPair.getLeft();

        final Pair<Common, ForgeConfigSpec> syncedSpecPair = new ForgeConfigSpec.Builder().configure(builder -> new Common(builder, "synced"));
        CommentedConfig config = CommentedConfig.of(InMemoryCommentedFormat.defaultInstance());
        ForgeConfigSpec syncedSpec = syncedSpecPair.getRight();
        syncedSpec.correct(config);
        syncedSpec.setConfig(config);
        COMMON_SYNCED_TO_CLIENT = syncedSpecPair.getLeft();
    }

    @SuppressWarnings("unused")
    private static boolean isElementNonNegativeFloat(Object num, boolean moreThanZero) {
        if (num instanceof Double) {
            Double numDouble = (Double) num;
            return (numDouble > 0 || !moreThanZero && numDouble == 0) && Float.isFinite(numDouble.floatValue());
        }
        return false;
    }

    public static Common getCommonConfigInstance(boolean isClientSide) {
        return isClientSide && !ClientUtil.isLocalServer() ? COMMON_SYNCED_TO_CLIENT : COMMON_FROM_FILE;
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfig.ModConfigEvent event) {
        ModConfig config = event.getConfig();
        if (MobsWithPowersAddon.MOD_ID.equals(config.getModId()) && config.getType() == ModConfig.Type.COMMON) {
            COMMON_FROM_FILE.onLoadOrReload();
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfig.Reloading event) {
        ModConfig config = event.getConfig();
        if (MobsWithPowersAddon.MOD_ID.equals(config.getModId()) && config.getType() == ModConfig.Type.COMMON) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.getPlayerList().getPlayers().forEach(Common.SyncedValues::syncWithClient);
            }
        }
    }

    public static class Common {
        public final ForgeConfigSpec.IntValue percentageChanceToGettingAStandForMob;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blockedEntities;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blockedStandsForMobs;
        public final ForgeConfigSpec.BooleanValue useAddonStands;
        public final ForgeConfigSpec.BooleanValue spawnBoy2Man;
        private boolean loaded = false;

        private Common(ForgeConfigSpec.Builder builder) {
            this(builder, null);
        }

        private Common(ForgeConfigSpec.Builder builder, @Nullable String mainPath) {
            if (mainPath != null) {
                builder.push(mainPath);
            }

            builder.push("Settings");
            percentageChanceToGettingAStandForMob = builder
                    .translation("rotp_mwp.config.percentageChanceToGettingAStandForMob")
                    .comment("    Percentage chance of getting a Stand for Mob",
                            "    - Default to 5%")
                    .defineInRange("percentageChanceToGettingAStandForMob", 5, 0, 100);
            blockedEntities = builder
                    .translation("rotp_mwp.config.blocked_entities")
                    .comment("    Blocked entities which cant have any Power")
                    .defineList("blockedEntities", Arrays.asList("rotp_harvest:harvest", "rotp_zbc:bad_soldier", "rotp_zbc:bad_tank", "rotp_zbc:bad_helicopter", "rotp_pj:pearljam"), obj -> obj instanceof String);
            blockedStandsForMobs = builder
                    .translation("rotp_mwp.config.blocked_stands")
                    .comment("    Blocked stands that cant have any mobs in them",
                            "    - HAVE SOME PROBLEMS WITH: Harvest (with summon), Bad Company (with summon), Mandom (Because stand only for players), Weather Report (Because dont optimized for Mobs (using ServerPlayerEntity))",
                            "    - If you want to help to fix this problems - send crash reports to addons topics")
                    .defineList("blockedStandsForMobs", Arrays.asList("rotp_mandom:mandom", "rotp_harvest:harvest_stand", "rotp_zbc:bad_company", "rotp_wr:weather_report"), obj -> obj instanceof String);
            useAddonStands = builder
                    .translation("rotp.mwp.config.use_addon_stands")
                    .comment("    Mobs will have stands from addons. ALSO, MAYBE HAVE SOME PROBLEMS, CRASH REPORTS SEND TO ADDONS!!!!!!!!")
                    .define("useAddonStands", false);
            spawnBoy2Man = builder
                    .translation("rotp.mwp.config.spawnboy2man")
                    .comment("    Boy2Man will spawn in villages... (WARNING: This kid in W.I.P in a mod, and can have some bugs)",
                            "    - Disabled.")
                    .define("spawnBoy2Man", false);
            builder.pop();

            if (mainPath != null) {
                builder.pop();
            }
        }

        public boolean isConfigLoaded() {
            return loaded;
        }

        private void onLoadOrReload() {
            loaded = true;
        }

        public static class SyncedValues {
            private final int percentageChanceToGettingAStandForMob;
            private final List<String> blockedEntities;
            private final List<String> blockedStandsForMobs;
            private final boolean useAddonStands;
            private final boolean spawnBoy2Man;

            public SyncedValues(PacketBuffer buf) {
                this.percentageChanceToGettingAStandForMob = buf.readInt();
                int blockedEntitiesSize = buf.readInt();
                this.blockedEntities = new ArrayList<>(blockedEntitiesSize);
                for (int i = 0; i < blockedEntitiesSize; i++) {
                    blockedEntities.add(buf.readUtf());
                }

                int blockedStandsForMobsSize = buf.readInt();
                this.blockedStandsForMobs = new ArrayList<>(blockedStandsForMobsSize);
                for (int i = 0; i < blockedStandsForMobsSize; i++) {
                    blockedStandsForMobs.add(buf.readUtf());
                }

                this.useAddonStands = buf.readBoolean();
                this.spawnBoy2Man = buf.readBoolean();
            }

            private SyncedValues(Common config) {
                percentageChanceToGettingAStandForMob = config.percentageChanceToGettingAStandForMob.get();
                blockedEntities = (List<String>) config.blockedEntities.get();
                blockedStandsForMobs = (List<String>) config.blockedStandsForMobs.get();
                useAddonStands = config.useAddonStands.get();
                spawnBoy2Man = config.spawnBoy2Man.get();
            }

            public static void resetConfig() {
                COMMON_SYNCED_TO_CLIENT.percentageChanceToGettingAStandForMob.clearCache();
                COMMON_SYNCED_TO_CLIENT.blockedEntities.clearCache();
                COMMON_SYNCED_TO_CLIENT.blockedStandsForMobs.clearCache();
                COMMON_SYNCED_TO_CLIENT.useAddonStands.clearCache();
                COMMON_SYNCED_TO_CLIENT.spawnBoy2Man.clearCache();
            }

            public static void syncWithClient(ServerPlayerEntity player) {
                AddonPackets.sendToClient(new CommonConfigPacket(new SyncedValues(COMMON_FROM_FILE)), player);
            }

            public static void onPlayerLogout(ServerPlayerEntity player) {
                AddonPackets.sendToClient(new ResetSyncedCommonConfigPacket(), player);
            }

            public void writeToBuf(PacketBuffer buf) {
                buf.writeInt(percentageChanceToGettingAStandForMob);
                buf.writeInt(blockedEntities.size());
                for (String entity : blockedEntities) {
                    buf.writeUtf(entity);
                }

                buf.writeInt(blockedStandsForMobs.size());
                for (String entity : blockedStandsForMobs) {
                    buf.writeUtf(entity);
                }
                buf.writeBoolean(useAddonStands);
                buf.writeBoolean(spawnBoy2Man);
            }

            public void changeConfigValues() {
                COMMON_SYNCED_TO_CLIENT.percentageChanceToGettingAStandForMob.set(percentageChanceToGettingAStandForMob);
                COMMON_SYNCED_TO_CLIENT.blockedEntities.set(blockedEntities);
                COMMON_SYNCED_TO_CLIENT.blockedStandsForMobs.set(blockedStandsForMobs);
                COMMON_SYNCED_TO_CLIENT.useAddonStands.set(useAddonStands);
                COMMON_SYNCED_TO_CLIENT.spawnBoy2Man.set(spawnBoy2Man);
            }
        }
    }
}
