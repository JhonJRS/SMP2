package net.sirplop.aetherworks.capabilities;

import com.rekindled.embers.compat.legacy.LazyOptional;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.api.capabilities.IAetheriometerCap;
import net.sirplop.aetherworks.network.MessageSyncAetheriometer;
import net.sirplop.aetherworks.network.PacketDistributor;
import net.sirplop.aetherworks.network.PacketHandler;

/** SMP 2 port: the per-chunk aether amount is the chunk data attachment AWRegistry.AETHERIOMETER (1.21 has no chunk capabilities). */
public final class AetheriometerChunkCapability {

    public static LazyOptional<IAetheriometerCap> getData(final Level level, final ChunkPos chunkPos) {
        return getData(level.getChunk(chunkPos.x, chunkPos.z));
    }

    public static LazyOptional<IAetheriometerCap> getData(final LevelChunk chunk) {
        return LazyOptional.of(() -> chunk.getData(AWRegistry.AETHERIOMETER_DATA));
    }

    @EventBusSubscriber(modid = Aetherworks.MODID)
    @SuppressWarnings("unused")
    private static class EventHandler {
        /** Send the chunk's aether amount to the client when a player starts watching the chunk. */
        @SubscribeEvent
        public static void chunkWatch(final ChunkWatchEvent.Watch event) {
            final var player = event.getPlayer();
            final int data = getData(event.getLevel(), event.getPos()).orElseThrow().getData();

            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new MessageSyncAetheriometer(event.getPos(), data));
        }
    }
}
