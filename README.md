# Vortex Online

Projeto novo de emulador multiplataforma focado em partidas online entre jogadores à distância. Não inclui sistemas Sony nem distribui jogos/BIOS/ROMs.

## Estado desta primeira etapa

- Interface web responsiva em português e seleção local de ROM.
- Catálogo inicial de sistemas não-Sony configurado para carregar núcleos do EmulatorJS.
- Identificação SHA-256 abreviada da ROM para os jogadores conferirem se escolheram o mesmo arquivo.
- Servidor separado EmulatorJS-Netplay provisionado para este projeto; a partida ainda precisa ser validada com duas pessoas.
- Ainda não há APK Android. A etapa atual é validar o fluxo no navegador antes de empacotar um app.

## Sistemas iniciais configurados

NES/Famicom, SNES, Sega Mega Drive/Genesis, Master System, Game Gear, Game Boy, Game Boy Color, Game Boy Advance, Nintendo 64, Atari 2600 e Arcade/FBNeo. Disponibilidade e netplay podem variar por núcleo e navegador.

## ROMs e direitos

O jogador seleciona um arquivo que já possui. O projeto não contém ROMs nem BIOS e não oferece links para baixar jogos comerciais. Ambos os jogadores precisam de cópias idênticas da mesma ROM; o ID mostrado é uma verificação auxiliar.

## Execução local

Servir esta pasta por HTTP(S) (não abrir como `file://`) e acessar `index.html`. A aplicação carrega EmulatorJS e seus núcleos do CDN oficial, portanto precisa de internet.

## Tecnologias / referências

- Interface própria do projeto.
- [EmulatorJS](https://emulatorjs.org/) e seus núcleos suportados.
- [EmulatorJS Netplay](https://github.com/EmulatorJS/EmulatorJS-Netplay) — servidor separado usado para partidas online.
