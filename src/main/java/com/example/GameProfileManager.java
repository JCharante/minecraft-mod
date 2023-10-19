package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameProfileManager {
    private final File gameProfileFile;
    private final List<GameProfile> gameProfiles;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(GameProfile.class, new GameProfileSerializer())
            .create();

    public GameProfileManager(File gameProfileFile) {
        this.gameProfileFile = gameProfileFile;
        this.gameProfiles = new ArrayList<>();
        load();
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