package net.sirplop.aetherworks.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.sirplop.aetherworks.Aetherworks;

/**
 * SMP 2 port: Forge's SimpleChannel became NeoForge payloads. Every old message class is carried inside an
 * {@link Envelope} with its own payload type; the messages keep their encode/decode/handle methods.
 */
public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final PacketHandler INSTANCE = new PacketHandler();

    private static final Map<Class<?>, CustomPacketPayload.Type<?>> TYPES = new HashMap<>();

    public record Envelope<M>(CustomPacketPayload.Type<Envelope<M>> type, M msg) implements CustomPacketPayload {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(Aetherworks.MODID).versioned(PROTOCOL_VERSION);
        add(r, "harvest_node", MessageHarvestNode.class, MessageHarvestNode::encode, MessageHarvestNode::decode, MessageHarvestNode::handle);
        add(r, "toggle_item", MessageToggleItem.class, MessageToggleItem::encode, MessageToggleItem::decode, MessageToggleItem::handle);
        add(r, "sync_item_entity_tag", MessageSyncItemEntityTag.class, MessageSyncItemEntityTag::encode, MessageSyncItemEntityTag::decode, MessageSyncItemEntityTag::handle);
        add(r, "focused_stack", MessageFocusedStack.class, MessageFocusedStack::encode, MessageFocusedStack::decode, MessageFocusedStack::handle);
        add(r, "surround_particles", MessageSurroundWIthParticles.class, MessageSurroundWIthParticles::encode, MessageSurroundWIthParticles::decode, MessageSurroundWIthParticles::handle);
        add(r, "fluid_sync", MessageFluidSync.class, MessageFluidSync::encode, MessageFluidSync::decode, MessageFluidSync::handle);
        add(r, "sync_aetheriometer", MessageSyncAetheriometer.class, MessageSyncAetheriometer::encode, MessageSyncAetheriometer::decode, MessageSyncAetheriometer::handle);
        add(r, "shield_particle", MessageShieldParticle.class, MessageShieldParticle::encode, MessageShieldParticle::decode, MessageShieldParticle::handle);
        add(r, "sync_entity_motion", MessageSyncEntityMotion.class, MessageSyncEntityMotion::encode, MessageSyncEntityMotion::decode, MessageSyncEntityMotion::handle);
    }

    private static <M> void add(PayloadRegistrar registrar, String name, Class<M> cls, BiConsumer<M, FriendlyByteBuf> encode,
                                Function<FriendlyByteBuf, M> decode, BiConsumer<M, Supplier<NetworkEvent.Context>> handle) {
        CustomPacketPayload.Type<Envelope<M>> type = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Aetherworks.MODID, name));
        TYPES.put(cls, type);
        StreamCodec<RegistryFriendlyByteBuf, Envelope<M>> codec = StreamCodec.of(
                (buf, env) -> encode.accept(env.msg(), buf), buf -> new Envelope<>(type, decode.apply(buf)));
        registrar.playBidirectional(type, codec, (env, ctx) -> {
            NetworkEvent.Context context = new NetworkEvent.Context(ctx);
            handle.accept(env.msg(), () -> context);
        });
    }

    @SuppressWarnings("unchecked")
    static <M> Envelope<M> wrap(M msg) {
        CustomPacketPayload.Type<Envelope<M>> type = (CustomPacketPayload.Type<Envelope<M>>) TYPES.get(msg.getClass());
        if (type == null) throw new IllegalArgumentException("Unregistered Aetherworks message " + msg.getClass());
        return new Envelope<>(type, msg);
    }

    public void sendToServer(Object msg) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(wrap(msg));
    }

    public void send(PacketDistributor.Target target, Object msg) {
        target.send(wrap(msg));
    }
}
