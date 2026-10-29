# Testador automático do SMP 2

Abre o Minecraft desta instância sozinho, cria um mundo novo, executa um roteiro de comandos e fecha o jogo.
Serve para conferir o modpack sem ninguém precisar jogar: receitas, bloqueios de profissão, pesquisas, rituais, materiais.

## Como rodar

```powershell
python -I smp-testes\rodar.py                        # roteiro principal (smp-testes\roteiro.txt)
python -I smp-testes\rodar.py roteiro_silentgear.txt # outro roteiro
python -I smp-testes\rodar.py --manter-aberto        # não fecha o jogo no fim
```

- Leva cerca de 11 minutos (a maior parte é carregar os 256 mods).
- Usa o Java 21 e as bibliotecas que o CurseForge já baixou; não baixa nada.
- O jogo abre em modo **offline**, com o jogador `SMPTeste`.
- Cria do zero o mundo `saves/smp_teste`, em criativo e no pacífico.
- **Não abra o jogo pelo CurseForge enquanto o teste roda**: os dois usariam a mesma pasta.

## O que sai

| Arquivo | Conteúdo |
|---|---|
| `smp-testes/resultado.txt` | cada linha do roteiro, com a saída do comando e `OK`/`FALHOU` |
| `smp-testes/erros_log.txt` | erros relevantes do `logs/latest.log` (receitas que não carregaram, mixins, crash) |
| `screenshots/smpteste_*.png` | fotos pedidas pelo roteiro |
| `smp-testes/saida_jogo.log` | saída completa do jogo |

## Formato do roteiro

```
## Título            → seção no resultado
# comentário         → ignorado
> comando            → roda como console (permissão máxima)
p> comando           → roda como o jogador do teste
espera 40            → espera 40 ticks (2 s)
foto nome            → screenshots/smpteste_nome.png
camera 1|2|3         → primeira pessoa, terceira por trás, terceira de frente
> comando => texto   → OK se a saída contém o texto
> comando !=> texto  → OK se a saída NÃO contém o texto
```

## Comandos de inspeção (`/smpteste`, só existem no modo de teste)

| Comando | Responde |
|---|---|
| `material <item>` | se o item é material do Silent Gear (id, nome, partes, categorias) |
| `gear <ferramenta> <peça> <material>` | monta uma ferramenta do Silent Gear e põe na mão do jogador |
| `receitas <item>` | receitas carregadas que produzem o item |
| `pode <licenças> <item>` | um jogador NÃO-OP com essas licenças (separadas por `+`, `-` = nenhuma) pode fabricar o item? |
| `craft <licenças> <receita>` | monta a grade da receita de bancada e pede ao jogo com esse jogador (testa a guarda do mod) |
| `itens <texto>` | ids de itens que contêm o texto |
| `pesquisa <id>` | nível, pai, requisitos e custo de uma pesquisa da MineColonies |
| `ramo <id>` | todas as pesquisas de um ramo |
| `registro <registro> <prefixo>` | quantas entradas de um registro de dados começam com o prefixo (ex.: rituais da Forja) |

## Como funciona

- `rodar.py` monta o comando de lançamento a partir dos perfis do CurseForge (`Install/versions/neoforge-21.1.253`).
- Ele copia `smp-testes/mod/build/smptestes-1.0.0.jar` para `mods/`, abre o jogo e tira o jar no fim, mesmo se der erro.
- O mod `smptestes` só age com `-Dsmp.teste=1`. Sem isso ele não registra nada.
- Código do mod: `smp-testes/mod/`. Para recompilar: `powershell -File smp-testes\mod\build.ps1`.
