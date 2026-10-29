"""Fase 2 (idempotente, edita src/ no lugar): capabilities pela camada legada do Embers Re-Ignited
(com.rekindled.embers.compat.legacy) e outras trocas de nome. Uso: python -I smp-aetherworks/tools/fase2.py"""
import re
from pathlib import Path

SRC = Path(__file__).resolve().parents[1] / "src" / "main" / "java"
LEG = "com.rekindled.embers.compat.legacy"
TROCAS = [
    (r"net\.neoforged\.neoforge\.common\.capabilities\.", LEG + ".capabilities."),
    (r"net\.neoforged\.neoforge\.common\.util\.LazyOptional", LEG + ".LazyOptional"),
    # BlockEntity não tem mais getCapability/invalidateCaps: a ponte é registrada no RegisterCapabilitiesEvent
    (r"return super\.getCapability\((\w+), (\w+)\);", "return LazyOptional.empty();"),
    (r"return super\.getCapability\((\w+)\);", "return LazyOptional.empty();"),
    (r"[ \t]*super\.invalidateCaps\(\);\n", ""),
    (r"\bgetBlockReach\(\)", "blockInteractionRange()"),
    (r"\bgetEntityReach\(\)", "entityInteractionRange()"),
    (r"\bsetSecondsOnFire\(", "igniteForSeconds("),
    (r"\bnet\.neoforged\.neoforge\.common\.ToolActions\b", "net.neoforged.neoforge.common.ItemAbilities"),
    (r"\bToolActions\b", "ItemAbilities"),
    (r"\bnet\.neoforged\.neoforge\.common\.ToolAction\b", "net.neoforged.neoforge.common.ItemAbility"),
    (r"\bToolAction\b", "ItemAbility"),
    (r"\bcanPerformAction\(", "canPerformAction("),
    # block entities: salvar/carregar recebem os registros
    (r"import net\.neoforged\.neoforge\.fluids\.capability\.FluidHandlerBlockEntity;\n", ""),
    (r"public void load\(CompoundTag (\w+)\)", r"public void loadAdditional(CompoundTag \1, HolderLookup.Provider registries)"),
    (r"super\.load\((\w+)\);", r"super.loadAdditional(\1, registries);"),
    (r"(public|protected) void saveAdditional\(CompoundTag (\w+)\)", r"\1 void saveAdditional(CompoundTag \2, HolderLookup.Provider registries)"),
    (r"super\.saveAdditional\((\w+)\);", r"super.saveAdditional(\1, registries);"),
    (r"public CompoundTag getUpdateTag\(\)", "public CompoundTag getUpdateTag(HolderLookup.Provider registries)"),
    (r"super\.getUpdateTag\(\)", "super.getUpdateTag(registries)"),
    (r"\bsaveWithoutMetadata\(\)", "saveWithoutMetadata(registries)"),
    (r"(\w*[iI]nventory\w*|\w*[hH]andler\w*)\.serializeNBT\(\)", r"\1.serializeNBT(registries)"),
    (r"deserializeNBT\((?:registries, )+", "deserializeNBT(registries, "),  # conserto de versão que se reaplicava
    (r"(\w*[iI]nventory\w*|\w*[hH]andler\w*)\.deserializeNBT\((?!registries)", r"\1.deserializeNBT(registries, "),
    (r"public (@\w+ )?CompoundTag getUpdateTag\(\)", r"public \1CompoundTag getUpdateTag(HolderLookup.Provider registries)"),
    (r"(LazyOptional|getCapability\([^;]*?\))\.resolve\(\)", r"\1.resolveOptional()"),
    (r"(\w*[tT]ank\w*)\.writeToNBT\((\w+)\)", r"\1.writeToNBT(registries, \2)"),
    (r"(\w*[tT]ank\w*)\.readFromNBT\((\w+)\)", r"\1.readFromNBT(registries, \2)"),
    (r"\bItemStack\.of\(", "Compat121.stackOf("),
    # receitas: RecipeInput, sem id, serializer legado do Embers
    (r"import net\.neoforged\.neoforge\.items\.wrapper\.RecipeWrapper;", "import net.sirplop.aetherworks.recipe.RecipeWrapper;"),
    (r"(assemble|getResultItem)\(([^()]*?)(?:@Nonnull )?RegistryAccess (\w+)\)", r"\1(\2HolderLookup.Provider \3)"),
    (r"@Override(\s+public (?:@\w+ )?ResourceLocation getId\(\))", r"\1"),
    (r"container\.canPlaceItem\(\w+, \w+\)", "true"),
    (r"implements RecipeSerializer<(\w+)>", r"extends LegacyRecipeSerializer<\1>"),
    (r"\bIngredient\.fromJson\(", "Compat121.ingredientFromJson("),
    (r"\bIngredient\.fromNetwork\((\w+)\)", r"Compat121.readIngredient(\1)"),
    (r"\bShapedRecipe\.itemStackFromJson\(", "Compat121.itemFromJson("),
    (r"\bCraftingHelper\.getItemStack\((\w+), (?:true|false)(?:, (?:true|false))?\)", r"Compat121.itemFromJson(\1)"),
    (r"(\w+)\.readItem\(\)", r"Compat121.readItem(\1)"),
    (r"Compat121\.writeItem\((?:Compat121, )+", "Compat121.writeItem("),  # conserto de uma versão que se reaplicava
    (r"\b(?!Compat121\b)(\w+)\.writeItemStack\((.+?), (?:true|false)\);", r"Compat121.writeItem(\1, \2);"),
    (r"\b(?!Compat121\b)(\w+)\.writeItem\((.+?)\);", r"Compat121.writeItem(\1, \2);"),
    (r"(recipe\.inputs?(?:\.get\(\w+\))?)\.toNetwork\((\w+)\);", r"Compat121.writeIngredient(\2, \1);"),
    # receitas de bancada: CraftingContainer -> CraftingInput
    (r"import net\.minecraft\.world\.inventory\.CraftingContainer;", "import net.minecraft.world.item.crafting.CraftingInput;"),
    (r"\bCraftingContainer\b", "CraftingInput"),
    (r"(\w+)\.readFluidStack\(\)", r"Compat121.readFluid(\1)"),
    (r"(\w+)\.writeFluidStack\((.+?)\);", r"Compat121.writeFluid(\1, \2);"),
    (r"\bFluidStack\.readFromPacket\((\w+)\)", r"Compat121.readFluid(\1)"),
    # dados do item, só leitura (escritas são revistas à mão com Compat121.edit)
    (r"([\w.]+(?:\([\w.]*\))?)\.getOrCreateTag\(\)\.(contains|getInt|getByte|getCompound|getBoolean|getString|getFloat|getDouble|getLong|getList|equals)\(",
     r"Compat121.tag(\1).\2("),
    (r"([\w.]+(?:\([\w.]*\))?)\.getTag\(\)\.(contains|getInt|getByte|getCompound|getBoolean|getString|getFloat|getDouble|getLong|getList|equals|copy)\(",
     r"Compat121.tag(\1).\2("),
    (r"([\w.]+(?:\([\w.]*\))?)\.hasTag\(\)", r"Compat121.hasTag(\1)"),
    (r"new FluidHandlerItemStack\((?!AWRegistry\.FLUID_CONTENTS)", "new FluidHandlerItemStack(AWRegistry.FLUID_CONTENTS, "),
    # rede: classes de compatibilidade no pacote network
    (r"import net\.neoforged\.neoforge\.network\.NetworkEvent;\n", "import net.sirplop.aetherworks.network.NetworkEvent;\n"),
    (r"import net\.neoforged\.neoforge\.network\.PacketDistributor;\n", "import net.sirplop.aetherworks.network.PacketDistributor;\n"),
    # encantamentos: chaves de registro
    (r"EnchantmentHelper\.getItemEnchantmentLevel\((Enchantments\.\w+), ", r"Compat121.enchantmentLevel(\1, "),
    (r"\b(\w+)\.getEnchantmentLevel\((Enchantments\.\w+)\)", r"Compat121.enchantmentLevel(\2, \1)"),
    (r"\.setLight\(([^,()]+), ([^,()]+)\)", r".setUv2(\1, \2)"),
    (r"ItemHandlerHelper\.copyStackWithSize\(([^,()]+(?:\([^()]*\))?), ([^()]+?)\)", r"\1.copyWithCount(\2)"),
    (r"\bItemStack\.isSameItemSameTags\(", "ItemStack.isSameItemSameComponents("),
    (r"\bEnchantments\.FLAMING_ARROWS\b", "Enchantments.FLAME"),
    (r"\bEnchantments\.POWER_ARROWS\b", "Enchantments.POWER"),
    (r"\bEnchantments\.PUNCH_ARROWS\b", "Enchantments.PUNCH"),
    (r"\bOperation\.MULTIPLY_TOTAL\b", "Operation.ADD_MULTIPLIED_TOTAL"),
    (r"\bOperation\.MULTIPLY_BASE\b", "Operation.ADD_MULTIPLIED_BASE"),
    (r"\bOperation\.ADDITION\b", "Operation.ADD_VALUE"),
    (r"\.dimensionTypeId\(\)", ".dimensionTypeRegistration().unwrapKey().orElseThrow()"),
    (r"MapCodec<\? extends (BaseEntityBlock|HorizontalDirectionalBlock|DirectionalBlock)>", r"MapCodec<? extends net.minecraft.world.level.block.\1>"),
    (r"\.orElseThrow\([\w.]+::new\)", ".orElseThrow()"),
    # capabilities de outros objetos: pela ponte do Embers (bloco, item, jogador)
    (r"CapabilityCompat\.getCapability\((?:[\w.]*CapabilityCompat, )+", "CapabilityCompat.getCapability("),  # conserto
    (r"(?<![\w.])(?!this\b|super\b|[\w.]*CapabilityCompat\.getCapability)([a-zA-Z_][\w]*(?:\.[a-zA-Z_]\w*)*(?:\([^()]*\))?)\.getCapability\(",
     r"CapabilityCompat.getCapability(\1, "),
    (r"(?m)^(\s*)(.+?)\.hurt\(([^,]+), [^,]+, \(ServerPlayer\) ?(.+?)\);$",
     r"\1\2.hurtAndBreak(\3, (net.minecraft.server.level.ServerLevel) ((ServerPlayer) \4).level(), (ServerPlayer) \4, item -> {});"),
    # efeitos: Holder<MobEffect>
    (r"AWRegistry\.(EFFECT_\w+)\.get\(\)", r"AWRegistry.\1"),
    (r"\bMobEffect (\w+) = (\w+)\.getEffect\(\);", r"net.minecraft.core.Holder<MobEffect> \1 = \2.getEffect();"),
    (r"EnchantmentHelper\.getEnchantmentLevel\((Enchantments\.\w+), ", r"EnchantmentHelper.getEnchantmentLevel(Compat121.enchantment(\1), "),
    (r"(\w+)\.canSurvive\(level\.getBlockState\((\w+)\), level, \2\)", r"\1.defaultBlockState().canSurvive(level, \2)"),
    (r"\bmobeffect\.(isInstantenous|applyInstantenousEffect)\(", r"mobeffect.value().\1("),
    (r"import net\.minecraft\.data\.worldgen\.BootstapContext;\n", ""),
    (r"\.hurtAndBreak\((\w+), (\w+), \(?\w+\)? -> \{\s*\w+\.broadcastBreakEvent\((\w+)\);\s*\}\);",
     r".hurtAndBreak(\1, \2, net.minecraft.world.entity.LivingEntity.getSlotForHand(\3));"),
    (r"(?m)^(\s*)([\w.()]+?)\.(?:getOrCreateTag|getTag)\(\)\.(put\w*|remove)\((.*)\);$", r"\1Compat121.edit(\2, tag -> tag.\3(\4));"),
    (r"public void load\((@\w+ )?CompoundTag (\w+)\)", r"public void loadAdditional(\1CompoundTag \2, HolderLookup.Provider registries)"),
    (r"(\w+)\.getUseDuration\(\) - (\w+)\.getUseItemRemainingTicks\(\)\) / \(float\)CrossbowItem\.getChargeDuration\(\1\)",
     r"\1.getUseDuration(\2) - \2.getUseItemRemainingTicks()) / (float)CrossbowItem.getChargeDuration(\1, \2)"),
    (r"tag\.put\((\w+), (\w+)\.serializeNBT\(\)\)", r"tag.put(\1, Compat121.save(\2))"),
    (r"Lazy<(\w+)> (\w+) = \(\) -> (.+?);", r"Lazy<\1> \2 = Lazy.of(() -> \3);"),
    (r"import net\.neoforged\.neoforge\.event\.TickEvent;", "import net.neoforged.neoforge.event.tick.LevelTickEvent;"),
    (r"TickEvent\.LevelTickEvent\b", "LevelTickEvent.Post"),
    # 1.21: os efeitos pós-dano de encantamento passam pela fonte de dano
    (r"(?m)^(\s*)EnchantmentHelper\.doPostHurtEffects\((\w+), \w+\);\s*\n\s*EnchantmentHelper\.doPostDamageEffects\(\w+, \w+\);",
     r"\1if (\2.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)\n\1    EnchantmentHelper.doPostAttackEffects(serverLevel, \2, this.source.apply(projectileEntity));"),
    (r"\.resolve\(\)\.get\(\)", ".resolveOptional().get()"),
    (r"\bmezz\.jei\.api\.forge\b", "mezz.jei.api.neoforge"),
    (r"\bForgeTypes\b", "NeoForgeTypes"),
    (r"(\w+)\.hasCustomHoverName\(\)", r"\1.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)"),
    (r"ItemHandlerHelper\.copyStackWithSize\((\w+), (.+?)\);", r"\1.copyWithCount(\2);"),
    (r"\.hurtAndBreak\((\w+), (\w+), \(?\w+\)? -> \{\s*\w+\.broadcastBreakEvent\(([\w.()]+)\);\s*\}\);",
     r".hurtAndBreak(\1, \2, net.minecraft.world.entity.LivingEntity.getSlotForHand(\3));"),
    (r"public int getUseDuration\((?:@\w+ )?ItemStack (\w+)\)", r"public int getUseDuration(ItemStack \1, LivingEntity entity)"),
    (r"CommonHooks\.onBlockBreakEvent\((\w+), ([^;]+?), ([^,;]+), (\w+)\);", r"CommonHooks.fireBlockBreak(\1, \2, \3, \4, \1.getBlockState(\4));"),
    (r"(\w+)\.getItem\(\)\.setTag\((.+?)\);", r"Compat121.setTag(\1.getItem(), \2);"),
    (r"\.pickupBlock\((\w+), (\w+), (\w+)\)", r".pickupBlock(null, \1, \2, \3)"),
    (r"public static final Codec<MeteorStructure> CODEC", "public static final com.mojang.serialization.MapCodec<MeteorStructure> CODEC"),
    (r"ClientboundBlockEntityDataPacket\.create\(this, \(BE\) -> \{\s*return this\.getUpdateTag\(\);\s*\}\)",
     "ClientboundBlockEntityDataPacket.create(this, (be, registries) -> this.getUpdateTag(registries))"),
    (r"import net\.neoforged\.neoforge\.event\.TickEvent;", "import net.neoforged.neoforge.client.event.ClientTickEvent;"),
    (r"TickEvent\.ClientTickEvent\b", "ClientTickEvent.Post"),
    # itens
    (r"appendHoverText\(((?:@\w+ )?ItemStack \w+), (?:@\w+ )?Level (\w+), ", r"appendHoverText(\1, Item.TooltipContext \2, "),
    (r"int damageItem\(ItemStack (\w+), int (\w+), T (\w+), Consumer<T> (\w+)\)", r"int damageItem(ItemStack \1, int \2, T \3, Consumer<Item> \4)"),
    (r"public int getUseDuration\(ItemStack (\w+)\)", r"public int getUseDuration(ItemStack \1, LivingEntity entity)"),
]
IMPORTS_RECEITA = {"LegacyRecipeSerializer<": "com.rekindled.embers.recipe.LegacyRecipeSerializer"}
IMPORTS = {"HolderLookup.Provider": "net.minecraft.core.HolderLookup", "Compat121.": "net.sirplop.aetherworks.util.Compat121",
           "Item.TooltipContext": "net.minecraft.world.item.Item",
           "AWRegistry.": "net.sirplop.aetherworks.AWRegistry", "CapabilityCompat.": "com.rekindled.embers.util.CapabilityCompat"}


