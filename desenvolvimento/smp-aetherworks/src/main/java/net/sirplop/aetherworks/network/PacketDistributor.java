package net.sirplop.aetherworks.network;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** SMP 2 port: Forge 1.20's PacketDistributor targets (PLAYER, NEAR, TRACKING_CHUNK) on top of NeoForge 1.21's. */
public final class PacketDistributor<T> {
    public static final PacketDistributor<ServerPlayer> PLAYER = new PacketDistributor<>((player, payload) ->
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload));
    public static final PacketDistributor<TargetPoint> NEAR = new PacketDistributor<>((p, payload) -> {
        var server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel level = server == null ? null : server.getLevel(p.dim());
        if (level != null)
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(level, p.excluded(), p.x(), p.y(), p.z(), p.r(), payload);
    });
    public static final PacketDistributor<LevelChunk> TRACKING_CHUNK = new PacketDistributor<>((chunk, payload) -> {
        if (chunk.getLevel() instanceof ServerLevel level)
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(level, chunk.getPos(), payload);
    });

    private final java.util.function.BiConsumer<T, CustomPacketPayload> sender;

    private PacketDistributor(java.util.function.BiConsumer<T, CustomPacketPayload> sender) {
        this.sender = sender;
    }

    public Target with(Supplier<T> arg) {
        return payload -> sender.accept(arg.get(), payload);
    }

    public interface Target {
        void send(CustomPacketPayload payload);
    }

    public record TargetPoint(ServerPlayer excluded, double x, double y, double z, double r, ResourceKey<Level> dim) {
        public TargetPoint(double x, double y, double z, double r, ResourceKey<Level> dim) {
            this(null, x, y, z, r, dim);
        }
    }

    @SuppressWarnings("unused")
    private static void unused(Consumer<?> c) {}
}
