package com.example;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.text.Text;

public class ControlledPlayer {
    public FakePlayer player;

    public ControlledPlayer(FakePlayer player) {
        this.player = player;
    }

    public void performAction() {
        // Call methods on the player instance to control its behavior.
        // For example, making the player jump:
        player.sendMessage(Text.literal("Hello, world!"), false);
        player.jump();
    }
}
