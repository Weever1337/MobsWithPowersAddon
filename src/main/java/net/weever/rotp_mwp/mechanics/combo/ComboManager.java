package net.weever.rotp_mwp.mechanics.combo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import net.weever.rotp_mwp.MobsWithPowersAddon;
import org.jetbrains.annotations.NotNull;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class ComboManager implements ISelectiveResourceReloadListener {
    private static final String STAND_FOLDER_NAME = "stand_combos";
    private static final String NON_STAND_FOLDER_NAME = "nonstand_combos";

    private static final Gson STAND_GSON = new GsonBuilder()
            .registerTypeAdapter(StandComboData.class, new StandComboData.Deserializer())
            .setPrettyPrinting().create();

    private static final Gson NON_STAND_GSON = new GsonBuilder()
            .registerTypeAdapter(NonStandComboData.class, new NonStandComboData.Deserializer())
            .setPrettyPrinting().create();

    private static final Map<String, StandComboData> STAND_COMBOS = new HashMap<>();
    private static final Map<String, NonStandComboData> NON_STAND_COMBOS = new HashMap<>();

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager, @NotNull Predicate<IResourceType> resourcePredicate) {
        loadStandCombos(resourceManager);
        loadNonStandCombos(resourceManager);
    }

    private void loadStandCombos(IResourceManager resourceManager) {
        STAND_COMBOS.clear();
        Collection<ResourceLocation> resources = resourceManager.listResources(STAND_FOLDER_NAME, path -> path.endsWith(".json"));
        MobsWithPowersAddon.getLogger().info("Found {} stand combo files to load.", resources.size());
        for (ResourceLocation location : resources) {
            try (IResource resource = resourceManager.getResource(location);
                 Reader reader = new InputStreamReader(resource.getInputStream())) {
                Type type = new TypeToken<Map<String, StandComboData>>() {}.getType();
                Map<String, StandComboData> loadedFile = STAND_GSON.fromJson(reader, type);
                if (loadedFile != null) {
                    STAND_COMBOS.putAll(loadedFile);
                }
            } catch (Exception e) {
                MobsWithPowersAddon.getLogger().error("Failed to load stand combo file: " + location, e);
            }
        }
        MobsWithPowersAddon.getLogger().info("Loaded a total of {} stand combo configurations.", STAND_COMBOS.size());
    }

    private void loadNonStandCombos(IResourceManager resourceManager) {
        NON_STAND_COMBOS.clear();
        Collection<ResourceLocation> resources = resourceManager.listResources(NON_STAND_FOLDER_NAME, path -> path.endsWith(".json"));
        MobsWithPowersAddon.getLogger().info("Found {} non-stand combo files to load.", resources.size());
        for (ResourceLocation location : resources) {
            try (IResource resource = resourceManager.getResource(location);
                 Reader reader = new InputStreamReader(resource.getInputStream())) {
                Type type = new TypeToken<Map<String, NonStandComboData>>() {}.getType();
                Map<String, NonStandComboData> loadedFile = NON_STAND_GSON.fromJson(reader, type);
                if (loadedFile != null) {
                    NON_STAND_COMBOS.putAll(loadedFile);
                }
            } catch (Exception e) {
                MobsWithPowersAddon.getLogger().error("Failed to load non-stand combo file: " + location, e);
            }
        }
        MobsWithPowersAddon.getLogger().info("Loaded a total of {} non-stand combo configurations.", NON_STAND_COMBOS.size());
    }

    public static StandComboData getDataForStand(String standId) {
        return STAND_COMBOS.get(standId);
    }

    public static NonStandComboData getDataForNonStand(String powerId) {
        return NON_STAND_COMBOS.get(powerId);
    }
}