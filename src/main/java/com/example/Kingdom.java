package com.example;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Kingdom {
    public ArrayList<ControlledPlayer> citizens;
    public ArrayList<String> citizenList;
    public String name;
    public File saveFile;
    private MinecraftServer server;
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Kingdom.class, new KingdomSerializer())
            .registerTypeAdapter(ControlledPlayer.class, new ControlledPlayer.ControlledPlayerSerializer())
            .create();

    public Kingdom(String name) {
        this.citizens = new ArrayList<>();
        this.citizenList = new ArrayList<>();
        this.name = name;
    }

    public void addCitizen(ControlledPlayer citizen) {
        citizens.add(citizen);
        citizenList.add(citizen.getUsername());
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public void setSaveFile(File saveFile) {
        this.saveFile = saveFile;
    }

    /**
     * Reads kingdom from disk, spawns citizens, and adds citizens to ExampleMod's list.
     * @param server
     * @param kingdomName
     * @param mod
     * @return
     */
    public static Kingdom getFromDisk(MinecraftServer server, String kingdomName, ExampleMod mod) {
        Path saveDirectory = server.getSavePath(WorldSavePath.ROOT);
        File saveFile = new File(new File(saveDirectory.toFile(), "kingdoms"), kingdomName + ".json");

        Kingdom kingdom = null;
        if (saveFile.exists()) {
            try (Reader reader = new FileReader(saveFile)) {
                kingdom = gson.fromJson(reader, Kingdom.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                saveFile.getParentFile().mkdirs();
                saveFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (kingdom == null) {
            kingdom = new Kingdom(kingdomName);
        }
        kingdom.setServer(server);
        kingdom.setSaveFile(saveFile);

        // Spawn players
        List<GameProfile> gameProfiles = ExampleMod.gameProfileManager.getGameProfiles();
        for (ControlledPlayer citizen : kingdom.citizens) {
            // find corresponding gameProfile
            for (GameProfile profile : gameProfiles) {
                if (profile.getName().equals(citizen.getUsername())) {
                    // Spawn player
                    CustomPlayer fakePlayer = CustomPlayer.get(server.getOverworld(), profile);
                    citizen.setPlayer(fakePlayer);
                    mod.fakePlayers.add(citizen);
                    server.getPlayerManager().loadPlayerData(fakePlayer);
                    PlayerListS2CPacket packet = new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, fakePlayer);
                    server.getPlayerManager().sendToAll(packet);
                    server.getOverworld().spawnEntity(fakePlayer);
                    server.getPlayerManager().broadcast(Text.literal("Spawning " + citizen.getUsername() + " into " + kingdomName + " kingdom."), false);
                }
            }

        }
        return kingdom;
    }

    public void saveToDisk() {
        try (Writer writer = new FileWriter(saveFile)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadNPCs(ArrayList<ControlledPlayer> allPlayers) {
        for (ControlledPlayer player : allPlayers) {
            if (citizenList.contains(player.getUsername())) {
                citizens.add(player);
            }
        }
    }

    public static class KingdomSerializer implements JsonSerializer<Kingdom>, JsonDeserializer<Kingdom> {
        @Override
        public JsonElement serialize(Kingdom src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            result.add("name", new JsonPrimitive(src.name));
            result.add("citizenList", context.serialize(src.citizenList));
            result.add("citizens", context.serialize(src.citizens));
            return result;
        }

        @Override
        public Kingdom deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json.isJsonObject()) {
                JsonObject jsonObject = json.getAsJsonObject();
                String name = jsonObject.get("name").getAsString();
                Type typeOfArrayListOfString = new TypeToken<ArrayList<String>>(){}.getType();
                ArrayList<String> citizens = context.deserialize(jsonObject.get("citizenList"), typeOfArrayListOfString);
                Kingdom kingdom = new Kingdom(name);
                kingdom.citizenList = citizens;

                Type typeOfArrayListofControlledPlayer = new TypeToken<ArrayList<ControlledPlayer>>(){}.getType();
                ArrayList<ControlledPlayer> controlledPlayers = context.deserialize(jsonObject.get("citizens"), typeOfArrayListofControlledPlayer);
                kingdom.citizens = controlledPlayers;
                return kingdom;
            }
            throw new JsonParseException("Invalid Kingdom JSON structure.");
        }
    }
}
