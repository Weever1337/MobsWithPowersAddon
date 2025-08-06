package net.weever.rotp_mwp;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.InMemoryCommentedFormat;
import com.github.standobyte.jojo.client.ClientUtil;
import com.google.common.collect.Lists;
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
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blockedStandActionsForMobs;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> longRangeStands;
        public final ForgeConfigSpec.BooleanValue useAddonStands;
        public final ForgeConfigSpec.BooleanValue smallAnarchyWithStands;
        public final ForgeConfigSpec.BooleanValue dropStandDiscFromMobs;
        public final ForgeConfigSpec.BooleanValue spawnBoy2Man;
//        public final ForgeConfigSpec.BooleanValue removeCastExceptionCrash;
        public final ForgeConfigSpec.BooleanValue useWIPComboSystem;
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
                    .defineListAllowEmpty(Lists.newArrayList("blockedEntities"), () -> Arrays.asList("rotp_harvest:harvest", "rotp_zbc:bad_soldier", "rotp_zbc:bad_tank", "rotp_zbc:bad_helicopter", "rotp_pj:pearljam", "rotp_zkq:sheer_heart", "rotp_stfn:player_arm", "rotp_stfn:player_leg", "rotp_stfn:player_head"), obj -> obj instanceof String);
            blockedStandsForMobs = builder
                    .translation("rotp_mwp.config.blocked_stands")
                    .comment("    Blocked stands that cant have any mobs in them",
                            "    - HAVE SOME PROBLEMS WITH: Harvest (with summon), Bad Company (with summon), Weather Report (Because dont optimized for Mobs (using ServerPlayerEntity))",
                            "    - If you want to help to fix this problems - send crash reports to addons topics")
                    .defineListAllowEmpty(Lists.newArrayList("blockedStandsForMobs"), () -> Arrays.asList("rotp_harvest:harvest_stand", "rotp_zbc:bad_company", "rotp_wr:weather_report", "rotp_ctr:catch_the_rainbow", "rotp_metallica:metallica", "rotp_zwa:white_album", "rotp_wou:wonder_of_you", "rotp_wonderofu:wonderofu", "rotp_lovers:lovers"), obj -> obj instanceof String);
            blockedStandActionsForMobs = builder
                    .translation("rotp_mwp.config.blocked_actions")
                    .comment("    Blocked stand actions for Mobs. They cant use them. By default: SP's Zoom, CD's Repair, CD's Anchor Move")
                    .defineListAllowEmpty(Lists.newArrayList("blockedStandActions"), () -> Arrays.asList("rotp_harvest:search", "rotp_harvest:go_to_this_place", "rotp_harvest:set_target", "rotp_harvest:forget_target", "rotp_zbc:set_target", "rotp_zbc:forget_target", "rotp_harvest:stay_with", "rotp_harvest:carry_up"), obj -> obj instanceof String);
            longRangeStands = builder
                    .translation("rotp_mwp.config.long_range_stands")
                    .comment("    Long range stands for Mobs. They have 30 blocks range.")
                    .defineListAllowEmpty(Lists.newArrayList("longRangeStands"), () -> Arrays.asList("rotp_harvest:harvest_stand", "rotp_zbc:bad_company", "rotp_zgd:green_day"), obj -> obj instanceof String);
            useAddonStands = builder
                    .translation("rotp.mwp.config.use_addon_stands")
                    .comment("    Mobs will have stands from addons. ALSO, MAYBE HAVE SOME PROBLEMS, CRASH REPORTS SEND TO ADDONS!!!!!!!!")
                    .define("useAddonStands", false);
            smallAnarchyWithStands = builder
                    .translation("rotp.mwp.config.smallanarchywithstands")
                    .comment("    Adding a way to give a stand to stand to stand to stand to stand....")
                    .define("smallAnarchyWithStands", false);
            dropStandDiscFromMobs = builder
                    .translation("rotp.mwp.config.drop_stand_disc_from_mobs")
                    .comment("    If enabled, Stand users drop their Stand's Disc upon death, BUT FOR MOB VERSION",
                            "    - Works only when keepStandOnDeath (in jojo-common config) is set to false.")
                    .define("dropStandDiscFromMobs", false);
            spawnBoy2Man = builder
                    .translation("rotp.mwp.config.spawnboy2man")
                    .comment("    Boy2Man will spawn in villages... (WARNING: This kid W.I.P in a mod, and can have some bugs)")
                    .define("spawnBoy2Man", false);
//            removeCastExceptionCrash = builder
//                    .translation("rotp.mwp.config.remove_cast_exception_crash")
//                    .comment("    Remove cast exception crash")
//                    .define("removeCastExceptionCrash", false);
            useWIPComboSystem = builder
                    .translation("rotp.mwp.config.usewipcombosystem")
                    .comment("    Use W.I.P Combo System. If it's false mobs will use random abilities")
                    .define("useWIPComboSystem", true);
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
            private final List<String> blockedStandActionsForMobs;
            private final List<String> longRangeStands;
            private final boolean useAddonStands;
            private final boolean smallAnarchyWithStands;
            private final boolean dropStandDiscFromMobs;
            private final boolean spawnBoy2Man;
