package rearth.client.ui;

import dev.architectury.networking.NetworkManager;
import org.jetbrains.annotations.Nullable;
import rearth.Drones;
import rearth.blocks.controller.ControllerBlockEntity;
import rearth.drone.DroneData;
import rearth.drone.RecordedBlock;
import rearth.drone.behaviour.DroneBehaviour;
import rearth.drone.behaviour.DroneBehaviour.BlockFunctions;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DroneCreatorScreen extends Screen {
    
    private static final ResourceLocation BACKGROUND_TEXTURE = Drones.id("textures/gui/assembler.png");
    
    private static final ResourceLocation BIG_BUTTON_TEXTURE = Drones.id("textures/gui/big_button.png");
    private static final ResourceLocation BIG_BUTTON_HOVER_TEXTURE = Drones.id("textures/gui/big_button_hover.png");
    private static final ResourceLocation BIG_BUTTON_PRESSED_TEXTURE = Drones.id("textures/gui/big_button_pressed.png");
    
    private static final ResourceLocation SLOT_PANEL_TEXTURE = Drones.id("textures/gui/slot_panel.png");
    
    private final DroneData droneData;
    private final HashMap<Vec3i, BlockEntity> renderedEntities = new HashMap<>();
    private float openTime = 0;
    private final BlockPos machinePos;
    
    private float previewAngle = 0;
    private EditBox nameField;
    
    public DroneCreatorScreen(DroneData data, BlockPos machinePos) {
        super(Component.empty());
        this.machinePos = machinePos;
        
        if (data == null)
            data = new DroneData(List.of(), 1, Vec3i.ZERO);
        
        this.droneData = data;
        
        
        for (var pair : droneData.getBlocks()) {
            var state = pair.state();
            if (state.getBlock() instanceof EntityBlock blockEntityProvider) {
                var blockEntity = blockEntityProvider.newBlockEntity(new BlockPos(pair.localPos()), state);
                if (blockEntity != null && this.minecraft != null) {
                    blockEntity.setLevel(this.minecraft.level);
                    var renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity);
                    if (renderer != null) {
                        renderedEntities.put(pair.localPos(), blockEntity);
                    }
                }
            }
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        var centerX = this.width / 2;
        var centerY = this.height / 2;
        var backgroundStartX = centerX - (300 / 2);
        var backgroundStartY = centerY - (183 / 2);
        var buttonX = backgroundStartX + 154;
        var buttonY = backgroundStartY + 113;
        var nameX = backgroundStartX + 7;
        var nameY = backgroundStartY + 140;
        
        var buttonWidget = new BigDroneButton(buttonX, buttonY, 138, 59, Component.literal("BUILD!").withStyle(ChatFormatting.BOLD), button -> {
        }, this);
        
        this.addRenderableWidget(buttonWidget);
        
        nameField = new EditBox(this.font, nameX, nameY, 138, 32, Component.literal("Input"));
        nameField.setMaxLength(32);
        nameField.setTooltip(Tooltip.create(Component.translatable("tooltip.drones.name_field")));
        nameField.setValue("Dronie");
        this.addRenderableWidget(nameField);
        
    }
    
    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        var textColor = 0xFF000000 | 13685204;
        
        var centerX = this.width / 2;
        var centerY = this.height / 2;
        var backgroundStartX = centerX - (300 / 2);
        var backgroundStartY = centerY - (183 / 2);
        
        context.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, backgroundStartX, backgroundStartY, 0.0F, 0.0F, 300, 183, 300, 183);

        /* Disabled pending 1.21.11 rewrite
        for (var pair : droneData.getBlocks()) {
            var entity = renderedEntities.get(pair.localPos());
            renderBlock(context, pair.localPos(), pair.state(), entity, delta);
        } */
        
        for (var drawable : this.renderables) {
            drawable.render(context, mouseX, mouseY, delta);
        }
        
        context.drawString(this.font, Component.literal("Speed:"), backgroundStartX + 161, backgroundStartY + 13, textColor, false);
        context.drawString(this.font, Component.literal("Size:"), backgroundStartX + 161, backgroundStartY + 44, textColor, false);
        
        // render bars
        var greenColor = -12810969;
        var orangeColor = -1012726;
        var markerColor = -3092012;
        var endColor = -526345;
        
        // speed bar
        var speedBarFill = getSpeedProgress();
        var speedColor = speedBarFill > 0.25 ? greenColor : orangeColor;
        drawBarPart(context, backgroundStartX, backgroundStartY, 0, speedBarFill, speedColor, 5, 0);
        drawBarPart(context, backgroundStartX, backgroundStartY, 0.248f, 0.252f, markerColor, 3, 1);
        drawBarPart(context, backgroundStartX, backgroundStartY, 0.498f, 0.502f, markerColor, 3, 1);
        drawBarPart(context, backgroundStartX, backgroundStartY, 0.748f, 0.752f, markerColor, 3, 1);
        drawBarPart(context, backgroundStartX, backgroundStartY, speedBarFill - 0.005f, speedBarFill + 0.005f, endColor, 5, 0);
        
        // size bar
        var sizeBarFill = getSizeProgress();
        var sizeColor = sizeBarFill < 0.75 ? greenColor : orangeColor;
        drawBarPart(context, backgroundStartX, backgroundStartY, 0, sizeBarFill, sizeColor, 5, 32);
        drawBarPart(context, backgroundStartX, backgroundStartY, 0.248f, 0.252f, markerColor, 3, 32 + 1);
        drawBarPart(context, backgroundStartX, backgroundStartY, 0.498f, 0.502f, markerColor, 3, 32 + 1);
        drawBarPart(context, backgroundStartX, backgroundStartY, 0.748f, 0.752f, markerColor, 3, 32 + 1);
        drawBarPart(context, backgroundStartX, backgroundStartY, sizeBarFill - 0.005f, sizeBarFill + 0.005f, endColor, 5, 32);
        
        // installed behaviours
        var abilitiesStartX = backgroundStartX + 159;
        var abilitiesStartY = backgroundStartY + 80;
        var index = 0;
        
        for (var ability : droneData.installed) {
            
            if (ability.equals(DroneBehaviour.BlockFunctions.FLIGHT)) continue;
            
            var startAtX = abilitiesStartX + index * (20 + 3);
            
            context.blit(RenderPipelines.GUI_TEXTURED, SLOT_PANEL_TEXTURE, startAtX, abilitiesStartY, 0.0F, 0.0F, 20, 20, 20, 20);
            
            var isHovered = mouseX > startAtX && mouseX < startAtX + 20 && mouseY > abilitiesStartY && mouseY < abilitiesStartY + 20;
            if (isHovered) {

                var tooltipLines = new java.util.ArrayList<Component>();
                tooltipLines.add(Component.translatable("drones.ability." + ability.name().toLowerCase()));

                for (int i = 0; i < 5; i++) {
                    var tooltipKey = "drones.ability." + ability.name().toLowerCase() + "." + i;
                    if (I18n.exists(tooltipKey)) {
                        tooltipLines.add(Component.translatable(tooltipKey));
                    }
                }

                context.setTooltipForNextFrame(this.font, tooltipLines, Optional.empty(), startAtX, abilitiesStartY + 40);

            }
            
            var renderedItem = DroneBehaviour.getItem(ability);
            context.renderFakeItem(new ItemStack(renderedItem), startAtX + 2, abilitiesStartY + 2);
            
            index++;
        }
        
        if (index == 0) {
            context.drawString(this.font, Component.literal("No Abilities"), abilitiesStartX + 5, abilitiesStartY + 7, textColor, false);
        }
        
        this.openTime += delta;
        
        
    }
    
    private void drawBarPart(GuiGraphics context, int backgroundStartX, int backgroundStartY, float fillStart, float fillEnd, int color, int height, int yOffset) {
        var speedFromX = backgroundStartX + 161 + (int) (124 * fillStart);
        var speedFromY = backgroundStartY + 26 + yOffset;
        var speedToX = backgroundStartX + 161 + (int) (124 * fillEnd);
        var speedToY = speedFromY + height;
        context.fill(speedFromX, speedFromY, speedToX, speedToY, color);
    }

    /* 3D rendered preview is temporarily disabled pending a full rewrite in 1.21.11 for a PictureInPictureRenderer
     * mixin implementation.
     *
    private void renderBlock(GuiGraphics context, Vec3i offset, BlockState state, @Nullable BlockEntity entity, float partialTicks) {

        var x = this.width / 2 - (300 / 2) + 70;
        var y = this.height / 2 - 30;

        var size = 20;
        var rotation = (openTime * 2) % 360;

        var scale = droneData.getRenderScale();

        var matrices = new PoseStack();

        matrices.translate(x + size / 2f, y + size / 2f, 400);
        matrices.scale(40 * size / 64f, -40 * size / 64f, 40);
        matrices.scale(scale, scale, scale);

        matrices.mulPose(Axis.XP.rotationDegrees(30 + previewAngle));
        matrices.mulPose(Axis.YP.rotationDegrees(45 + 180 + rotation));

        matrices.translate(-.5 + offset.getX(), -.5 + offset.getY(), -.5 + offset.getZ());

        final var vertexConsumers = minecraft.renderBuffers().bufferSource();
        this.minecraft.getBlockRenderer().renderSingleBlock(
          state, matrices, vertexConsumers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
        );

        if (entity != null) {
            var dispatcher = this.minecraft.getBlockEntityRenderDispatcher();
            var entityRenderer = dispatcher.getRenderer(entity);
            if (entityRenderer != null) {
                dispatcher.prepare(this.minecraft.gameRenderer.getMainCamera());
                var renderState = dispatcher.tryExtractRenderState(entity, partialTicks, null);
                if (renderState != null) {
                    var collector = new DroneRenderer.ImmediateSubmitNodeCollector(vertexConsumers);
                    dispatcher.submit(renderState, matrices, collector, new CameraRenderState());
                }
            }
        }

        vertexConsumers.endBatch();
        this.minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
    }

    private static void drawItem(GuiGraphics context, Item item, int x, int y, int size) {
        var client = Minecraft.getInstance();
        var entityBuffers = client.renderBuffers().bufferSource();
        var stack = new ItemStack(item);

        var itemRenderState = new ItemStackRenderState();
        client.getItemModelResolver().updateForTopItem(itemRenderState, stack, ItemDisplayContext.GUI, client.level, null, 0);

        final boolean notSideLit = !itemRenderState.usesBlockLight();
        if (notSideLit) {
            client.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        }

        var matrices = new PoseStack();

        // Translate to the root of the component
        matrices.translate(x, y, 100);

        // Scale according to component size and translate to the center
        matrices.scale(size / 16f, size / 16f, 1);
        matrices.translate(8.0, 8.0, 0.0);

        // Vanilla scaling and y inversion
        matrices.scale(16, -16, 16);

        var collector = new DroneRenderer.ImmediateSubmitNodeCollector(entityBuffers);
        itemRenderState.submit(matrices, collector, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
        entityBuffers.endBatch();

        if (notSideLit) {
            client.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        }
    }
    */

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double deltaX, double deltaY) {

        var mouseX = event.x();
        var mouseY = event.y();

        if (Math.abs(deltaY) > 0.001) {
            var centerX = this.width / 2;
            var centerY = this.height / 2;
            var backgroundStartX = centerX - (300 / 2);
            var backgroundStartY = centerY - (183 / 2);

            var previewStartX = backgroundStartX + 5;
            var previewEndX = previewStartX + 140;
            var previewStartY = backgroundStartY + 5;
            var previewEndY = previewStartY + 170;

            if (mouseX < previewEndX && mouseX > previewStartX && mouseY < previewEndY && mouseY > previewStartY) {
                previewAngle += deltaY;
            }

        }

        return super.mouseDragged(event, deltaX, deltaY);
    }
    
    private float getSpeedProgress() {
        return Math.clamp(droneData.power / 10f, 0.01f, 1);
    }
    
    private float getSizeProgress() {
        return Math.clamp(droneData.getSize() / 10f, 0.1f, 1);
    }
    
    public void assembleDrone() {
        
        if (!this.droneData.isValid()) {
            this.minecraft.getToastManager().addToast(  // says "assembled, drone has been added to inv"
              SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("drone.message.invalid_drone"), Component.translatable("drone.message.invalid_drone_desc"))
            );
            
            return;
        }
        
        this.minecraft.getToastManager().addToast(  // says "assembled, drone has been added to inv"
          SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("drone.message.assembled"), Component.translatable("drone.message.assembled_desc"))
        );
        
        NetworkManager.sendToServer(new ControllerBlockEntity.AssembleDronePacket(nameField.getValue(), machinePos));
        
        this.onClose();
    }
    
    private static class BigDroneButton extends Button {
        
        private boolean isPressed = false;
        private final DroneCreatorScreen parent;
        
        protected BigDroneButton(int x, int y, int width, int height, Component message, OnPress onPress, DroneCreatorScreen parent) {
            super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
            this.parent = parent;
        }
        
        @Override
        public void onPress(net.minecraft.client.input.InputWithModifiers input) {
            isPressed = true;

            super.onPress(input);
        }
        
        @Override
        public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
            var valid = super.mouseReleased(event);
            if (valid && isPressed) {
                isPressed = false;
                this.parent.assembleDrone();
                playUpSound(Minecraft.getInstance().getSoundManager());
            }
            
            return valid;
        }
        
        @Override
        public void playDownSound(SoundManager soundManager) {
            soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NETHER_WOOD_PRESSURE_PLATE_CLICK_ON, 0.7F));
        }
        
        public void playUpSound(SoundManager soundManager) {
            soundManager.play(SimpleSoundInstance.forUI(SoundEvents.BEACON_DEACTIVATE, 0.7F));
        }
        
        @SuppressWarnings("lossy-conversions")
        @Override
        protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
            
            var usedTexture = BIG_BUTTON_TEXTURE;
            
            if (this.isHovered())
                usedTexture = BIG_BUTTON_HOVER_TEXTURE;
            
            if (isPressed)
                usedTexture = BIG_BUTTON_PRESSED_TEXTURE;
            
            
            context.blit(RenderPipelines.GUI_TEXTURED, usedTexture, this.getX(), this.getY(), 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
            
            var scale = 3.4f;
            
            var textX = this.getX() + 11;
            var textY = this.getY() + 13;
            
            var textColor = 0xFF000000 | 13685204;
            
            if (this.isHovered())
                textY += 4;
            
            if (this.isPressed)
                textY += 6;
            
            
            context.pose().pushMatrix();
            context.pose().scale(scale, scale);

            textX /= scale;
            textY /= scale;

            context.drawString(Minecraft.getInstance().font, this.getMessage(), textX, textY, textColor, false);

            context.pose().popMatrix();
        }
    }
}
