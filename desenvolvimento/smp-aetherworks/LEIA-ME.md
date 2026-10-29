# Aetherworks Refracted — port 1.21.1 (NeoForge) para o SMP 2

Port não oficial do **Aetherworks Refracted 1.20.1-1.1.8** (Sirplop, **GPLv3**) para Minecraft 1.21.1 / NeoForge 21.1 /
**Embers Re-Ignited 1.5.8**. Fonte original: <https://github.com/Sirplop/AetherworksRefracted>, commit `06c80407`.

> GPLv3: ao distribuir o modpack com este jar, o código-fonte modificado (esta pasta) precisa estar disponível
> junto ou por link. A licença vai dentro do jar (`META-INF/LICENSE_aetherworks.txt`).

## Pastas

| Pasta | O quê |
|---|---|
| `original/` | Fonte original intocada (só leitura) |
| `src/main/java` | Fonte portada (editar aqui) |
| `src/main/resources` | **Gerada** por `tools/recursos.py` a partir de `original/` — não editar à mão |
| `tools/` | `converter.py` (passo mecânico, já rodado), `fase2.py` (trocas idempotentes), `recursos.py` (dados), `erros.py` (agrupa erros do javac), `so_constantes.py` |

```powershell
python -I smp-aetherworks\tools\recursos.py
powershell -ExecutionPolicy Bypass -File smp-aetherworks\build.ps1 -Instalar   # gera mods\aetherworks-1.21.1-1.1.8-smp.jar
python -I smp-aetherworks\tools\erros.py                                      # se o javac falhar
```

## O que mudou (e por quê)

- **Capabilities**: o Embers Re-Ignited mantém uma camada legada (`com.rekindled.embers.compat.legacy`). Os block entities
  continuam com `getCapability(Capability, Direction)`; `AWRegistry#registerCapabilities` expõe isso como capabilities do
  NeoForge (itens, fluidos, ember) — a mesma ponte que o Re-Ignited usa. Chamadas em outros objetos passam por `CapabilityCompat`.
- **Dados de item** (NBT) → componente `custom_data` via `util/Compat121` (ler = cópia; escrever = `Compat121.edit`).
  Gema de poção usa `potion_contents`; a besta carregada usa `charged_projectiles`; a pá de prismarinho, o componente `aetherworks:fluid_contents`.
- **Aetheriômetro**: o valor de éter por chunk virou *data attachment* (`aetherworks:aether`), no lugar da capability de chunk.
- **Receitas**: serializers herdam `LegacyRecipeSerializer` do Embers (mesmo JSON de antes); contextos usam `recipe/RecipeWrapper`
  (o de 1.20, que o NeoForge 1.21 tirou). Receitas de bancada: `CraftingInput`.
- **Rede**: as 9 mensagens antigas viajam dentro de payloads (`network/PacketHandler`, `PacketDistributor`, `NetworkEvent` imitam a API antiga).
- **Encantamento Aetheric**: já vinha desligado no original; removido. Itens de Aetherium continuam se consertando ao luar.
- **Tier Aetherium**: sem `TierSortingRegistry` em 1.21 — ferramentas até netherite não colhem `#aetherworks:needs_aetherium_tool`
  (tags `incorrect_for_*_tool`).
- **Ferramentas de área**: o golpe em área roda em `mineBlock` (o `onBlockStartBreak` sumiu do NeoForge).
- **Modelo do Lexicon Receptacle**: modelo avulso registrado (`ModelEvent.RegisterAdditional`), sem access transformer.
- **Dados**: pastas no formato 1.21, tags `forge:` → `c:`, resultados `{"id": ...}`, avanços de receita (livro de receitas) fora.
- Datagen removido (a saída já vem pronta em `original/src/generated`).

## Não testado em jogo (o usuário pediu testes só offline)

Compila contra os jars instalados e os dados passam pelo gerador do SMP, mas **nada disso rodou no jogo ainda**.
Pontos de maior risco, nesta ordem: abrir o mundo (registros/receitas), a Forja do Aether montada (tick, calor, ember),
prisma + amplificador gerando gás de éter, ferramentas de área, besta, escudo, coroa (modelo e gema), JEI, meteoros (estrutura).
