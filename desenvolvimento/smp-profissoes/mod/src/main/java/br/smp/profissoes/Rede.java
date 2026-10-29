package br.smp.profissoes;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Servidor → cliente: abre a tela de escolha de profissão (cliente/TelaProfissoes). A escolha em si continua no
 * servidor: a tela só manda o mesmo comando dos botões do chat (/trigger prof.escolha ou /guildcharter).
 */
public final class Rede {
    private Rede() {}

    /** guildCharter = segunda profissão (Guild Charter); jaTem = profissões que o jogador já tem (ficam apagadas). */
    public record AbrirEscolha(boolean guildCharter, List<String> jaTem) implements CustomPacketPayload {
        public static final Type<AbrirEscolha> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SmpProfissoes.MODID, "abrir_escolha"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AbrirEscolha> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, AbrirEscolha::guildCharter,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), AbrirEscolha::jaTem,
                AbrirEscolha::new);

        @Override
        public Type<AbrirEscolha> type() {
            return TYPE;
        }
    }

    static void registrar(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent e) ->
                e.registrar("1").optional().playToClient(AbrirEscolha.TYPE, AbrirEscolha.CODEC, Rede::receber));
    }

    private static void receber(AbrirEscolha msg, IPayloadContext ctx) {
        // só roda no cliente; a classe da tela nunca é carregada no servidor dedicado
        if (FMLEnvironment.dist.isClient()) ctx.enqueueWork(() -> br.smp.profissoes.cliente.TelaProfissoes.abrir(msg.guildCharter(), msg.jaTem()));
    }

    static void abrirEscolha(ServerPlayer p, boolean guildCharter, List<String> jaTem) {
        PacketDistributor.sendToPlayer(p, new AbrirEscolha(guildCharter, List.copyOf(jaTem)));
    }
}