def provedor(t, nome):
    """Block entities com getCapability legado declaram ICapabilityProvider (a ponte do Embers chama direto, sem reflexão)."""
    if not re.search(r"public <T> LazyOptional<T> getCapability\(\s*(?:@\w+ )?Capability<T> \w+, (?:@\w+ )?Direction \w+\)", t):
        return t
    m = re.search(r"public (?:abstract )?class " + nome + r"\b[^{]*", t)
    if not m or "ICapabilityProvider" in m.group(0):
        return t
    decl = m.group(0).rstrip()
    nova = decl + (", " if " implements " in decl else " implements ") + "com.rekindled.embers.compat.legacy.capabilities.ICapabilityProvider"
    return t.replace(decl, nova, 1)


def tirar_override(t, nomes):
    """@Override antes de métodos que deixaram de sobrescrever algo (capabilities legadas)."""
    for n in nomes:
        t = re.sub(r"@Override(\s+(?:@\w+(?:\([^)]*\))?\s+)*public\s+(?:<\w+>\s+)?[\w<>?,\s]+\b" + n + r"\()", r"\1", t)
    return t


VERTICE = [(r"\.vertex\(", ".addVertex("), (r"\.uv\(", ".setUv("), (r"\.color\(", ".setColor("), (r"\.uv2\(", ".setLight("),
           (r"\.overlayCoords\(", ".setOverlay("), (r"\.normal\(", ".setNormal("), (r"\.endVertex\(\)", "")]


