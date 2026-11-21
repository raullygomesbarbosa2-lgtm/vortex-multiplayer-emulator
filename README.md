# Vortex Online

Projeto novo para consoles clássicos sem sistemas Sony, com interface web, wrapper Android e infraestrutura experimental de netplay.

## O que existe hoje

- Site público e responsivo: https://raullygomesbarbosa2-lgtm.github.io/vortex-multiplayer-emulator/
- APK Beta Android 6+: app WebView que abre o site e permite selecionar ROM local: https://github.com/raullygomesbarbosa2-lgtm/vortex-multiplayer-emulator/releases/download/android-beta-1/app-debug.apk
- Seleção de ROM local, carregamento de núcleos EmulatorJS e identificação abreviada SHA-256 para comparar os arquivos.
- Servidor separado EmulatorJS-Netplay configurado e respondendo por HTTPS.
- **Não validamos uma partida real entre dois jogadores. A FAQ oficial do EmulatorJS ainda informa que netplay não é suportado; portanto trate esta etapa como protótipo, não multiplayer comprovado.**
- A compilação do APK passou em CI, mas instalação/execução no telefone e compatibilidade completa não foram testadas em aparelho real.

## Sistemas na interface

NES/Famicom, SNES, Sega Mega Drive/Genesis, Master System, Game Gear, Game Boy, Game Boy Color, Game Boy Advance, Nintendo 64, Atari 2600 e Arcade/FBNeo. Disponibilidade depende dos núcleos e do navegador; netplay pode não funcionar em alguns ou todos.

## ROMs e direitos

O jogador seleciona um arquivo que já possui. O projeto não contém ROMs nem BIOS e não oferece downloads de jogos comerciais. Os dois jogadores precisam de cópias idênticas da mesma ROM; o ID é uma conferência auxiliar.

## Execução local

Servir esta pasta por HTTP(S) e abrir `index.html`; não usar `file://`. O site carrega EmulatorJS e núcleos do CDN oficial, então precisa de internet.

## Referências

- EmulatorJS: https://emulatorjs.org/
- FAQ oficial (status netplay): https://emulatorjs.org/docs/faq/
- Servidor EmulatorJS-Netplay: https://github.com/EmulatorJS/EmulatorJS-Netplay
