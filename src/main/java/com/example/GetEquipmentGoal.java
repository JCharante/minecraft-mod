package com.example;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.AxeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetEquipmentGoal extends Goal {
    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");

    private final AgentEntity agent;
    private final int checkInterval = 100; // Adjust as needed, this checks every 5 seconds
    private int timer = 0;

    public GetEquipmentGoal(AgentEntity agent) {
        this.agent = agent;
    }

    @Override
    public boolean canStart() {
        // This goal can start if the agent doesn't have an axe
        return !hasAxe();
    }

    @Override
    public boolean shouldContinue() {
        if (hasAxe()) {
            agent.sayInChat("I have an axe!");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        timer++;

        // Every few seconds, if we still don't have an axe, ask for one
        if (timer >= checkInterval && !hasAxe()) {
            agent.sayInChat("I need an axe");
            timer = 0; // Reset the timer
        }
    }

    private boolean hasAxe() {
        // Loop through the agent's inventory and check for any type of axe
        for (int i = 0; i < agent.getInventory().size(); i++) {
            ItemStack stack = agent.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                return true;
            }
        }
        return false;
    }

}
