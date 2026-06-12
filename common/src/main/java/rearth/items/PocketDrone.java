package rearth.items;

import rearth.drone.DroneData;
import rearth.drone.behaviour.DroneBehaviour.BlockFunctions;
import rearth.init.ComponentContent;
import java.util.EnumSet;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class PocketDrone extends Item {

    public PocketDrone(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag type) {

        if (!stack.has(ComponentContent.DRONE_DATA_TYPE.get())) {
            super.appendHoverText(stack, context, tooltipDisplay, tooltip, type);
            return;
        }

        var data = stack.get(ComponentContent.DRONE_DATA_TYPE.get());

        var speed = String.format("%.1f", data.power);
        var blocks = data.getBlocks().size();
        var abilities = data.installed;
        var size = data.getSize();

        tooltip.accept(Component.translatable("tooltip.drones.data_speed", speed));
        tooltip.accept(Component.translatable("tooltip.drones.block_count", blocks));
        tooltip.accept(Component.translatable("tooltip.drones.data_size", size));
        tooltip.accept(Component.translatable("tooltip.drones.abilities_heading"));

        for (var ability : abilities) {
            tooltip.accept(Component.literal(" - ").append(Component.translatable("drones.ability." + ability.name().toLowerCase())).withStyle(ChatFormatting.ITALIC));
        }

        tooltip.accept(Component.literal(""));
        tooltip.accept(Component.translatable("tooltip.drones.equip_hint").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));


        super.appendHoverText(stack, context, tooltipDisplay, tooltip, type);
    }
}
