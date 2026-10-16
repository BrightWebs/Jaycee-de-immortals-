package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.gameplay.AbilityCatalog;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

final class AbilityDesignData {
    private AbilityDesignData() {}

    static AbilityBrowserModel load() {
        List<AbilityBrowserModel.Variant> variants = new ArrayList<>();
        try (var stream = AbilityDesignData.class.getResourceAsStream("/assets/jaycee/ability_designs.json")) {
            if (stream == null) throw new IllegalStateException("Missing canonical ability design data");
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var element : root.getAsJsonArray("abilities")) {
                var row = element.getAsJsonObject();
                var fields = new LinkedHashMap<String, String>();
                row.entrySet().forEach(field -> fields.put(field.getKey(),
                        field.getValue().isJsonPrimitive() ? field.getValue().getAsString() : field.getValue().toString()));
                variants.add(new AbilityBrowserModel.Variant(row.get("index").getAsInt(), row.get("id").getAsString(),
                        row.get("name").getAsString(), row.get("family").getAsString(),
                        row.get("gameplay_concept").getAsString(), fields));
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot read canonical ability design data", exception);
        }
        return new AbilityBrowserModel(variants, AbilityCatalog.ALL.stream().map(AbilityCatalog.Art::name).toList());
    }
}
