package net.sirplop.aetherworks.lib;

import com.rekindled.embers.particle.GlowParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.sirplop.aetherworks.network.PacketDistributor;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.network.MessageSurroundWIthParticles;
import net.sirplop.aetherworks.network.MessageToggleItem;
import net.sirplop.aetherworks.network.PacketHandler;
import net.sirplop.aetherworks.util.Utils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class AWExchangeNode extends AWHarvestNode
{
    public final ItemStack stackConsumed;

    public AWExchangeNode(Player harvester, Level level, BlockPos beginning, int range,
                          Predicate<Player> canHarvest, @Nullable GlowParticleOptions particle, ItemStack is)
    {
        super(harvester, level, beginning, range, canHarvest, particle, 1);
        this.stackConsumed = is;
    }


    @Override
    public void tick()
    {
        if (!canHarvest.test(harvester) || !Utils.hasEnoughDurability(harvester.getMainHandItem(), 1)) {
            this.invalid = true;
            return;
        }
        BlockPos pos = this.toHarvest.pop();
        boolean empty = this.toHarvest.isEmpty();
        BlockState state = this.level.getBlockState(pos);
        if (!this.isLoaded(pos) ||
                state.isAir() ||
                !state.getBlock().equals(baseState.getBlock()) ||
                level.getBlockEntity(pos) != null) {
            if (empty)
                this.invalid = true;
            return;
        }

        int index = harvester.getInventory().findSlotMatchingItem(stackConsumed);
        ItemStack is = index == -1 ? null : this.harvester.getInventory().getItem(index);
        if (index == -1 || !(is.getItem() instanceof BlockItem)) {
            this.invalid = true;
            return;
        }
        BlockState currentState = this.level.getBlockState(pos);
        List<ItemStack> stacks = Block.getDrops(currentState, (ServerLevel) this.level, pos, level.getBlockEntity(pos));
        BlockState toSet = ((BlockItem) is.getItem()).getBlock().defaultBlockState();
        CommonHooks.fireBlockBreak(level, ((ServerPlayer)this.harvester).gameMode.getGameModeForPlayer(), (ServerPlayer)this.harvester, pos, level.getBlockState(pos));

        SoundType sound = currentState.getBlock().getSoundType(state, this.level, pos, this.harvester);
        this.level.playSound(null, pos, sound.getBreakSound(), SoundSource.BLOCKS, sound.getVolume(), sound.getPitch());
        this.level.setBlock(pos, toSet, 1 | 2 | 4);
        sound = toSet.getBlock().getSoundType(state, this.level, pos, this.harvester);
        this.level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, sound.getVolume(), sound.getPitch());
        for (ItemStack stack : stacks)
        {
            if (!this.harvester.getInventory().add(stack))
            {
                this.harvester.drop(stack, true, false);
            }
        }
        if (particle != null) {
            PacketHandler.INSTANCE.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(pos.getX(), pos.getY(), pos.getZ(),
                    16, level.dimension())), new MessageSurroundWIthParticles(pos, 24, particle.getColor()));
        }

        is.shrink(1);
        if (!harvester.isCreative())
            this.harvester.getMainHandItem().hurtAndBreak(1, (net.minecraft.server.level.ServerLevel) ((ServerPlayer) harvester).level(), (ServerPlayer) harvester, item -> {});

        if (empty)
        {
            this.invalid = true;
        }
    }
}
