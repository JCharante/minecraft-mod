package com.example;

import net.minecraft.text.Text;

public class ControlledPlayer {
    public CustomPlayer player;

    public ControlledPlayer(CustomPlayer player) {
        this.player = player;
    }

    public void tick() {
        player.playerTick();
//        this.player.interactionManager.update();
//        this.player.baseTick();
////        Criteria.TICK.trigger(this.player);
////        if (this.player.levitationStartPos != null) {
////            Criteria.LEVITATION.trigger(this, this.levitationStartPos, this.age - this.levitationStartTick);
////        }
//        this.player.tickFallStartPos();
    }

    public void performAction() {
        // Call methods on the player instance to control its behavior.
        // For example, making the player jump:
        player.sendMessage(Text.literal("Hello, world!"), false);
        player.jump();
    }
}
