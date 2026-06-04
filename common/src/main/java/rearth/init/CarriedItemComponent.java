package rearth.init;

import com.mojang.serialization.Codec;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.Objects;

/**
 * Wrapper around ItemStack for use as a DataComponentType value.
 * NeoForge requires DataComponent values to implement equals and hashCode.
 */
public record CarriedItemComponent(ItemStack stack) {

    public static final Codec<CarriedItemComponent> CODEC =
            ItemStack.CODEC.xmap(CarriedItemComponent::new, CarriedItemComponent::stack);

    public static final PacketCodec<RegistryByteBuf, CarriedItemComponent> PACKET_CODEC =
            ItemStack.PACKET_CODEC.xmap(CarriedItemComponent::new, CarriedItemComponent::stack);

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CarriedItemComponent c)) return false;
        return ItemStack.areItemsAndComponentsEqual(stack, c.stack)
                && stack.getCount() == c.stack.getCount();
    }

    @Override
    public int hashCode() {
        if (stack.isEmpty()) return 0;
        return Objects.hash(stack.getItem(), stack.getCount(), stack.getComponents());
    }
}