def vertices(t):
    """1.21: VertexConsumer.vertex(..).uv(..)...endVertex() -> addVertex(..).setUv(..)... (só nos comandos com endVertex)."""
    def troca(m):
        s = m.group(0)
        for a, b in VERTICE:
            s = re.sub(a, b, s)
        return s
    return re.sub(r"[^;{}]*\.endVertex\(\);", troca, t)


PONTE_USE = """
    // SMP 2 port: 1.21 split Block#use into useItemOn / useWithoutItem; both call the original logic.
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult r = useLegacy(state, level, pos, player, hand, hit);
        if (r == InteractionResult.PASS) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (r == InteractionResult.FAIL) return net.minecraft.world.ItemInteractionResult.FAIL;
        return r == InteractionResult.SUCCESS ? net.minecraft.world.ItemInteractionResult.SUCCESS : net.minecraft.world.ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return useLegacy(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
"""


def blocos(t, nome):
    if re.search(r"public InteractionResult use\(BlockState \w+, Level \w+, BlockPos \w+, Player \w+, InteractionHand \w+, BlockHitResult \w+\)", t):
        t = re.sub(r"@Override(\s+public InteractionResult) use\(BlockState", r"\1 useLegacy(BlockState", t)
        t = re.sub(r"public InteractionResult use\(BlockState", "public InteractionResult useLegacy(BlockState", t)
        fim = t.rstrip().rfind("}")
        t = t[:fim] + PONTE_USE + "}\n"
    m = re.search(r"public (?:abstract )?class " + nome + r" extends (BaseEntityBlock|HorizontalDirectionalBlock|HorizontalWaterloggableEntityBlock|DirectionalBlock)\b", t)
    if m and "codec()" not in t and "abstract class" not in m.group(0):
        base = "BaseEntityBlock" if m.group(1) == "HorizontalWaterloggableEntityBlock" else m.group(1)
        simples = re.search(r"public " + nome + r"\((?:BlockBehaviour\.)?Properties \w+\)", t)
        corpo = f"simpleCodec({nome}::new)" if simples else "null"
        metodo = (f"\n    // 1.21: block types need a codec (never serialized by type here)\n    @Override\n"
                  f"    protected com.mojang.serialization.MapCodec<? extends {base}> codec() {{\n        return {corpo};\n    }}\n")
        abre = t.index("{", m.end())
        t = t[:abre + 1] + metodo + t[abre + 1:]
    return t


