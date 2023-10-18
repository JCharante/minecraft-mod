package com.example;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.server.command.CommandManager.*;

public class ExampleMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");

	public static final EntityType<AgentEntity> AGENT_ENTITY = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier("mymod", "agent_entity"),
			FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, AgentEntity::new)
					.dimensions(EntityDimensions.fixed(0.6f, 1.95f))  // Assuming typical player dimensions
					.build()
	);

	private static int spawnAgent(ServerCommandSource source, String name) {
		ServerWorld world = source.getWorld();
		AgentEntity agent = new AgentEntity(ExampleMod.AGENT_ENTITY, world);
		agent.setRealName(name);
		agent.setCustomName(Text.of(name));
		agent.setCustomNameVisible(true);

		Vec3d vec3d = source.getPosition();
		agent.refreshPositionAndAngles(vec3d.x, vec3d.y, vec3d.z, 0.0F, 0.0F);
		world.spawnEntity(agent);

		source.sendFeedback(() -> Text.literal("Spawned agent with name: " + name), true);
		return 1;  // Command success!
	}



	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		FabricDefaultAttributeRegistry.register(AGENT_ENTITY, AgentEntity.createMobAttributes());


//		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("king")
//				.executes(context -> {
//					// For versions since 1.20, please use the following, which is intended to avoid creating Text objects if no feedback is needed.
//					context.getSource().sendMessage(() -> Text.literal("Called /foo with no arguments"));
//
//					return 1;
//				})));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("king")
				.then(CommandManager.literal("newAgent")
						.then(CommandManager.argument("name", StringArgumentType.string())
								.executes(context -> {
                                    return spawnAgent(context.getSource(), StringArgumentType.getString(context, "name"));
								})).build()
				)
		));

		LOGGER.info("Hello Fabric world!");
	}
}