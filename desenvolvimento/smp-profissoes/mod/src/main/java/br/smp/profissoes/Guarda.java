package br.smp.profissoes;

import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.rp.rpessentials.profession.LicenseManager;
import net.rp.rpessentials.profession.ProfessionRestrictionManager;

/**
 * Decide se uma receita de bancada/cortador/ferraria pode ser usada no servidor.
 * <ul>
 *   <li>Itens dos mods das profissões só saem com jogador autorizado no {@link Contexto} (profissão + etapa).</li>
 *   <li>Selos das pesquisas cruzadas (receitas smpprofissoes:selo/...) só saem para quem tem a licença da pesquisa.</li>
 *   <li>Sem jogador (Crafter vanilla, Crafty Crate, tartaruga CC, Assembler IE, Create...) nenhum dos dois é fabricado.</li>
 * </ul>
 */
public final class Guarda {
    private Guarda() {}

    public static final String CHAVE_SELO = "smp_selo";

    public static boolean tipoVigiado(RecipeType<?> t) {
        return t == RecipeType.CRAFTING || t == RecipeType.STONECUTTING || t == RecipeType.SMITHING;
    }

    public static boolean itemProtegido(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(s.getItem());
        return Regras.get().namespaces().contains(id.getNamespace());
    }

    /** Licenças exigidas por uma receita de selo, ou null se a receita não é de selo. */
    public static List<String> licencasDaReceita(ResourceLocation id) {
        return id == null ? null : Regras.get().receitasComLicenca.get(id.toString());
    }

    /** Id do selo gravado no item (custom_data.smp_selo), ou null. */
    public static String seloDoItem(ItemStack s) {
        if (s == null || s.isEmpty()) return null;
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        String v = cd.copyTag().getString(CHAVE_SELO);
        return v.isEmpty() ? null : v;
    }

    public static boolean temAlguma(ServerPlayer p, List<String> licencas) {
        if (p.hasPermissions(2)) return true; // mesma regra do opLevelBypass do RPEssentials
        List<String> minhas = LicenseManager.getLicenses(p.getUUID());
        for (String l : licencas) for (String m : minhas) if (m.equalsIgnoreCase(l)) return true;
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ItemStack resultado(RecipeHolder<?> h, RecipeInput entrada, HolderLookup.Provider reg) {
        try {
            Recipe r = h.value();
            ItemStack s = r.getResultItem(reg);
            if ((s == null || s.isEmpty()) && entrada != null) s = r.assemble(entrada, reg);
            return s == null ? ItemStack.EMPTY : s;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    /**
     * Pode este jogador obter este item fabricando? Usado onde não há receita à mão (padrão AE2, ensino MineColonies).
     * Devolve a mensagem de recusa, ou null se pode.
     */
    public static String recusaParaItem(ServerPlayer p, ItemStack s) {
        String selo = seloDoItem(s);
        if (selo != null) {
            List<String> lic = licencasDaReceita(ResourceLocation.fromNamespaceAndPath(SmpProfissoes.MODID, "selo/" + selo));
            if (lic != null && !temAlguma(p, lic)) return "§c✘ This seal requires the research stage: §e" + String.join(", ", lic);
            return null;
        }
        if (itemProtegido(s)) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(s.getItem());
            if (!ProfessionRestrictionManager.canCraft(p, id)) return ProfessionRestrictionManager.getCraftBlockedMessage(id);
        }
        return null;
    }

    /**
     * Receita de máquina (qualquer tipo fora da bancada) com operador no contexto: bloqueia se o operador não poderia
     * fabricar o resultado (profissão + etapa). Sem operador = não interfere. Nunca lança exceção.
     */
    public static boolean bloquearMaquina(RecipeHolder<?> h, RecipeInput entrada, HolderLookup.Provider reg) {
        try {
            ServerPlayer op = Contexto.operador();
            if (op == null || h == null || tipoVigiado(h.value().getType())) return false;
            ItemStack s = resultado(h, entrada, reg);
            if (s.isEmpty()) return false;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(s.getItem());
            if ("minecraft".equals(id.getNamespace()) || op.hasPermissions(2)) return false;
            return !ProfessionRestrictionManager.canCraft(op, id);
        } catch (Throwable t) {
            return false;
        }
    }

    /** true = bloquear esta receita agora. Nunca lança exceção (falha = não interfere). */
    public static boolean bloquear(RecipeHolder<?> h, RecipeInput entrada, HolderLookup.Provider reg) {
        try {
            if (h == null || !tipoVigiado(h.value().getType())) return false;
            List<String> lic = licencasDaReceita(h.id());
            ServerPlayer p = Contexto.jogador();
            if (lic != null) {
                if (p == null) return true;
                if (temAlguma(p, lic)) return false;
                p.displayClientMessage(Component.literal("§c✘ This seal requires the research stage: §e" + String.join(", ", lic)), true);
                return true;
            }
            ItemStack s = resultado(h, entrada, reg);
            if (!itemProtegido(s)) return false;
            if (p == null) return true;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(s.getItem());
            if (ProfessionRestrictionManager.canCraft(p, id)) return false;
            ProfessionRestrictionManager.sendCraftBlockedMessage(p, id); // tem cooldown próprio no RPEssentials
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
