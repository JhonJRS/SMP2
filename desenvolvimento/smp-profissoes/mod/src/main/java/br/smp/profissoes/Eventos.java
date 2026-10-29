package br.smp.profissoes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.rp.rpessentials.profession.LicenseManager;
import net.rp.rpessentials.profession.ProfessionRestrictionManager;
import net.rp.rpessentials.profession.ProfessionSyncHelper;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import java.util.List;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class Eventos {
    private Eventos() {}

    private static int contador = 0;

    @SubscribeEvent
    public static void aoIniciar(ServerStartedEvent ev) {
        Regras.recarregar();
        verificarProtecao("net.minecraft.world.item.crafting.RecipeManager", "guarda central de receitas (autocrafters sem jogador)");
        verificarProtecao("net.minecraft.world.inventory.CraftingMenu", "contexto da bancada/inventário");
        verificarProtecao("net.minecraft.server.network.ServerGamePacketListenerImpl", "contexto de cliques em menus");
        verificarProtecao("com.simibubi.create.AllRecipeTypes", "automação do Create (misturador/prensa/crafter/serra)");
        verificarProtecao("com.simibubi.create.content.equipment.blueprint.BlueprintEntity", "Create Blueprint (fabricar)");
        verificarProtecao("com.simibubi.create.content.equipment.blueprint.BlueprintMenu", "Create Blueprint (tela)");
        verificarProtecao("appeng.menu.me.items.PatternEncodingTermMenu", "padrões AE2");
        verificarProtecao("appeng.menu.me.items.CraftingTermMenu", "AE2 Crafting Terminal (tela)");
        verificarProtecao("appeng.menu.slot.CraftingTermSlot", "AE2 Crafting Terminal (retirar)");
        verificarProtecao("velrondevs.botania.common.item.AssemblyHaloItem", "Botania Assembly Halo");
        verificarProtecao("com.minecolonies.api.inventory.container.ContainerCrafting", "MineColonies tela de ensino");
        verificarProtecao("com.minecolonies.core.network.messages.server.colony.building.worker.AddRemoveRecipeMessage", "ensino de receitas MineColonies");
        verificarProtecao("com.minecolonies.core.research.LocalResearchTree", "cadeia completa de pesquisas (Guild of Trades)");
        verificarProtecao("net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity", "operador das máquinas (tick)");
        verificarProtecao("net.minecraft.server.level.ServerPlayerGameMode", "operador das máquinas (clique)");
        verificarProtecao("com.minecolonies.core.colony.buildings.AbstractBuilding", "nível da colônia por licença de ADM");
        verificarProtecao("net.satisfy.vinery.core.util.WineYears", "Apple Wine até Resistência IV");
        verificarProtecao("appeng.blockentity.misc.CondenserBlockEntity", "Condensador do AE2 sem Singularity");
    }

    /** O Clérigo não vende Echo Shard (troca nível 5 do Create: Deep Dark): o eco vem só da exploração do Deep Dark. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void trocasAldeoes(VillagerTradesEvent ev) {
        if (ev.getType() != VillagerProfession.CLERIC) return;
        RandomSource rnd = RandomSource.create(0);
        int n = 0;
        for (List<VillagerTrades.ItemListing> nivel : ev.getTrades().values()) {
            int antes = nivel.size();
            nivel.removeIf(t -> {
                if (!(t instanceof BasicItemListing)) return false;
                try {
                    MerchantOffer o = t.getOffer(null, rnd);
                    return o != null && o.getResult().is(Items.ECHO_SHARD);
                } catch (Exception ex) {
                    return false;
                }
            });
            n += antes - nivel.size();
        }
        if (n > 0) SmpProfissoes.LOG.info("Trocas do Clérigo removidas (Echo Shard): {}", n);
    }

    @SubscribeEvent
    public static void aoTickInicio(ServerTickEvent.Pre ev) {
        Contexto.limpar();
    }

    private static void verificarProtecao(String classe, String nome) {
        try {
            boolean ok = MarcadorProtecao.class.isAssignableFrom(Class.forName(classe));
            if (ok) SmpProfissoes.LOG.info("Proteção ativa: {}", nome);
            else SmpProfissoes.LOG.error("Proteção NÃO aplicada: {} (mixin falhou; veja o log do Mixin)", nome);
        } catch (ClassNotFoundException e) {
            SmpProfissoes.LOG.info("Proteção ignorada: {} (mod ausente)", nome);
        }
    }

    @SubscribeEvent
    public static void aoTick(ServerTickEvent.Post ev) {
        if (++contador < Regras.get().intervaloTicks) return;
        contador = 0;
        for (ServerPlayer p : ev.getServer().getPlayerList().getPlayers()) {
            try {
                Progressao.sincronizar(p, true);
            } catch (Exception ex) {
                SmpProfissoes.LOG.error("Falha ao sincronizar {}: {}", p.getGameProfile().getName(), ex.toString());
            }
        }
    }

    /**
     * Sem ponto de renascimento de cama/âncora: todos renascem no spawn do mundo ou pelo Shrine (Legendary Shrines,
     * que tem vínculo próprio). Só o /spawnpoint dos ADMs (forçado) ainda define o ponto.
     */
    @SubscribeEvent
    public static void semCama(PlayerSetSpawnEvent ev) {
        if (ev.getNewSpawn() != null && !ev.isForced()) ev.setCanceled(true);
    }

    /**
     * Vagabundo deixou de ser licença (29/10): é só o estado de quem não tem profissão (tag prof.vagabundo). Quem recebeu a
     * licença antiga perde o registro, e o item físico dela fica marcado como revogado.
     */
    static void tirarLicencaVagabundo(ServerPlayer p) {
        if (LicenseManager.getLicenses(p.getUUID()).stream().noneMatch(l -> l.equalsIgnoreCase("vagabundo"))) return;
        LicenseManager.removeLicense(p.getUUID(), "vagabundo");
        ProfessionRestrictionManager.invalidatePlayerCache(p.getUUID());
        ProfessionSyncHelper.syncToPlayer(p);
        net.rp.rpessentials.profession.TempLicenseExpirationManager.markRevokedLicenseItems(p);
        SmpProfissoes.LOG.info("Licença antiga de Vagabundo removida de {}", p.getGameProfile().getName());
    }

    @SubscribeEvent
    public static void aoEntrar(PlayerEvent.PlayerLoggedInEvent ev) {
        if (ev.getEntity() instanceof ServerPlayer p) {
            if (p.getRespawnPosition() != null && !p.isRespawnForced())  // camas definidas antes desta regra
                p.setRespawnPosition(net.minecraft.world.level.Level.OVERWORLD, null, 0f, false, false);
            try {
                tirarLicencaVagabundo(p);
                Progressao.sincronizar(p, false);
                Especiais.aoEntrar(p);
                Tickets.aoEntrar(p);
            } catch (Exception ex) {
                SmpProfissoes.LOG.error("Falha ao sincronizar {} no login: {}", p.getGameProfile().getName(), ex.toString());
            }
        }
    }

    @SubscribeEvent
    public static void comandos(RegisterCommandsEvent ev) {
        CommandDispatcher<CommandSourceStack> d = ev.getDispatcher();
        Especiais.comandos(d);
        Dimensoes.comandos(d);
        Tickets.comandos(d);
        d.register(Commands.literal("progressao").executes(c -> mostrar(c, c.getSource().getPlayerOrException())));
        d.register(Commands.literal("progression").executes(c -> mostrar(c, c.getSource().getPlayerOrException())));
        d.register(Commands.literal(SmpProfissoes.MODID).requires(s -> s.hasPermission(2))
                .then(Commands.literal("status").then(Commands.argument("jogador", EntityArgument.player())
                        .executes(c -> mostrar(c, EntityArgument.getPlayer(c, "jogador")))))
                .then(Commands.literal("sincronizar").executes(c -> {
                    int n = 0;
                    for (ServerPlayer p : c.getSource().getServer().getPlayerList().getPlayers()) if (Progressao.sincronizar(p, true)) n++;
                    int fn = n;
                    c.getSource().sendSuccess(() -> Component.literal("Sincronizado; " + fn + " jogador(es) mudaram de etapa."), true);
                    return n;
                }))
                .then(Commands.literal("inicio").then(Commands.argument("jogador", EntityArgument.player())
                        .then(Commands.argument("licenca", StringArgumentType.word()).executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "jogador");
                            String lic = StringArgumentType.getString(c, "licenca").toLowerCase();
                            Regras.Etapa e = Regras.get().etapas.stream().filter(x -> x.licenca.equals(lic) && x.inicio).findFirst().orElse(null);
                            if (e == null) {
                                c.getSource().sendFailure(Component.literal("Not a starting-mod stage: " + lic));
                                return 0;
                            }
                            for (Regras.Etapa x : Regras.get().etapas)  // um mod inicial por profissão (a 2ª vem da Guild Charter)
                                if (x.inicio && x.profissao.equals(e.profissao)) LicenseManager.removeLicense(p.getUUID(), Regras.PREFIXO_INICIO + x.licenca);
                            LicenseManager.addLicense(p.getUUID(), Regras.PREFIXO_INICIO + lic);
                            Progressao.sincronizar(p, true);
                            SmpProfissoes.LOG.info("{} começou pelo mod da etapa {}", p.getGameProfile().getName(), lic);
                            c.getSource().sendSuccess(() -> Component.literal("Starting mod set: " + e.nome), true);
                            return 1;
                        }))))
                .then(Commands.literal("resetar").then(Commands.argument("jogador", EntityArgument.player()).executes(c -> {
                    ServerPlayer p = EntityArgument.getPlayer(c, "jogador");
                    for (String prof : Regras.get().profissoes) LicenseManager.removeLicense(p.getUUID(), prof);
                    for (Regras.Etapa x : Regras.get().etapas) {
                        LicenseManager.removeLicense(p.getUUID(), x.licenca);
                        LicenseManager.removeLicense(p.getUUID(), Regras.PREFIXO_INICIO + x.licenca);
                    }
                    if (net.neoforged.fml.ModList.get().isLoaded("irons_restrictions")) EscolasMagia.limpar(p);  // escolas de magia também
                    ProfessionRestrictionManager.invalidatePlayerCache(p.getUUID());
                    ProfessionSyncHelper.syncToPlayer(p);
                    c.getSource().sendSuccess(() -> Component.literal("Profession, starting mod and stages removed: " + p.getGameProfile().getName()), true);
                    return 1;
                })))
                // nível máximo da colônia (Prefeitura) liberado pela administração: licença colonia_N (o "license give" do
                // RPEssentials só aceita profissões cadastradas, por isso o comando é nosso)
                .then(Commands.literal("colonia").then(Commands.argument("jogador", EntityArgument.player())
                        .executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "jogador");
                            int n = Colonia.nivelLiberado(p);
                            c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + ": colony level up to " + n), false);
                            return n;
                        })
                        .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 5)).executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "jogador");
                            int n = IntegerArgumentType.getInteger(c, "nivel");
                            Colonia.definirNivel(p, n);
                            c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + " can now grow the colony up to level " + n), true);
                            p.sendSystemMessage(Component.literal("✦ Your colony may now grow up to level " + n + ".").withStyle(net.minecraft.ChatFormatting.GOLD));
                            return n;
                        }))))
                // área protegida do ArcadiaGuard a partir da seleção da varinha (/ag wand give): ninguém quebra nem
                // coloca blocos, mas portas, botões, baús e blocos interativos continuam funcionando
                .then(Commands.literal("zona").then(Commands.argument("nome", StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String nome = StringArgumentType.getString(c, "nome");
                    var cmds = c.getSource().getServer().getCommands();
                    var src = p.createCommandSourceStack();
                    cmds.performPrefixedCommand(src, "ag zone create " + nome);
                    for (String f : new String[] {"block-break", "block-place"}) cmds.performPrefixedCommand(src, "ag zone flag " + nome + " " + f + " deny");
                    for (String f : new String[] {"block-interact", "container-access", "pressure-plate"})
                        cmds.performPrefixedCommand(src, "ag zone flag " + nome + " " + f + " allow");
                    return 1;
                })))
                .then(Commands.literal("recarregar").executes(c -> {
                    String erro = Regras.recarregar();
                    if (erro != null) {
                        c.getSource().sendFailure(Component.literal(erro));
                        return 0;
                    }
                    c.getSource().sendSuccess(() -> Component.literal("Regras recarregadas: " + Regras.get().etapas.size() + " etapas."), true);
                    return 1;
                })));
    }

    private static int mostrar(CommandContext<CommandSourceStack> c, ServerPlayer alvo) {
        Progressao.sincronizar(alvo, true);
        for (Component l : Progressao.status(alvo)) c.getSource().sendSystemMessage(l);
        return 1;
    }
}
