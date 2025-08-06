package net.weever.rotp_mwp.mechanics.combo;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StandComboData {
    private final Map<String, List<ComboStep>> combos = new HashMap<>();
    private String blockAction;
    private Map<String, List<String>> useByResolve;

    public List<ComboStep> getComboSteps(String comboName) {
        return combos.get(comboName);
    }

    public String getBlockAction() {
        return blockAction;
    }

    public List<String> getAvailableCombos(int resolveLevel) {
        if (useByResolve == null || useByResolve.isEmpty()) {
            return new ArrayList<>(combos.keySet());
        }
        return useByResolve.getOrDefault(resolveLevel + "lvl", new ArrayList<>());
    }

    public static class Deserializer implements JsonDeserializer<StandComboData> {
        @Override
        public StandComboData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            StandComboData data = new StandComboData();
            JsonObject jsonObject = json.getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();

                switch (key) {
                    case "block":
                        data.blockAction = value.getAsString();
                        break;
                    case "use_by_resolve":
                        data.useByResolve = context.deserialize(value, new com.google.gson.reflect.TypeToken<Map<String, List<String>>>() {
                        }.getType());
                        break;
                    default:
                        List<ComboStep> stepList = context.deserialize(value, new com.google.gson.reflect.TypeToken<List<ComboStep>>() {}.getType());
                        data.combos.put(key, stepList);
                        break;
                }
            }
            return data;
        }
    }
}