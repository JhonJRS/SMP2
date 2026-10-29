package net.sirplop.aetherworks.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sirplop.aetherworks.network.NetworkEvent;

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.function.Supplier;

public class MessageSyncItemEntityTag {
    public int syncTarget;
    public String tag;

    public MessageSyncItemEntityTag(ItemEntity entity, String add)
    {
        syncTarget = entity.getId();
        tag = add;
    }
    public MessageSyncItemEntityTag(int ent, String add)
    {
        syncTarget = ent;
        tag = add;
    }

    public static void encode(MessageSyncItemEntityTag msg, FriendlyByteBuf buf)
    {
        buf.writeInt(msg.syncTarget);
        buf.writeInt(msg.tag.length());
        buf.writeCharSequence(msg.tag, Charset.defaultCharset());
    }

    public static MessageSyncItemEntityTag decode(FriendlyByteBuf buf) {

        return new MessageSyncItemEntityTag(buf.readInt(), buf.readCharSequence(buf.readInt(), Charset.defaultCharset()).toString());
    }

    public static void handle(MessageSyncItemEntityTag msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> {
                handleClient(msg);
            });
        }
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(MessageSyncItemEntityTag msg) {
        Entity ent = Minecraft.getInstance().level.getEntity(msg.syncTarget);
        if (ent instanceof ItemEntity) {
            ent.addTag(msg.tag);
        }
    }
}
