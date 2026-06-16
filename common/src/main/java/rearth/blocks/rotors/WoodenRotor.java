package rearth.blocks.rotors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WoodenRotor extends Block {

    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Half> HALF = BlockStateProperties.HALF;

    public WoodenRotor(Properties settings) {
        super(settings);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HALF);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var direction = context.getClickedFace();
        var clickedPos = context.getClickedPos();
        var half = direction != Direction.DOWN && (direction == Direction.UP || context.getClickLocation().y - (double) clickedPos.getY() <= 0.5)
          ? Half.BOTTOM
          : Half.TOP;
        return defaultBlockState().setValue(HALF, half);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (state.getValue(HALF) == Half.BOTTOM)
            return Shapes.box(0, 0, 0, 1, 6 / 16f, 1);
        return Shapes.box(0, 10 / 16f, 0, 1, 1f, 1);
    }
}
