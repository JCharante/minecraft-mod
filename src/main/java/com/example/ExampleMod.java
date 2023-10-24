package com.example;

import com.example.kingdom.Kingdom;
import com.example.npcs.ControlledPlayer;
import com.example.npcs.bare.CustomPlayer;
import com.example.server.GameProfileManager;
import com.example.util.LogManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

public class ExampleMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");
	public static GameProfileManager gameProfileManager;

	public ArrayList<ControlledPlayer> fakePlayers;

	private int timer = 0;
	private boolean executed = false;
	private Kingdom kingdom;


	private static int spawnAgent(ServerCommandSource source, String name, ArrayList<ControlledPlayer> list, ExampleMod mod) {
		GameProfile profile = new GameProfile(UUID.randomUUID(), name);
		gameProfileManager.addProfile(profile);
		ServerWorld world = source.getWorld();
		CustomPlayer fakePlayer = CustomPlayer.get(world, profile);

		PlayerListS2CPacket packet = new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, fakePlayer);
		source.getServer().getPlayerManager().sendToAll(packet);
		LOGGER.info("Sent PlayerListS2CPacket to all players.");

		world.spawnEntity(fakePlayer);
		LOGGER.info("world.spawnEntity executed.");

		fakePlayer.setPos(source.getPosition().x, source.getPosition().y, source.getPosition().z);
		LOGGER.info("fakePlayer.setPos executed.");


		ControlledPlayer controlledPlayer = new ControlledPlayer(fakePlayer);
		mod.kingdom.addCitizen(controlledPlayer);

		LOGGER.info("Created new controlled player");
		list.add(controlledPlayer);
		return 1;
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
			LOGGER.info("Server is stopping, saving Kingdom");
			kingdom.saveToDisk();
			LOGGER.info("Server is stopping, saving player data");
			for (ControlledPlayer fakePlayer: fakePlayers) {
				CustomPlayer player = fakePlayer.player;
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

			gameProfileManager =  new GameProfileManager(server, server.getOverworld());
			ServerTickEvents.END_SERVER_TICK.register(server1 -> {
				timer++;
				if (timer > 100 && !executed) { // Adjust this value to increase/decrease delay
					server.submit(() -> {
						executed = true;
						kingdom = Kingdom.getFromDisk(server, "Emerald", this);
					});
				}
				for (ControlledPlayer fakePlayer : fakePlayers) {
					fakePlayer.tick();
				}
			});
		});
	}

	public static int setRoleCommand(ServerCommandSource source, String agentName, String roleName, ArrayList<ControlledPlayer> fakePlayers) {
		LOGGER.info("setRoleCommand called");
		for (ControlledPlayer fakePlayer : fakePlayers) {
			if (fakePlayer.getUsername().equals(agentName)) {
				fakePlayer.setRole(roleName);
				return 1;
			}
		}
		LOGGER.info("No usernames match");
		return 0;
	}

	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->  {
				dispatcher.register(CommandManager.literal("king")
					.then(CommandManager.literal("newAgent")
							.then(CommandManager.argument("name", StringArgumentType.string())
									.executes(context -> {
										return spawnAgent(context.getSource(), StringArgumentType.getString(context, "name"), this.fakePlayers, this);
									})
							)
					)
					.then(CommandManager.literal("setRole")
							.then(CommandManager.argument("agentName", StringArgumentType.string())
									.then(CommandManager.argument("roleName", StringArgumentType.string())
										.executes(context -> {
											return setRoleCommand(
													context.getSource(),
													StringArgumentType.getString(context, "agentName"),
													StringArgumentType.getString(context, "roleName"),
													this.fakePlayers
											);
										})
									)
							)
					)
				);
				dispatcher.register(CommandManager.literal("logon")
						.then(CommandManager.argument("LOGGERID", StringArgumentType.string())
								.executes(context -> {
									String loggerID = StringArgumentType.getString(context, "LOGGERID");
									LogManager.logOn(loggerID);
									return 1; // Success code for command
								})));

				dispatcher.register(CommandManager.literal("logoff")
						.then(CommandManager.argument("LOGGERID", StringArgumentType.string())
								.executes(context -> {
									String loggerID = StringArgumentType.getString(context, "LOGGERID");
									LogManager.logOff(loggerID);
									return 1; // Success code for command
								})));

			}
		);
	}


	@Override
	public void onInitialize() {
		fakePlayers = new ArrayList<>();
		setupPlayerSave();
		greetOnJoin();
		loadNPCs();

		registerCommands();


		LOGGER.info("Hello Fabric world!");
	}
}