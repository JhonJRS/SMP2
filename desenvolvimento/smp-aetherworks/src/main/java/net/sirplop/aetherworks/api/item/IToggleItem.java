package net.sirplop.aetherworks.api.item;
import net.sirplop.aetherworks.util.Compat121;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sirplop.aetherworks.network.PacketDistributor;
import net.sirplop.aetherworks.network.MessageToggleItem;
import net.sirplop.aetherworks.network.PacketHandler;

public interface IToggleItem {
    String KEY = "toggleState";

    byte getToggleMax();

    default void toggleItem(ItemStack stack, Player player, byte stateFlag) {
        MessageToggleItem.toggleItem(stack, player, stateFlag);
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToggleItem(stateFlag));
    }

    default byte getToggled(ItemStack stack) {
        if (stack != null && Compat121.tag(stack).contains(KEY))
            return Compat121.tag(stack).getByte(KEY);
        return 0;
    }

    @OnlyIn(Dist.CLIENT)
    void clientModeChanged(ItemStack stack, Player player, byte oldValue, byte newVal, byte stateFlag);
}
