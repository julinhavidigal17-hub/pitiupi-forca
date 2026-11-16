# Plugin Forca em Grupo - pitiupi

Plugin para o [pitiupi](https://git.alice.ufsj.edu.br/fls/pitiupi) com o jogo da forca para
várias pessoas ao mesmo tempo. Uma pessoa escolhe a palavra secreta e todo mundo da sala
chuta letras. A comunicação é **em grupo (multicast)**.

Autora: Júlia Paiva Vidigal

## Como funciona o jogo

- Uma pessoa digita a palavra secreta (sem acento) e uma dica e clica em **Começar partida**.
- A partida aparece para todos que estão na sala com o plugin aberto.
- Todos (menos quem escolheu a palavra) chutam letras. Cada letra só pode ser chutada uma vez.
- Quem acerta ganha 1 ponto para cada vez que a letra aparece na palavra.
- Cada letra errada desenha uma parte do boneco. Com 6 erros, todos perdem e a palavra é revelada.
- Quando a palavra fica completa, a partida acaba e o placar mostra quem fez mais pontos.

## Arquivos

```
plugins/forca/
├── README.md
├── build.xml
├── nbproject/
└── src/
    ├── META-INF/services/pitiupi.models.Plugin
    └── pitiupi/forca/
        ├── ForcaPlugin.java          classe do plugin, ligação com o pitiupi
        ├── ForcaPanel.java           tela e regras do jogo
        ├── NovaPartidaMessage.java   mensagem de nova partida
        └── LetraMessage.java         mensagem de letra chutada
```

## Como compilar e rodar

Precisa de Java 17 ou mais novo e do Apache Ant.

Entrada no `plugins.registry.json`:

```json
{
  "id": "forca",
  "dir": "plugins/forca",
  "build": "ant",
  "jar": "dist/pitiupi.forca.jar",
  "menuCategory": "Jogos"
}
```

Na pasta do pitiupi:

```bash
./scripts/pitiupi.sh run --all
```

## Como jogar

1. Abra o pitiupi em dois ou mais computadores da mesma rede, todos na mesma porta (ex.: 5000).
2. Na aba **Plugins**, abra a **Forca**.
3. Uma pessoa digita a palavra e a dica e clica em **Começar partida**.
4. As outras digitam uma letra e clicam em **Chutar letra** (ou apertam Enter).

## Protocolo

O plugin usa **UDP multicast**: toda mensagem vai para o grupo multicast do pitiupi
(`224.0.0.3`, na porta da sala) e todos da sala recebem. O plugin não abre sockets. Ele usa o
`MessageDispatcher` do pitiupi com `SendTarget.broadcastAll()` e `TransportMode.MULTICAST_UDP`.
As mensagens estendem `Message` e são enviadas com serialização Java, uma por datagrama.

### Mensagens

**NovaPartidaMessage**: enviada quando alguém começa uma partida.

| Campo | Tipo | Descrição |
|---|---|---|
| idPartida | String | identificador único da partida (UUID) |
| palavra | String | a palavra secreta, em maiúsculas |
| dica | String | a dica |
| criador | String | nome de quem escolheu a palavra |

**LetraMessage**: enviada quando alguém chuta uma letra.

| Campo | Tipo | Descrição |
|---|---|---|
| idPartida | String | partida em que a letra foi chutada |
| letra | char | a letra chutada |
| nome | String | nome de quem chutou |

### Regras de quem recebe

1. **NovaPartidaMessage** com um id diferente da partida atual: começa a nova partida (zera letras,
   erros e placar). Se o id for o mesmo da partida atual, ignora (é a própria mensagem voltando
   pelo multicast).
2. **LetraMessage** da partida atual:
   - se a partida já acabou ou a letra já foi chutada, ignora;
   - se a letra está na palavra, soma pontos para quem chutou (1 por vez que a letra aparece);
   - se não está, conta um erro;
   - se todas as letras foram descobertas, a partida acaba com vitória;
   - se chegou a 6 erros, a partida acaba e a palavra é revelada.
3. Cada computador aplica as mesmas regras às mesmas mensagens, então todos ficam com o mesmo jogo.

Exemplo:

```
Julia --NovaPartidaMessage(id=1, "BANANA", "fruta amarela", Julia)--> grupo
Beto  --LetraMessage(id=1, 'A', Beto)--> grupo     Beto +3 pontos
Caio  --LetraMessage(id=1, 'X', Caio)--> grupo     1 erro
Beto  --LetraMessage(id=1, 'B', Beto)--> grupo     Beto +1 ponto
Caio  --LetraMessage(id=1, 'N', Caio)--> grupo     Caio +2 pontos, palavra completa
```

### Limitações

- A palavra secreta viaja na mensagem sem criptografia. Quem capturar os pacotes da rede
  (por exemplo, com o Wireshark) consegue ver a palavra.
- UDP não garante entrega nem ordem. Se uma mensagem se perder, um computador pode ficar com o
  jogo diferente dos outros. Se duas pessoas chutarem a mesma letra quase juntas, cada
  computador dá o ponto para quem chegou primeiro nele.
- Quem abrir o plugin no meio de uma partida só entra na próxima.
- Uma partida nova substitui a que estava acontecendo.
