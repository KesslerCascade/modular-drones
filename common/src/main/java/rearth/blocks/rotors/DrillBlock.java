package rearth.blocks.rotors;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DrillBlock extends Block {
    public DrillBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return makeShape();
    }
    
    public VoxelShape makeShape() {	
        VoxelShape shape = Shapes.empty();
        shape = Shapes.or(shape, Shapes.box(0, 0.125, 0, 1, 0.875, 0.1875));
        shape = Shapes.or(shape, Shapes.box(0.1875, 0.1875, 0.1875, 0.8125, 0.8125, 0.375));
        shape = Shapes.or(shape, Shapes.box(0.25, 0.25, 0.375, 0.75, 0.75, 0.5625));
        shape = Shapes.or(shape, Shapes.box(0.3125, 0.3125, 0.5625, 0.6875, 0.6875, 0.75));
        shape = Shapes.or(shape, Shapes.box(0.375, 0.375, 0.75, 0.625, 0.625, 0.875));
        shape = Shapes.or(shape, Shapes.box(0.4375, 0.4375, 0.875, 0.5625, 0.5625, 1));
        
        return shape;
    }
}
