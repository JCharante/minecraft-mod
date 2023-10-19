package com.example;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

public class ExampleMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");
	public static final GameProfileManager gameProfileManager = new GameProfileManager(new File("gameProfiles.json"));

	public ArrayList<ControlledPlayer> fakePlayers;

	public static final EntityType<AgentEntity> AGENT_ENTITY = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier("mymod", "agent_entity"),
			FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, AgentEntity::new)
					.dimensions(EntityDimensions.fixed(0.6f, 1.95f))  // Assuming typical player dimensions
					.build()
	);

	private static int spawnAgent(ServerCommandSource source, String name, ArrayList<ControlledPlayer> list) {
		GameProfile profile = new GameProfile(UUID.randomUUID(), name);
		gameProfileManager.addProfile(profile);
		ServerWorld world = source.getWorld();
		FakePlayer fakePlayer = FakePlayer.get(world, profile);

		PlayerListS2CPacket packet = new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, fakePlayer);
		source.getServer().getPlayerManager().sendToAll(packet);
		LOGGER.info("Sent PlayerListS2CPacket to all players.");

		world.spawnEntity(fakePlayer);
		LOGGER.info("world.spawnEntity executed.");

		fakePlayer.setPos(source.getPosition().x, source.getPosition().y, source.getPosition().z);
		LOGGER.info("fakePlayer.setPos executed.");


		ControlledPlayer controlledPlayer = new ControlledPlayer(fakePlayer);
		LOGGER.info("Created new controlled player");
		list.add(controlledPlayer);
		controlledPlayer.performAction();
		LOGGER.info("Made controlled player perform action");
		return 1;
	}

	private int timer = 0;
	private boolean executed = false;

	private void spawnPlayers(MinecraftServer server) {
		ServerWorld world = server.getOverworld();
		server.getPlayerManager().broadcast(Text.literal("Spawn Players function called!"), false);
		for (GameProfile profile : gameProfileManager.getGameProfiles()) {
			LOGGER.info("Adding fake player: " + profile.getName());
			FakePlayer fakePlayer = FakePlayer.get(world, profile);
			fakePlayers.add(new ControlledPlayer(fakePlayer));
			server.getPlayerManager().loadPlayerData(fakePlayer);
			PlayerListS2CPacket packet = new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, fakePlayer);
			server.getPlayerManager().sendToAll(packet);

			for (ServerPlayerEntity spe : server.getPlayerManager().getPlayerList()) {
				LOGGER.info("Player list " + spe.getDisplayName());
			}

			world.spawnEntity(fakePlayer);
			server.getPlayerManager().broadcast(Text.literal("Sent PlayerListS2CPacket to all players"), false);
			server.sendMessage(Text.literal("Sent PlayerListS2CPacket to all players"));
			LOGGER.info("Sent PlayerListS2CPacket to all players.");
		}
	}

	private void setupPlayerSave() {
		Method savePlayerDataMethod = null;
		try {
			savePlayerDataMethod = PlayerManager.class.getDeclaredMethod("savePlayerData", ServerPlayerEntity.class);
			savePlayerDataMethod.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}

		Method finalSavePlayerDataMethod = savePlayerDataMethod;
		ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
			LOGGER.info("Server is stopping, saving player data");
			for (ControlledPlayer fakePlayer: fakePlayers) {
				FakePlayer player = fakePlayer.player;
				LOGGER.info(player.getName().getString());
				try {
					finalSavePlayerDataMethod.invoke(server.getPlayerManager(), player);
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
            }
			server.getPlayerManager().saveAllPlayerData();
		});
	}

	private void greetOnJoin() {
		ServerPlayConnectionEvents.JOIN.register((ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) -> {
			handler.player.sendMessage(Text.literal("You joined the server!"), false);
		});
	}

	private void loadNPCs() {
		ServerLoginConnectionEvents.INIT.register((ServerLoginNetworkHandler handler, MinecraftServer server) -> {
			ServerTickEvents.END_SERVER_TICK.register(server1 -> {
				timer++;
				if (timer > 200 && !executed) { // Adjust this value to increase/decrease delay
					server.submit(() -> {
						executed = true;
						spawnPlayers(server);
					});
				}
			});
		});
	}

	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("king")
				.then(CommandManager.literal("newAgent")
						.then(CommandManager.argument("name", StringArgumentType.string())
								.executes(context -> {
									return spawnAgent(context.getSource(), StringArgumentType.getString(context, "name"), this.fakePlayers);
								})).build()
				)
		));
	}


	@Override
	public void onInitialize() {
		fakePlayers = new ArrayList<ControlledPlayer>();
		setupPlayerSave();
		greetOnJoin();
		loadNPCs();

		registerCommands();

		LOGGER.info("Hello Fabric world!");
	}
}