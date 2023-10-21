package com.example;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;

public class Kingdom {
    public ArrayList<ControlledPlayer> citizens;
    private ArrayList<String> citizenList;
    private String name;
    private File saveFile;

    public Kingdom(MinecraftServer server, ServerWorld world, String name) {
        this.citizens = new ArrayList<>();
        this.citizenList = new ArrayList<>();
        this.name = name;

        Path saveDirectory = server.getSavePath(WorldSavePath.ROOT);
        // String worldName = world.getRegistryKey().getValue().toString().replace(':', '_');
        saveFile = new File(new File(saveDirectory.toFile(), "kingdoms"), name + ".json");

        if(saveFile.exists()){
            loadFromDisk();
        } else {
            try {
                saveFile.getParentFile().mkdirs();
                saveFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadFromDisk() {
        try (Reader reader = new FileReader(saveFile)) {
            //GameProfile[] profiles = gson.fromJson(reader, GameProfile[].class);
            //if (profiles != null) {
            //    gameProfiles.addAll(Arrays.asList(profiles));
            //}
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveToDisk() {
        try (Writer writer = new FileWriter(saveFile)) {
            //gson.toJson(gameProfiles.toArray(new GameProfile[0]), writer);
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
}