//            private final boolean removeCastExceptionCrash;
            private final boolean useWIPComboSystem;

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

                int blockedStandActionsForMobsSize = buf.readInt();
                this.blockedStandActionsForMobs = new ArrayList<>(blockedStandActionsForMobsSize);
                for (int i = 0; i < blockedStandActionsForMobsSize; i++) {
                    blockedStandActionsForMobs.add(buf.readUtf());
                }

                int longRangeStandsSize = buf.readInt();
                this.longRangeStands = new ArrayList<>(longRangeStandsSize);
                for (int i = 0; i < longRangeStandsSize; i++) {
                    longRangeStands.add(buf.readUtf());
                }

                this.useAddonStands = buf.readBoolean();
                this.smallAnarchyWithStands = buf.readBoolean();
                this.dropStandDiscFromMobs = buf.readBoolean();
                this.spawnBoy2Man = buf.readBoolean();
//                this.removeCastExceptionCrash = buf.readBoolean();
                this.useWIPComboSystem = buf.readBoolean();
            }

            private SyncedValues(Common config) {
                percentageChanceToGettingAStandForMob = config.percentageChanceToGettingAStandForMob.get();
                blockedEntities = (List<String>) config.blockedEntities.get();
                blockedStandsForMobs = (List<String>) config.blockedStandsForMobs.get();
                blockedStandActionsForMobs = (List<String>) config.blockedStandActionsForMobs.get();
                longRangeStands = (List<String>) config.longRangeStands.get();
                useAddonStands = config.useAddonStands.get();
                smallAnarchyWithStands = config.smallAnarchyWithStands.get();
                dropStandDiscFromMobs = config.dropStandDiscFromMobs.get();
                spawnBoy2Man = config.spawnBoy2Man.get();
//                removeCastExceptionCrash = config.removeCastExceptionCrash.get();
                useWIPComboSystem = config.useWIPComboSystem.get();
            }

            public static void resetConfig() {
                COMMON_SYNCED_TO_CLIENT.percentageChanceToGettingAStandForMob.clearCache();
                COMMON_SYNCED_TO_CLIENT.blockedEntities.clearCache();
                COMMON_SYNCED_TO_CLIENT.blockedStandsForMobs.clearCache();
                COMMON_SYNCED_TO_CLIENT.blockedStandActionsForMobs.clearCache();
                COMMON_SYNCED_TO_CLIENT.longRangeStands.clearCache();
                COMMON_SYNCED_TO_CLIENT.useAddonStands.clearCache();
                COMMON_SYNCED_TO_CLIENT.smallAnarchyWithStands.clearCache();
                COMMON_SYNCED_TO_CLIENT.dropStandDiscFromMobs.clearCache();
                COMMON_SYNCED_TO_CLIENT.spawnBoy2Man.clearCache();
//                COMMON_SYNCED_TO_CLIENT.removeCastExceptionCrash.clearCache();
                COMMON_SYNCED_TO_CLIENT.useWIPComboSystem.clearCache();
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

                buf.writeInt(blockedStandActionsForMobs.size());
                for (String entity : blockedStandActionsForMobs) {
                    buf.writeUtf(entity);
                }

                buf.writeInt(longRangeStands.size());
                for (String entity : longRangeStands) {
                    buf.writeUtf(entity);
                }
                buf.writeBoolean(useAddonStands);
                buf.writeBoolean(smallAnarchyWithStands);
                buf.writeBoolean(dropStandDiscFromMobs);
                buf.writeBoolean(spawnBoy2Man);
//                buf.writeBoolean(removeCastExceptionCrash);
                buf.writeBoolean(useWIPComboSystem);
            }

            public void changeConfigValues() {
                COMMON_SYNCED_TO_CLIENT.percentageChanceToGettingAStandForMob.set(percentageChanceToGettingAStandForMob);
                COMMON_SYNCED_TO_CLIENT.blockedEntities.set(blockedEntities);
                COMMON_SYNCED_TO_CLIENT.blockedStandsForMobs.set(blockedStandsForMobs);
                COMMON_SYNCED_TO_CLIENT.blockedStandActionsForMobs.set(blockedStandActionsForMobs);
                COMMON_SYNCED_TO_CLIENT.longRangeStands.set(longRangeStands);
                COMMON_SYNCED_TO_CLIENT.useAddonStands.set(useAddonStands);
                COMMON_SYNCED_TO_CLIENT.smallAnarchyWithStands.set(smallAnarchyWithStands);
                COMMON_SYNCED_TO_CLIENT.dropStandDiscFromMobs.set(dropStandDiscFromMobs);
                COMMON_SYNCED_TO_CLIENT.spawnBoy2Man.set(spawnBoy2Man);
//                COMMON_SYNCED_TO_CLIENT.removeCastExceptionCrash.set(removeCastExceptionCrash);
                COMMON_SYNCED_TO_CLIENT.useWIPComboSystem.set(useWIPComboSystem);
            }
        }
    }
}
