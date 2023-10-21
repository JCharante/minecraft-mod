package com.example;

import net.fabricmc.api.ClientModInitializer;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		//EntityRendererRegistry.register(ExampleMod.AGENT_ENTITY, AgentEntityRenderer::new);
	}
}