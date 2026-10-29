package net.sirplop.aetherworks.capabilities;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.LazyOptional;
import net.sirplop.aetherworks.network.PacketDistributor;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.api.capabilities.IAetheriometerCap;
import net.sirplop.aetherworks.network.MessageSyncAetheriometer;
import net.sirplop.aetherworks.network.PacketHandler;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AetheriometerChunk implements IAetheriometerCap {
    public final static String TAG = "aw.aetheramount";

    private final Level level;
    private final ChunkPos chunk;
    private int data;

    private final LazyOptional<IAetheriometerCap> holder;

    public static AetheriometerChunk createDefault(final Level level, final ChunkPos chunkPos, final int data) {
        return new AetheriometerChunk(level, chunkPos, data);
    }

    /** SMP 2 port: default value of the chunk attachment (meteors placed during generation seed it). */
    public static AetheriometerChunk forHolder(net.neoforged.neoforge.attachment.IAttachmentHolder holder) {
        if (holder instanceof LevelChunk chunk) {
            ChunkPos pos = chunk.getPos();
            Integer set = net.sirplop.aetherworks.worldgen.MeteorPlacer.map.remove(pos);
            return new AetheriometerChunk(chunk.getLevel(), pos, set == null ? 0 : set);
        }
        return new AetheriometerChunk(null, ChunkPos.ZERO, 0);
    }
    public static Codec<AetheriometerChunk> codec(final Level level, final ChunkPos chunkPos) {
        return Codec.INT.xmap(
                (val) -> new AetheriometerChunk(level, chunkPos, val),
                AetheriometerChunk::getData
        );
    }

    private AetheriometerChunk(final Level level, final ChunkPos chunkPos, final int data) {
        this.level = level;
        this.chunk = chunkPos;
        this.data = data;

        this.holder = LazyOptional.of(() -> this);
    }

    @Override
    public int getData() {
        return data;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public ChunkPos getChunk() {
        return chunk;
    }

    @Override
    public void adjustData(int change) {
        data = Math.max(0, data + change);
        onChange();
    }

    public void setDataNoUpdate(int set) {
        data = Math.max(0, set);
    }

    private void onChange() {
        if (level == null || level.isClientSide)
            return;

        if (level.hasChunk(chunk.x, chunk.z)) {
            LevelChunk varChunk = level.getChunk(chunk.x, chunk.z);
            varChunk.setUnsaved(true);
            PacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> varChunk), new MessageSyncAetheriometer(chunk, data));
        }
    }

    @Override
    public @Nonnull <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == AWCapabilities.AETHERIOMETER_CAPABILITY ? AWCapabilities.AETHERIOMETER_CAPABILITY.orEmpty(cap, this.holder) : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (data > 0)
            tag.putInt(TAG, data);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains(TAG)) {
            data = nbt.getInt(TAG);
            return;
        }
        data = 0;
    }
}
