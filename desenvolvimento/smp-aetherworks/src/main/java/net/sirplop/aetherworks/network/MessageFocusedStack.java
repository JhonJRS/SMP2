package net.sirplop.aetherworks.network;
import net.sirplop.aetherworks.util.Compat121;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.sirplop.aetherworks.network.NetworkEvent;
import net.sirplop.aetherworks.Aetherworks;

import java.util.function.Supplier;

public class MessageFocusedStack {
    public static final String FOCUS_TAG = "aw.focus";

    public ItemStack held;
    public ItemStack focus;
    public MessageFocusedStack(ItemStack held, ItemStack focus)
    {
        this.held = held;
        this.focus = focus;
    }

    public static void encode(MessageFocusedStack msg, FriendlyByteBuf buf)
    {
        Compat121.writeItem(buf, msg.held);
        Compat121.writeItem(buf, msg.focus);
    }

    public static MessageFocusedStack decode(FriendlyByteBuf buf) {

        return new MessageFocusedStack(Compat121.readItem(buf), Compat121.readItem(buf));
    }

    public static void handle(MessageFocusedStack msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> {
                setFocus(msg.held, msg.focus);
            });
        }
        ctx.get().setPacketHandled(true);
    }

    public static void setFocus(ItemStack held, ItemStack focus) {
        if(!held.isEmpty()) {
            if (focus.isEmpty())
                Compat121.edit(held, tag -> tag.remove(FOCUS_TAG));
            else
                Compat121.edit(held, tag -> tag.put(FOCUS_TAG, Compat121.save(focus)));
        }
    }
}
