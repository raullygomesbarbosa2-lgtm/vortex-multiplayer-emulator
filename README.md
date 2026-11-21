# Vortex Online — protótipo nativo

Estamos reconstruindo a versão Android para rodar jogos no aparelho sem WebView. A versão web antiga continua publicada, mas o foco novo é um frontend Android nativo com núcleos Libretro.

## Estado real desta build

- Protótipo Android nativo (sem WebView) para Android 6+, com o núcleo NES QuickNES e controles touch.
- O usuário escolhe sua própria ROM NES pelo seletor de arquivos; nenhuma ROM/BIOS é distribuída.
- O primeiro APK nativo ainda é **single-player**: sala, envio de comandos remotos e sincronização multiplayer ainda não estão implementados.
- Compilação CI não equivale a teste de instalação, execução ou partida em dois celulares.
- A beta WebView anterior não representa o produto pedido e foi substituída pelo alvo nativo deste desenvolvimento.

## Plano de implementação

1. Validar o frontend e o núcleo nativos em aparelho.
2. Adicionar criação/entrada em sala e relay autenticado de comandos entre os dispositivos.
3. Sincronizar comandos no mesmo quadro, conferir ROM/núcleo/versões e recuperar diferenças de estado.
4. Validar uma partida real em dois celulares e só então ampliar núcleos/sistemas.

Um relay online sozinho não implementa netplay. A sincronização precisa ser integrada ao ciclo de execução do emulador; o desempenho e a estabilidade também variam por núcleo e aparelho.

## Dependências e licenças

- Frontend: [LibretroDroid](https://github.com/Swordfish90/LibretroDroid) (GPL-3.0).
- Núcleo NES: QuickNES para Android ARM64/ARMv7, vindo do [buildbot oficial Libretro](https://buildbot.libretro.com/nightly/android/latest/).
- Não inclui ROMs ou BIOS de jogos comerciais.
