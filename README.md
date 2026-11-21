# Vortex Online — protótipo Android nativo e multiplayer alpha

A versão Android está sendo reconstruída para rodar jogos no aparelho sem WebView.

## O que há nesta alpha

- Emulação NES nativa com QuickNES, Android 6+ (ARM64 e ARMv7), controles touch.
- Seleção da própria ROM NES pelo usuário; aceita `.nes` e ZIP contendo um `.nes`, valida cabeçalho iNES; não incluímos ROMs/BIOS.
- Corrigido o nome do núcleo usado na busca no diretório nativo: o APK empacota `quicknes_libretro_android.so` sem o prefixo `lib`.
- Botões **CRIAR** e **ENTRAR** na partida para criar ou usar um código de sala.
- As salas usam o serviço Socket.IO hospedado e retransmitem eventos de controles para o outro participante, identificando a mesma ROM.
- Isso é **relay experimental de eventos**, não sincronização determinística quadro a quadro: não há rollback, buffer de atraso ou recuperação de estado integrada ao ciclo de emulação. Latência, ordem/tempo de chegada e início em momentos diferentes podem dessincronizar os jogos.
- Relay do servidor foi verificado com clientes de teste: criação de sala, entrada de outro cliente e entrega de evento `input` funcionaram. A integração do app Android, instalação, execução de jogo e partida entre dois telefones ainda precisam de teste real.
- Mesmo com o relay de eventos verificado, não há sincronização determinística por quadro; o jogo pode ficar dessincronizado.

## O que ainda falta para chamar de multiplayer funcionando

1. Validar instalação/abertura e carregamento de ROM num aparelho Android real.
2. Fazer uma sessão em dois celulares, redes distintas, verificar criação/entrada de sala e chegada de inputs nos dois lados.
3. Integrar comandos remotos ao mesmo número de quadro em ambos os núcleos, com política de atraso, sincronização de início e tratamento de perda/jitter.
4. Integrar estados salvos para recuperação de dessincronização (ou adotar outro protocolo adequado ao núcleo), medir latência e testar desconexão/reconexão.
5. Só então ampliar do NES para outros núcleos/sistemas, testando cada um separadamente.

Um relay de comandos não é prova de que o jogo está sincronizado. Os jogadores precisam do mesmo núcleo e do mesmo arquivo de jogo.

## Base e dependências

- [LibretroDroid](https://github.com/Swordfish90/LibretroDroid) (GPL-3.0), frontend nativo Android com estados salvos e controles.
- [QuickNES para Android](https://buildbot.libretro.com/nightly/android/latest/) (Libretro buildbot).
- Relay Socket.IO configurado no serviço deste projeto.
- Nenhuma ROM/BIOS comercial é fornecida.
