package com.example;

//class HeldItemFeatureRenderer extends BipedEntityModel<AgentEntity> {
//    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");
//
//    public HeldItemFeatureRenderer(EntityRendererFactory.Context context) {
//        super(context.getPart(EntityModelLayers.PLAYER));
//    }
//
//    @Override
//    public void setAngles(AgentEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
//        ItemStack mainHand = entity.getEquippedStack(EquipmentSlot.MAINHAND);
//        LOGGER.info("Rendering Agent with item: " + mainHand);
//        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
//    }
//}
//
//public class AgentEntityRenderer extends MobEntityRenderer<AgentEntity, PlayerEntityModel<AgentEntity>> {
//    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");
//    int timer = 0;
//
//    public AgentEntityRenderer(EntityRendererFactory.Context context) {
//        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5f);
//        model.leftArm.visible = true;
//        model.rightArm.visible = true;
//    }
//
//    @Override
//    public Identifier getTexture(AgentEntity entity) {
//        return new Identifier("mymod", "textures/entity/steve.png");
//    }
//
//    @Override
//    public void render(AgentEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
//        if (timer++ > 60) {
//            ItemStack mainHand = entity.getEquippedStack(EquipmentSlot.MAINHAND);
//            LOGGER.info("Rendering Agent with item: " + mainHand);
//            timer = 0;
//        }
//        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
//    }
//
//}
