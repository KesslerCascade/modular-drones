package rearth.init;

import dev.architectury.impl.NetworkAggregator;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import rearth.Drones;
import rearth.DronesClient;
import rearth.blocks.controller.ControllerBlockEntity;
import rearth.util.Helpers;

import java.util.List;

public class NetworkContent {
    
    public static void init() {
        
        NetworkContent.registerS2C(DroneMoveSyncPacket.PAYLOAD_ID, DroneMoveSyncPacket.PACKET_CODEC, ((value, context) -> DronesClient.CURRENT_DATA.put(value.droneId(), value)));
        
        if (Platform.getEnvironment().equals(Env.SERVER)) {
            NetworkAggregator.registerS2CType(OpenDroneScreenPacket.PAYLOAD_ID, OpenDroneScreenPacket.PACKET_CODEC, List.of());
            NetworkAggregator.registerS2CType(DroneCarriedItemPacket.PAYLOAD_ID, DroneCarriedItemPacket.PACKET_CODEC,
                    List.of());
        } else {
            NetworkAggregator.registerReceiver(NetworkManager.Side.S2C, DroneCarriedItemPacket.PAYLOAD_ID,
                    DroneCarriedItemPacket.PACKET_CODEC, List.of(),
                    (value, context) -> DronesClient.CARRIED_ITEMS.put(value.droneId(), value.carriedItem()));
        }
        
        NetworkContent.registerC2S(ControllerBlockEntity.AssembleDronePacket.PAYLOAD_ID, ControllerBlockEntity.AssembleDronePacket.PACKET_CODEC, (((value, context) -> {
            
            var world = context.getPlayer().getWorld();
            var player = context.getPlayer();
            var candidate = world.getBlockEntity(value.controllerPos(), BlockEntitiesContent.ASSEMBLER_CONTROLLER.get());
            candidate.ifPresent(controllerBlockEntity -> controllerBlockEntity.assembleDrone(player, value.name()));
            
        })));
        
    }
    
    private static <T extends CustomPayload> void registerS2C(
      CustomPayload.Id<T> dataPayloadId,
      PacketCodec<ByteBuf, T> packetCodec,
      NetworkManager.NetworkReceiver<T> receiver) {
        
        if (Platform.getEnvironment().equals(Env.SERVER)) {
            NetworkAggregator.registerS2CType(dataPayloadId, packetCodec, List.of());
        } else {
            NetworkAggregator.registerReceiver(NetworkManager.Side.S2C, dataPayloadId, packetCodec, List.of(), receiver);
        }
        
    }
    
    private static <T extends CustomPayload> void registerC2S(
      CustomPayload.Id<T> dataPayloadId,
      PacketCodec<ByteBuf, T> packetCodec,
      NetworkManager.NetworkReceiver<T> receiver) {
        
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, dataPayloadId, packetCodec, receiver);
        
    }
    
    public record OpenDroneScreenPacket(BlockPos controllerPos) implements CustomPayload {
        
        public static final CustomPayload.Id<OpenDroneScreenPacket> PAYLOAD_ID = new CustomPayload.Id<>(Drones.id("open_screen"));
        
        public static final PacketCodec<ByteBuf, OpenDroneScreenPacket> PACKET_CODEC = PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          OpenDroneScreenPacket::controllerPos,
          OpenDroneScreenPacket::new
        );
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return PAYLOAD_ID;
        }
    }
    
    
    public record DroneMoveSyncPacket(Vec3d position, Vec3d rotation, int droneId) implements CustomPayload {
        
        public static final CustomPayload.Id<DroneMoveSyncPacket> PAYLOAD_ID = new CustomPayload.Id<>(Drones.id("move"));
        
        public static final PacketCodec<ByteBuf, DroneMoveSyncPacket> PACKET_CODEC = PacketCodec.tuple(
          Helpers.VEC3D_PACKET_CODEC, DroneMoveSyncPacket::position,
          Helpers.VEC3D_PACKET_CODEC, DroneMoveSyncPacket::rotation,
          PacketCodecs.INTEGER, DroneMoveSyncPacket::droneId,
          DroneMoveSyncPacket::new
        );
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return PAYLOAD_ID;
        }
    }
    
    public record DroneCarriedItemPacket(int droneId, ItemStack carriedItem) implements CustomPayload {

        public static final CustomPayload.Id<DroneCarriedItemPacket> PAYLOAD_ID = new CustomPayload.Id<>(
                Drones.id("carried_item"));

        public static final PacketCodec<RegistryByteBuf, DroneCarriedItemPacket> PACKET_CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, DroneCarriedItemPacket::droneId,
                ItemStack.OPTIONAL_PACKET_CODEC, DroneCarriedItemPacket::carriedItem,
                DroneCarriedItemPacket::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return PAYLOAD_ID;
        }
    }

}
