package com.example.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameProfileManager {
    private File gameProfileFile;
    private final List<GameProfile> gameProfiles;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(GameProfile.class, new GameProfileSerializer())
            .create();


    public GameProfileManager(MinecraftServer server, ServerWorld world) {
        Path saveDirectory = server.getSavePath(WorldSavePath.ROOT);
        String worldName = world.getRegistryKey().getValue().toString().replace(':', '_');
        gameProfileFile = new File(new File(saveDirectory.toFile(), "fake-players"), worldName + ".json");
        this.gameProfiles = new ArrayList<>();

        if(gameProfileFile.exists()){
            load();
        } else {
            try {
                gameProfileFile.getParentFile().mkdirs();
                gameProfileFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public List<GameProfile> getGameProfiles() {
        return gameProfiles;
    }

    public void addProfile(GameProfile profile) {
        gameProfiles.add(profile);
        save();
    }

    public void removeProfile(GameProfile profile) {
        gameProfiles.remove(profile);
        save();
    }

    private void load() {
        try (Reader reader = new FileReader(gameProfileFile)) {
            GameProfile[] profiles = gson.fromJson(reader, GameProfile[].class);
            if (profiles != null) {
                gameProfiles.addAll(Arrays.asList(profiles));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try (Writer writer = new FileWriter(gameProfileFile)) {
            gson.toJson(gameProfiles.toArray(new GameProfile[0]), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}