package br.smp.profissoes;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * "Operador" de uma máquina: quem a colocou (guardado no próprio bloco, sobrevive ao reinício). Enquanto a máquina
 * trabalha (tick do bloco) as receitas dela só entregam itens que o operador poderia fabricar (profissão + etapa);
 * no momento do clique vale quem clicou (CliqueBlocoMixin). Assim o Cooking Pot de quem não tem Gourmet
 * Cuisine não faz os pratos dessa pesquisa, o caldeirão do Enchanted só faz o que a etapa libera, etc.
 * Máquina sem operador (colocada antes desta versão, por contraption ou mundo) segue a regra antiga (só o gerador veda).
 */
public final class Operador {
    private Operador() {}

    private static final DeferredRegister<AttachmentType<?>> ANEXOS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SmpProfissoes.MODID);
    public static final Supplier<AttachmentType<String>> OPERADOR = ANEXOS.register("operador",
            () -> AttachmentType.builder(() -> "").serialize(Codec.STRING).build());

    static void registrar(IEventBus modBus) {
        ANEXOS.register(modBus);
    }

    public static void marcar(BlockEntity be, ServerPlayer p) {
        if (be == null || p == null || p instanceof net.neoforged.neoforge.common.util.FakePlayer) return;
        String id = p.getUUID().toString();
        if (!id.equals(be.getData(OPERADOR))) {
            be.setData(OPERADOR, id);
            be.setChanged();
        }
    }

    /** Jogador que responde pela máquina: online (vale o bypass de OP) ou um FakePlayer com o mesmo UUID (licenças são por UUID). */
    public static ServerPlayer jogadorDe(BlockEntity be) {
        if (be == null || !be.hasData(OPERADOR) || !(be.getLevel() instanceof ServerLevel sl)) return null;
        String id = be.getData(OPERADOR);
        if (id.isEmpty()) return null;
        try {
            UUID uuid = UUID.fromString(id);
            ServerPlayer online = sl.getServer().getPlayerList().getPlayer(uuid);
            return online != null ? online : FakePlayerFactory.get(sl, new GameProfile(uuid, "smp_operador"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Empilha o operador do bloco no {@link Contexto}; devolve true se empilhou (quem chamou desempilha). */
    public static boolean entrar(BlockEntity be) {
        ServerPlayer p = jogadorDe(be);
        if (p == null) return false;
        Contexto.entrarOperador(p);
        return true;
    }

    // ------------------------------------------------------------------ eventos (servidor)
    /** Clique só adota máquina sem dono (colocada antes desta versão): visitante não troca o operador da máquina alheia. */
    @SubscribeEvent
    public static void aoClicar(PlayerInteractEvent.RightClickBlock ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p)) return;
        BlockEntity be = ev.getLevel().getBlockEntity(ev.getPos());
        if (be != null && (!be.hasData(OPERADOR) || be.getData(OPERADOR).isEmpty())) marcar(be, p);
    }

    @SubscribeEvent
    public static void aoColocar(BlockEvent.EntityPlaceEvent ev) {
        if (ev.getEntity() instanceof ServerPlayer p) marcar(ev.getLevel().getBlockEntity(ev.getPos()), p);
    }
}
