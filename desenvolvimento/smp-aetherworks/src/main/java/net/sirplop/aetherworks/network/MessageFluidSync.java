package net.sirplop.aetherworks.network;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.util.Compat121;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.sirplop.aetherworks.network.NetworkEvent;

import java.util.function.Supplier;

public class MessageFluidSync {

    public ItemStack held;
    public FluidStack fluid;
    public int capacity;

    public MessageFluidSync(ItemStack held, FluidStack focus, int capacity)
    {
        this.held = held;
        this.fluid = focus;
        this.capacity = capacity;
    }

    public static void encode(MessageFluidSync msg, FriendlyByteBuf buf)
    {
        Compat121.writeItem(buf, msg.held);
        Compat121.writeFluid(buf, msg.fluid);
        buf.writeInt(msg.capacity);
    }

    public static MessageFluidSync decode(FriendlyByteBuf buf) {

        return new MessageFluidSync(Compat121.readItem(buf), Compat121.readFluid(buf), buf.capacity());
    }

    public static void handle(MessageFluidSync msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> {
                FluidHandlerItemStack stack = new FluidHandlerItemStack(AWRegistry.FLUID_CONTENTS, msg.held, msg.capacity);
                setFluid(stack, msg.fluid);
            });
        }
        ctx.get().setPacketHandled(true);
    }

    public static void setFluid(FluidHandlerItemStack held, FluidStack focus) {
        if(!held.getContainer().isEmpty()) {
            if (focus.isEmpty()) {
                held.drain(held.getTankCapacity(1), IFluidHandler.FluidAction.EXECUTE);
            }
            else {
                held.drain(held.getFluidInTank(1).getAmount(), IFluidHandler.FluidAction.EXECUTE);
                held.fill(focus, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }
}
