package rearth.drone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import rearth.util.Helpers;

public record RecordedBlock(BlockState state, Vec3i localPos) {
    
    public static final Codec<RecordedBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      BlockState.CODEC.fieldOf("b").forGetter(RecordedBlock::state),
      Vec3i.CODEC.fieldOf("p").forGetter(RecordedBlock::localPos)
    ).apply(instance, RecordedBlock::new));
    
    public static final StreamCodec<ByteBuf, RecordedBlock> PACKET_CODEC = StreamCodec.composite(
      ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY),
      RecordedBlock::state,
      Helpers.VEC3I_PACKET_CODEC,
      RecordedBlock::localPos,
      RecordedBlock::new
    );
    
}
