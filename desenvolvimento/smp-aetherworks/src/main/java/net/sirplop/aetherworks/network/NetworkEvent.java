package net.sirplop.aetherworks.network;

import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** SMP 2 port: the bits of Forge 1.20's NetworkEvent.Context the messages use, over NeoForge's IPayloadContext. */
public final class NetworkEvent {
    private NetworkEvent() {}

    public static final class Context {
        private final IPayloadContext ctx;

        Context(IPayloadContext ctx) {
            this.ctx = ctx;
        }

        public Direction getDirection() {
            return new Direction(ctx.flow() == PacketFlow.CLIENTBOUND ? LogicalSide.CLIENT : LogicalSide.SERVER);
        }

        public void enqueueWork(Runnable work) {
            ctx.enqueueWork(work);
        }

        public void setPacketHandled(boolean handled) {}

        public ServerPlayer getSender() {
            return ctx.player() instanceof ServerPlayer sp ? sp : null;
        }
    }

    public record Direction(LogicalSide receptionSide) {
        public LogicalSide getReceptionSide() {
            return receptionSide;
        }
    }
}
