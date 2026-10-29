# SMP 2 — Alpha 2

Modpack de RP medieval para **Minecraft 1.21.1** com **NeoForge 21.1.253**: colônias (MineColonies), profissões com
árvores de pesquisa (Engenheiro, Arcanista, Ferreiro, Naturalista), magia (Iron's Spells e addons, Malum, Botania),
tecnologia (Create, Immersive Engineering, Embers) e muito mais. São 277 arquivos do CurseForge, com a lista completa em
[MODS.md](MODS.md), mais os mods feitos para o servidor.

## Como instalar (CurseForge, recomendado)

1. Instale o [CurseForge App](https://www.curseforge.com/download/app), se ainda não tiver.
2. Baixe o arquivo **[SMP2.zip](https://github.com/JhonJRS/SMP2/raw/main/SMP2.zip)**. Não descompacte.
3. No CurseForge, abra **Minecraft**, clique em **Create Custom Profile** (canto superior direito) e escolha **Import**.
4. Selecione o `SMP2.zip` baixado e confirme.
   - O CurseForge baixa sozinho o NeoForge e todos os mods, nas mesmas versões do pack.
   - Pode demorar alguns minutos.
5. Antes de jogar, ajuste a memória: clique nos **⋯** do perfil, depois **Profile Options**, e desmarque
   *Use System Memory Settings*. Recomendamos **8 GB** (mínimo de 6 GB).
6. Clique em **Play**.

O primeiro carregamento é demorado. Isso é normal com mais de 270 mods.

## Atualizar para uma versão nova

Repita a importação com o `SMP2.zip` mais novo. Ela cria um perfil novo; para manter seus mundos, copie a pasta `saves`
do perfil antigo para o novo. No CurseForge, você abre a pasta de um perfil em **⋯ > Open Folder**.

## Outros launchers (Prism Launcher, ATLauncher, MultiMC)

O Prism Launcher também importa o zip: **Add Instance > Import** e selecione o `SMP2.zip`.

Em launchers que não leem o formato do CurseForge, instale o NeoForge 21.1.253 e cada mod listado em [MODS.md](MODS.md).
Depois copie o conteúdo de `overrides/` para a pasta da instância.

## Dicas

- O resource pack **Embers Retextured (SMP 2)** já vem incluído. Ative em *Opções > Pacotes de Recursos* se quiser.
- Os shaders (Complementary, BSL, Photon e outros) também já vêm instalados. Ative em *Opções > Vídeo > Shader Packs*
  (Iris).
- No servidor, use **exatamente** esta versão do pack. Mods diferentes impedem a entrada.
- Quem tiver problema ao abrir o jogo pode enviar o arquivo `logs/latest.log` da pasta do perfil.

## O que tem neste repositório

| Caminho | Conteúdo |
|---|---|
| `SMP2.zip` | Pack pronto para importar (é só ele que o jogador precisa) |
| `manifest.json` | Versões exatas de cada mod do CurseForge (formato de modpack do CurseForge) |
| `overrides/` | Configurações, datapacks, emotes, resource pack e os mods próprios que não estão no CurseForge |
| `MODS.md` | Lista de todos os mods com link |
| `desenvolvimento/` | Código-fonte dos mods próprios (SMP Profissões, Aetherworks port, Translucidez) e ferramentas |

Mods incluídos diretamente no pack, por não estarem no CurseForge nessa versão ou por serem próprios:
`smpprofissoes`, `smptranslucidez`, `aetherworks` (port 1.21.1), `coreprotectneo`, `SereneSeasons` e `GlitchCore`.

## Para quem mantém o pack

Para gerar um novo export a partir da instância do CurseForge, rode o comando abaixo e depois faça o commit:

```
python -I smp-profissoes/tools/exportar_modpack.py <pasta deste repositório>
```

Ele refaz `manifest.json`, `overrides/`, `MODS.md`, `SMP2.zip` e `desenvolvimento/`. Ficam de fora mundos, logs,
caches, o banco do CoreProtect e dados pessoais (skins, avatares, blueprints).
