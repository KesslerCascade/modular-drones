package rearth;

import dev.architectury.impl.NetworkAggregator;
import dev.architectury.networking.NetworkManager;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import rearth.client.ui.DroneCreatorScreen;
import rearth.init.BlockContent;
import rearth.init.BlockEntitiesContent;
import rearth.init.NetworkContent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class DronesClient {
    
    public static Map<Supplier<Block>, RenderLayer> RENDER_LAYERS = new HashMap<>();  // this is used in client-specific parts to set render layers
    
    public static final HashMap<Integer, NetworkContent.DroneMoveSyncPacket> CURRENT_DATA = new HashMap<>();
    public static final HashMap<Integer, ItemStack> CARRIED_ITEMS = new HashMap<>();
    
    public static void init() {
        Drones.LOGGER.info("Hello from drones client");
        
        NetworkAggregator.registerReceiver(NetworkManager.Side.S2C, NetworkContent.OpenDroneScreenPacket.PAYLOAD_ID, NetworkContent.OpenDroneScreenPacket.PACKET_CODEC, List.of(), DronesClient::onAssembleScreenPacket);
        
        RENDER_LAYERS.put(BlockContent.WOOD_ROTOR, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.IRON_ROTOR, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.ION_THRUSTER, RenderLayer.getCutout());
        
    }
    
    public static void onAssembleScreenPacket(NetworkContent.OpenDroneScreenPacket packet, NetworkManager.PacketContext context) {
        var player = context.getPlayer();
        var world = player.getWorld();
        var pos = packet.controllerPos();
        var candidate = world.getBlockEntity(pos, BlockEntitiesContent.ASSEMBLER_CONTROLLER.get());
        candidate.ifPresent(controllerBlockEntity ->
                              MinecraftClient.getInstance().setScreen(new DroneCreatorScreen(candidate.get().getCurrentDroneData(), pos))
        );
    }
}
