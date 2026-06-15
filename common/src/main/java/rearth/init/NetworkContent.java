package rearth.init;

import dev.architectury.impl.NetworkAggregator;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.ByteBuf;
import rearth.Drones;
import rearth.DronesClient;
import rearth.blocks.controller.ControllerBlockEntity;
import rearth.util.Helpers;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

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
            
            var world = context.getPlayer().level();
            var player = context.getPlayer();
            var candidate = world.getBlockEntity(value.controllerPos(), BlockEntitiesContent.ASSEMBLER_CONTROLLER.get());
            candidate.ifPresent(controllerBlockEntity -> controllerBlockEntity.assembleDrone(player, value.name()));
            
        })));
        
    }
    
    private static <T extends CustomPacketPayload> void registerS2C(
      CustomPacketPayload.Type<T> dataPayloadId,
      StreamCodec<ByteBuf, T> packetCodec,
      NetworkManager.NetworkReceiver<T> receiver) {
        
        if (Platform.getEnvironment().equals(Env.SERVER)) {
            NetworkAggregator.registerS2CType(dataPayloadId, packetCodec, List.of());
        } else {
            NetworkAggregator.registerReceiver(NetworkManager.Side.S2C, dataPayloadId, packetCodec, List.of(), receiver);
        }
        
    }
    
    private static <T extends CustomPacketPayload> void registerC2S(
      CustomPacketPayload.Type<T> dataPayloadId,
      StreamCodec<ByteBuf, T> packetCodec,
      NetworkManager.NetworkReceiver<T> receiver) {
        
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, dataPayloadId, packetCodec, receiver);
        
    }
    
    public record OpenDroneScreenPacket(BlockPos controllerPos, String lastDroneName) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<OpenDroneScreenPacket> PAYLOAD_ID = new CustomPacketPayload.Type<>(Drones.id("open_screen"));

        public static final StreamCodec<ByteBuf, OpenDroneScreenPacket> PACKET_CODEC = StreamCodec.composite(
          BlockPos.STREAM_CODEC,
          OpenDroneScreenPacket::controllerPos,
          ByteBufCodecs.STRING_UTF8,
          OpenDroneScreenPacket::lastDroneName,
          OpenDroneScreenPacket::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PAYLOAD_ID;
        }
    }
    
    
    public record DroneMoveSyncPacket(Vec3 position, Vec3 rotation, int droneId) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<DroneMoveSyncPacket> PAYLOAD_ID = new CustomPacketPayload.Type<>(Drones.id("move"));
        
        public static final StreamCodec<ByteBuf, DroneMoveSyncPacket> PACKET_CODEC = StreamCodec.composite(
          Helpers.VEC3D_PACKET_CODEC, DroneMoveSyncPacket::position,
          Helpers.VEC3D_PACKET_CODEC, DroneMoveSyncPacket::rotation,
          ByteBufCodecs.INT, DroneMoveSyncPacket::droneId,
          DroneMoveSyncPacket::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PAYLOAD_ID;
        }
    }
    
    public record DroneCarriedItemPacket(int droneId, ItemStack carriedItem) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<DroneCarriedItemPacket> PAYLOAD_ID = new CustomPacketPayload.Type<>(
                Drones.id("carried_item"));

        public static final StreamCodec<RegistryFriendlyByteBuf, DroneCarriedItemPacket> PACKET_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, DroneCarriedItemPacket::droneId,
                ItemStack.OPTIONAL_STREAM_CODEC, DroneCarriedItemPacket::carriedItem,
                DroneCarriedItemPacket::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PAYLOAD_ID;
        }
    }

}
