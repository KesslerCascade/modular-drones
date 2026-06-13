package rearth.blocks.controller;

import com.mojang.serialization.MapCodec;
import dev.architectury.networking.NetworkManager;
import org.jetbrains.annotations.Nullable;
import rearth.drone.DroneData;
import rearth.init.BlockEntitiesContent;
import rearth.init.ComponentContent;
import rearth.init.ItemContent;
import rearth.init.NetworkContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ControllerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ControllerBlock(Properties settings) {
        super(settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }
    
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ControllerBlockEntity(pos, state);
    }
    
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            var candidate = world.getBlockEntity(pos, BlockEntitiesContent.ASSEMBLER_CONTROLLER.get());
            candidate.ifPresent(controllerBlockEntity ->
                                  NetworkManager.sendToPlayer(serverPlayer, new NetworkContent.OpenDroneScreenPacket(pos))
            );
        }

        
        return InteractionResult.SUCCESS;
    }
    
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        if (stack.is(ItemContent.POCKET_DRONE.get()) && stack.has(ComponentContent.DRONE_DATA_TYPE.get())) {
            System.out.println("Loading pocket drone");

            var stackData = stack.get(ComponentContent.DRONE_DATA_TYPE.get());

            var candidate = world.getBlockEntity(pos, BlockEntitiesContent.ASSEMBLER_CONTROLLER.get());
            if (candidate.isPresent() && !world.isClientSide()) {
                var imported = candidate.get().loadDroneToWorld(stackData, player);
                if (imported) {
                    stack.shrink(1);
                    return InteractionResult.CONSUME;
                }
                return ItemInteractionResult.FAIL;
            }
        }

        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }
}
