package net.sirplop.aetherworks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.util.LogicalSidedProvider;
import net.neoforged.fml.LogicalSide;
import net.sirplop.aetherworks.network.NetworkEvent;
import net.sirplop.aetherworks.api.capabilities.IAetheriometerCap;
import net.sirplop.aetherworks.capabilities.AetheriometerChunk;
import net.sirplop.aetherworks.capabilities.AetheriometerChunkCapability;

import java.util.function.Supplier;

public class MessageSyncAetheriometer {
    private final ChunkPos pos;
    private final int set;

    public MessageSyncAetheriometer(ChunkPos pos, int set) {
        this.pos = pos;
        this.set = set;
    }

    public static void encode(MessageSyncAetheriometer msg, FriendlyByteBuf buf) {
        buf.writeChunkPos(msg.pos);
        buf.writeInt(msg.set);
    }
    public static MessageSyncAetheriometer decode(FriendlyByteBuf buf) {
        return new MessageSyncAetheriometer(buf.readChunkPos(), buf.readInt());
    }

    public static void handle(MessageSyncAetheriometer msg, Supplier<NetworkEvent.Context> ctx) {

        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> {
                final var optionalLevel = LogicalSidedProvider.CLIENTWORLD.get(LogicalSide.CLIENT);

                optionalLevel.ifPresent(world -> {
                    final IAetheriometerCap data = AetheriometerChunkCapability
                            .getData(world, msg.pos)
                            .orElseThrow();

                    if (data instanceof final AetheriometerChunk chunk) {
                        chunk.setDataNoUpdate(msg.set);
                    }
                });
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
