package net.weever.rotp_mwp.mechanics.combo;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NonStandComboData {
    private final Map<String, List<ComboStep>> combos = new HashMap<>();
    private MaintenanceStep maintenance;

    public List<ComboStep> getComboSteps(String comboName) {
        return combos.get(comboName);
    }

    public MaintenanceStep getMaintenance() {
        return maintenance;
    }

    public Set<String> getComboNames() {
        return combos.keySet();
    }

    public static class Deserializer implements JsonDeserializer<NonStandComboData> {
        @Override
        public NonStandComboData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            NonStandComboData data = new NonStandComboData();
            JsonObject jsonObject = json.getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();

                if (key.equals("maintenance")) {
                    data.maintenance = context.deserialize(value, MaintenanceStep.class);
                } else {
                    List<ComboStep> stepList = context.deserialize(value, new com.google.gson.reflect.TypeToken<List<ComboStep>>() {}.getType());
                    data.combos.put(key, stepList);
                }
            }
            return data;
        }
    }
}