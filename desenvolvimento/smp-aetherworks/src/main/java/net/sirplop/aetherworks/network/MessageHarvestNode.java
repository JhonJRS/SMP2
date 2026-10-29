package net.sirplop.aetherworks.network;

import com.rekindled.embers.particle.GlowParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sirplop.aetherworks.network.NetworkEvent;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class MessageHarvestNode {

    private int stateID;
    private BlockPos pos;

    public BlockPos getPos() {
        return pos;
    }
    public int getStateID() {
        return stateID;
    }

    public MessageHarvestNode(BlockState state, BlockPos pos)
    {
        this.stateID = Block.getId(state);
        this.pos = pos;
    }

    public MessageHarvestNode(int stateID, BlockPos pos)
    {
        this.stateID = stateID;
        this.pos = pos;
    }

    public static void encode(MessageHarvestNode msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.stateID);
        buf.writeBlockPos(msg.pos);
    }

    public static MessageHarvestNode decode(FriendlyByteBuf buf) {
        return new MessageHarvestNode(buf.readInt(), buf.readBlockPos());
    }
    public static void handle(MessageHarvestNode msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> spawnParticles(msg));
        }
        ctx.get().setPacketHandled(true);
    }
    private static final GlowParticleOptions GLOW = new GlowParticleOptions(new Vector3f(0, 0.72F, 0.95F), 1.0F, 100);

    @OnlyIn(Dist.CLIENT)
    public static void spawnParticles(MessageHarvestNode msg) {

        Level level = Minecraft.getInstance().level;
        final BlockParticleOption BLOCK = new BlockParticleOption(ParticleTypes.BLOCK, Block.stateById(msg.getStateID()));
        for (int i = 0; i < 10; ++i)
        {
            level.addParticle(GLOW,
                    msg.getPos().getX() + level.random.nextFloat(),
                    msg.getPos().getY() + level.random.nextFloat(),
                    msg.getPos().getZ() + level.random.nextFloat(),
                    level.random.nextFloat() - level.random.nextFloat() / 10f,
                    level.random.nextFloat() - level.random.nextFloat() / 10f,
                    level.random.nextFloat() - level.random.nextFloat() / 10f);
            level.addParticle(BLOCK,
                    msg.getPos().getX() + level.random.nextFloat(),
                    msg.getPos().getY() + level.random.nextFloat(),
                    msg.getPos().getZ() + level.random.nextFloat(),
                    0, 0, 0);
        }
    }
}
