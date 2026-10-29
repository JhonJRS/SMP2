package net.sirplop.aetherworks.network;
import net.sirplop.aetherworks.util.Compat121;

import com.rekindled.embers.util.EmberInventoryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sirplop.aetherworks.network.NetworkEvent;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.api.item.IToggleItem;
import net.sirplop.aetherworks.api.item.IToggleEmberItem;
import net.sirplop.aetherworks.AWConfig;

import java.util.function.Supplier;

public class MessageToggleItem {
    public byte stateFlag;

    public MessageToggleItem(byte stateFlag)
    {
        this.stateFlag = stateFlag;
    }

    public static void encode(MessageToggleItem msg, FriendlyByteBuf buf)
    {
        buf.writeByte(msg.stateFlag);
    }

    public static MessageToggleItem decode(FriendlyByteBuf buf) {

        return new MessageToggleItem(buf.readByte());
    }

    public static void handle(MessageToggleItem msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> {
                handleClient(msg);
            });
        } else if (ctx.get().getDirection().getReceptionSide().isServer()) {
            ctx.get().enqueueWork(() -> {
                handleServer(ctx);
            });
        }
        ctx.get().setPacketHandled(true);
    }

    private static void handleServer(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof IToggleItem toggle) {
            toggle.toggleItem(stack, player, (byte)0);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(MessageToggleItem msg) {
        Player player = Minecraft.getInstance().player;
        toggleItem(player.getMainHandItem(), player, msg.stateFlag);
    }

    public static void toggleItem(ItemStack stack, Player player, byte stateFlag) {
        if(!stack.isEmpty() && stack.getItem() instanceof IToggleItem item) {
            Level level = player.getCommandSenderWorld();
            if (stack.getItem() instanceof IToggleEmberItem) { //toggle and check min ember use config.
                if (!Compat121.hasTag(stack)) {
                    byte val = (byte) (EmberInventoryUtil.getEmberTotal(player) >= AWConfig.TOOL_EMBER_USE.get() ? 1 : 0);
                    Compat121.edit(stack, tag -> tag.putByte(IToggleItem.KEY, val));
                    if (level.isClientSide)
                        item.clientModeChanged(stack, player, (byte)0, val, (byte)0);
                } else {
                    byte oldVal = Compat121.tag(stack).getByte(IToggleItem.KEY);
                    byte val = (byte)(oldVal + 1);
                    boolean ember = EmberInventoryUtil.getEmberTotal(player) < AWConfig.TOOL_EMBER_USE.get();
                    if (val > item.getToggleMax() || ember)
                        val = 0;
                    final byte novo = val;
                    Compat121.edit(stack, tag -> tag.putByte(IToggleItem.KEY, novo));
                    if (level.isClientSide)
                        item.clientModeChanged(stack, player, oldVal, val, (byte)(ember ? 1 : stateFlag));
                }
            } else if (!Compat121.hasTag(stack)) {
                Compat121.edit(stack, tag -> tag.putByte(IToggleItem.KEY, (byte)1));
                if (level.isClientSide)
                    item.clientModeChanged(stack, player, (byte)0, (byte)1, stateFlag);
            } else {
                byte oldVal = Compat121.tag(stack).getByte(IToggleItem.KEY);
                byte val = (byte)(oldVal + 1);
                if (val > item.getToggleMax())
                    val = 0;
                final byte novo = val;
                Compat121.edit(stack, tag -> tag.putByte(IToggleItem.KEY, novo));
                if (level.isClientSide)
                    item.clientModeChanged(stack, player, oldVal, val, stateFlag);
            }
        }
    }

    public static void sendToServer() {
        PacketHandler.INSTANCE.sendToServer(new MessageToggleItem((byte)0));
    }
}
