package net.weever.rotp_mwp;

import com.github.standobyte.jojo.potion.StandVirusEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.weever.rotp_mwp.init.InitStands;
import net.weever.rotp_mwp.mechanics.virus.GenericMobStandGiver;
import net.weever.rotp_mwp.network.AddonPackets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MobsWithPowersAddon.MOD_ID)
public class MobsWithPowersAddon {
    public static final String MOD_ID = "rotp_mwp";
    public static final Logger LOGGER = LogManager.getLogger();

    public MobsWithPowersAddon() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.commonSpec);
        InitStands.ACTIONS.register(modEventBus);
        InitStands.STANDS.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup);
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        AddonPackets.init();
        StandVirusEffect.addMobStandGiver(new GenericMobStandGiver());
//        CastExceptionReturn.setupGlobalHandler(ModList.get().getMods().stream().map(mod -> mod.getModId()).collect(Collectors.toList()), Config.getCommonConfigInstance(false).removeCastExceptionCrash.get());
    }
}