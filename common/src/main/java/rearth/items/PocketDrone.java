package rearth.items;

import rearth.drone.DroneData;
import rearth.drone.behaviour.DroneBehaviour.BlockFunctions;
import rearth.init.ComponentContent;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class PocketDrone extends Item implements Equipable {
    
    public PocketDrone(Properties settings) {
        super(settings);
    }
    
    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        
        if (!stack.has(ComponentContent.DRONE_DATA_TYPE.get())) {
            super.appendHoverText(stack, context, tooltip, type);
            return;
        }
        
        var data = stack.get(ComponentContent.DRONE_DATA_TYPE.get());
        
        var speed = String.format("%.1f", data.power);
        var blocks = data.getBlocks().size();
        var abilities = data.installed;
        var size = data.getSize();
        
        tooltip.add(Component.translatable("tooltip.drones.data_speed", speed));
        tooltip.add(Component.translatable("tooltip.drones.block_count", blocks));
        tooltip.add(Component.translatable("tooltip.drones.data_size", size));
        tooltip.add(Component.translatable("tooltip.drones.abilities_heading"));
        
        for (var ability : abilities) {
            tooltip.add(Component.literal(" - ").append(Component.translatable("drones.ability." + ability.name().toLowerCase())).withStyle(ChatFormatting.ITALIC));
        }
        
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("tooltip.drones.equip_hint").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        
        
        super.appendHoverText(stack, context, tooltip, type);
    }
}