n = 0
for f in SRC.rglob("*.java"):
    t0 = t = f.read_text(encoding="utf-8")
    for a, b in TROCAS:
        t = re.sub(a, b, t)
    t = vertices(t)
    t = blocos(t, f.stem)
    t = provedor(t, f.stem)
    t = tirar_override(t, ["getCapability", "invalidateCaps"])
    # subclasses do nosso FluidHandlerBlockEntity: o "super" volta a dar o tanque
    if "extends FluidHandlerBlockEntity" in t:
        t = re.sub(r"(LazyOptional<T> getCapability\(Capability<T> (\w+), (?:@\w+ )?Direction (\w+)\) \{.*?)return LazyOptional\.empty\(\);",
                   r"\1return super.getCapability(\2, \3);", t, flags=re.S)
    if "CraftingInput" in t:
        t = re.sub(r"\b(\w+)\.getContainerSize\(\)", r"\1.size()", t)
        t = re.sub(r"\b(\w+)\.getWidth\(\)", r"\1.width()", t)
        t = re.sub(r"\b(\w+)\.getHeight\(\)", r"\1.height()", t)
    pacote = re.search(r"^package [\w.]+;\n", t, re.M)
    for uso, imp in {**IMPORTS, **IMPORTS_RECEITA}.items():
        if uso in t and f"import {imp};" not in t and not f.name.startswith(imp.rsplit(".", 1)[1]):
            t = t[:pacote.end()] + f"import {imp};\n" + t[pacote.end():]
    if t != t0:
        f.write_text(t, encoding="utf-8")
        n += 1
print(n, "arquivos alterados")
