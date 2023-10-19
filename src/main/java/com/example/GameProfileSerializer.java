package com.example;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;

import java.lang.reflect.Type;
import java.util.UUID;

public class GameProfileSerializer implements JsonSerializer<GameProfile>, JsonDeserializer<GameProfile> {
    @Override
    public JsonElement serialize(GameProfile src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.add("id", new JsonPrimitive(src.getId().toString()));
        result.add("name", new JsonPrimitive(src.getName()));
        return result;
    }

    @Override
    public GameProfile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            UUID id = UUID.fromString(jsonObject.get("id").getAsString());
            String name = jsonObject.get("name").getAsString();
            return new GameProfile(id, name);
        }
        throw new JsonParseException("Invalid GameProfile JSON structure.");
    }
}