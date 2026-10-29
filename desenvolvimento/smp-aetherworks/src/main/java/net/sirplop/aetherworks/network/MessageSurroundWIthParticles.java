package net.sirplop.aetherworks.network;

import com.rekindled.embers.particle.GlowParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sirplop.aetherworks.network.NetworkEvent;
import net.sirplop.aetherworks.Aetherworks;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class MessageSurroundWIthParticles {
    private final int numberOfParticles;
    private final BlockPos pos;
    private final Vector3f color;

    public BlockPos getPos() {
        return pos;
    }
    public int getNumberOfParticles() {
        return numberOfParticles;
    }

    public MessageSurroundWIthParticles(BlockPos pos, int numberOfParticles, Vector3f color)
    {
        this.numberOfParticles = numberOfParticles;
        this.pos = pos;
        this.color = color;
    }

    public MessageSurroundWIthParticles(BlockPos pos, int numberOfParticles, float r, float g, float b)
    {
        this.numberOfParticles = numberOfParticles;
        this.pos = pos;
        this.color = new Vector3f(r, g, b);
    }

    public static void encode(MessageSurroundWIthParticles msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.numberOfParticles);
        buf.writeVector3f(msg.color);
    }

    public static MessageSurroundWIthParticles decode(FriendlyByteBuf buf) {
        return new MessageSurroundWIthParticles(buf.readBlockPos(), buf.readInt(), buf.readVector3f());
    }
    public static void handle(MessageSurroundWIthParticles msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> spawnParticles(msg));
        }
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void spawnParticles(MessageSurroundWIthParticles msg) {
        Level level = Minecraft.getInstance().level;
        assert level != null;
        BlockPos pos = msg.pos;
        GlowParticleOptions particle = new GlowParticleOptions(msg.color, 1.0F, 50);
        for(Direction direction : Direction.values()) {
            BlockPos blockpos = pos.relative(direction);
            if (!level.getBlockState(blockpos).isSolidRender(level, blockpos)) {
                for (int i = 0; i < msg.numberOfParticles; i++) {
                    Direction.Axis direction$axis = direction.getAxis();
                    double d1 = direction$axis == Direction.Axis.X ? 0.5D + 0.5625D * (double) direction.getStepX() : level.random.nextFloat();
                    double d2 = direction$axis == Direction.Axis.Y ? 0.5D + 0.5625D * (double) direction.getStepY() : level.random.nextFloat();
                    double d3 = direction$axis == Direction.Axis.Z ? 0.5D + 0.5625D * (double) direction.getStepZ() : level.random.nextFloat();
                    level.addParticle(particle,
                            (double) pos.getX() + d1,
                            (double) pos.getY() + d2,
                            (double) pos.getZ() + d3,
                            (level.random.nextFloat() - 0.5f) * 0.3f,
                            (level.random.nextFloat() - 0.5f) * 0.3f,
                            (level.random.nextFloat() - 0.5f) * 0.3f);
                }
            }
        }
    }
}
