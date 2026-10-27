# Black Hole Infinite

Mod para **Minecraft 1.8.9 (Forge 11.15.1.2318)** que adiciona um buraco negro que **cresce continuamente**, absorve blocos e entidades e amplia seu alcance gravitacional conforme ganha massa. Não existe estágio final nem tamanho máximo configurável.

> ⚠️ **Status:** o código foi escrito, mas ainda **não foi compilado nem testado no jogo**. Se encontrar erros de compilação, abra uma *Issue* com a mensagem completa.

---

## Índice
1. [Recursos](#recursos)
2. [Requisitos](#requisitos)
3. [Compilar (Windows)](#compilar-windows)
4. [Instalar](#instalar)
5. [Como usar](#como-usar)
6. [Como funciona](#como-funciona)
7. [Configuração](#configuração)
8. [Estrutura do repositório](#estrutura-do-repositório)
9. [Solução de problemas](#solução-de-problemas)
10. [Contribuir](#contribuir)

---

## Recursos
- Crescimento contínuo: absorver → mais massa → mais gravidade → atrair matéria mais distante → absorver mais.
- **Raio gravitacional** e **raio de destruição** separados (a gravidade alcança muito além do que é consumido).
- Atração de itens, animais, monstros, XP, TNT, barcos e carrinhos; jogadores configuráveis.
- Varredura incremental de blocos por anéis de chunks (nunca varre o raio inteiro em um tick).
- Efeitos: esfera negra 3D, disco de acreção roxo, anéis gravitacionais, partículas em espiral, sons graves.
- Estado salvo no mundo; contador de blocos em `BigInteger`.
- Item **Black Hole Generator** e comandos `/blackhole`.

## Requisitos
| Item | Versão |
|---|---|
| JDK | **8** (não use 17/21) |
| Minecraft | 1.8.9 |
| Forge | 1.8.9-11.15.1.2318 |
| Gradle | o do MDK (`gradlew`) |

## Compilar (Windows)
1. Instale o **JDK 8** e configure `JAVA_HOME`.
2. Baixe o **Forge MDK 1.8.9** em files.minecraftforge.net e extraia.
3. Copie a pasta `src` deste repositório para dentro da pasta do MDK (substituindo `src`).
   - Se o `build.gradle` deste repositório falhar por repositórios antigos, mantenha o do MDK e altere apenas `version`, `group` e `archivesBaseName`.
4. No terminal, dentro da pasta do MDK:
   ```
   gradlew setupDecompWorkspace
   gradlew build
   ```
5. O jar sai em `build\libs\BlackHoleInfinite-1.0.0.jar`.
6. Para testar sem instalar: `gradlew runClient`.

## Compilar pelo GitHub (sem instalar nada)
O workflow `.github/workflows/build.yml` compila o mod a cada push na `main`.
1. Faça push do repositório para o GitHub.
2. Aba **Actions** → execução **Build mod** (ou **Run workflow** para rodar manualmente).
3. Ao terminar, abra a execução e baixe o **BlackHoleInfinite-jar** em *Artifacts* (zip com o `.jar` dentro).
4. Para publicar em **Releases**: `git tag v1.0.0` → `git push origin v1.0.0`. O jar é anexado automaticamente.

## Instalar
1. Instale o Forge 1.8.9 (11.15.1.2318) pelo instalador oficial.
2. Copie o jar para `%appdata%\.minecraft\mods`.
3. Abra o launcher e use o perfil **Forge**.

## Como usar

### Comandos (permissão nível 2 / cheats ligados)
| Comando | O que faz |
|---|---|
| `/blackhole spawn` | Cria um buraco negro na sua posição |
| `/blackhole remove` | Remove todos os buracos negros ativos |
| `/blackhole info` | Massa, blocos absorvidos, raios (visual, gravitacional, destruição) e crescimento |
| `/blackhole status` | Se está crescendo, atividade atual e progresso da varredura |

### Item
**Black Hole Generator** (aba Miscelânea). Clique direito para criar um buraco negro onde você mira.
Receita: 8 obsidianas + 1 estrela do Nether (anel de obsidiana com a estrela no centro).

### Roteiro de teste rápido
1. Mundo criativo com cheats → `/blackhole spawn`.
2. Rode `/blackhole info` a cada poucos segundos: massa e alcance devem subir.
3. Solte itens e mobs dentro do alcance: serão puxados e absorvidos.
4. Saia e reabra o mundo: o estado deve ser restaurado.
5. `/blackhole remove` para limpar.

## Como funciona
- Alcance gravitacional: `base × massa^expoente` (padrão 5 × massa^0,5 → 1 bloco = 5, 100 = 50, 1.000.000 = 5.000).
- Raio de destruição: `base × massa^0,45` (menor que o gravitacional).
- Velocidade de absorção por tick cresce com `log10(massa)`, limitada por `maxAbsorbPerTick`.
- A massa usa `double` com proteção contra NaN/Infinity (limite prático ≈ 1e298, imposto pelo tipo numérico).
- O raio **desenhado** é limitado pela distância de renderização; a lógica continua crescendo.
- Só chunks **carregados** são modificados (salvo `globalExpansion=true`).

## Configuração
Arquivo gerado em `config/blackholeinfinite.cfg`. Principais opções:

| Opção | Efeito |
|---|---|
| `startMass`, `massPerBlock`, `growthSpeed` | Crescimento |
| `gravityRadiusBase`, `gravityRadiusExponent` | Fórmula do alcance |
| `gravityIntensity`, `suctionSpeed` | Força de sucção |
| `destroyRadiusBase`, `destroyRadiusExponent` | Raio de destruição |
| `maxAbsorbPerTick`, `columnsPerTick`, `maxScanRadiusBlocks` | Desempenho (só limitam trabalho por tick) |
| `playersPulled`, `killPlayers` | Jogadores são puxados e morrem no núcleo |
| `visualToDestroyRatio` | Tamanho da bola = raio de destruição × razão |
| `flyingBlocksPerSync` | Blocos animados sendo puxados |
| `protectedBlocks`, `absorbableBlocksWhitelist` | Regras de blocos |
| `globalExpansion` | Carrega chunks para continuar expandindo (pesado) |
| `visualEffects`, `sounds` | Efeitos |

## Estrutura do repositório
```
build.gradle
src/main/java/com/blackholeinfinite/
  BlackHoleMod.java          entrada do mod
  BHConfig.java              configuração
  BHMath.java                fórmulas estáveis
  BlackHole.java             estado de um buraco negro
  BlackHoleEngine.java       simulação (blocos e entidades)
  BlackHoleManager.java      criação, remoção, sincronização
  BlackHoleSavedData.java    persistência
  BlackHoleTicker.java       tick do servidor
  CommandBlackHole.java      comandos
  ItemBlackHoleGenerator.java item
  SyncMessage.java           rede servidor → cliente
  ClientProxy.java / CommonProxy.java
  HoleRenderer.java          renderização
src/main/resources/          mcmod.info, textura, modelo, idiomas
```

## Solução de problemas
| Problema | Solução |
|---|---|
| `Unsupported class file version` | Está usando JDK diferente de 8 |
| `ForgeGradle` não baixa dependências | Use o `build.gradle` do MDK e só altere o nome/versão |
| Erro em `addTabCompletionOptions`, `getHeightValue`, `getMapStorage` ou `getPositionVector` | Confira o nome do método na sua versão de mapeamentos (`stable_22`) |
| Buraco negro não consome nada | Verifique se há chunks carregados ao redor e se os blocos não estão em `protectedBlocks` |
| Servidor lento | Reduza `maxAbsorbPerTick`, `columnsPerTick` e `maxScanRadiusBlocks` |

## Contribuir
1. Faça um *fork* e crie uma branch: `git checkout -b minha-melhoria`.
2. Commit e push: `git commit -m "Descrição"` → `git push origin minha-melhoria`.
3. Abra um **Pull Request** descrevendo a mudança e como testou.

Relatos de bug: abra uma *Issue* com versão do Forge, log (`logs/latest.log`) e passos para reproduzir.

## Licença
Escolha uma licença (por exemplo MIT) e adicione um arquivo `LICENSE`. Sem licença, ninguém pode legalmente reutilizar o código.
