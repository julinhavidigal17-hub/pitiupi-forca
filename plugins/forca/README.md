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

### Sequência de uma partida

1. Uma pessoa preenche a palavra e a dica e clica em **Começar partida**. O computador dela
   gera um `idPartida` (UUID), começa a partida localmente e envia uma **NovaPartidaMessage**.
2. Todos os outros recebem a NovaPartidaMessage e começam a mesma partida: forca vazia,
   6 vidas, placar zerado.
3. Os jogadores chutam letras. Cada chute é aplicado primeiro no computador de quem chutou e
   depois enviado como **LetraMessage**. Os outros aplicam a mesma letra quando ela chega.
4. A partida termina, em todos os computadores, quando a palavra fica completa (vitória) ou
   quando chega a 6 erros (enforcado). Depois disso, novas LetraMessage são ignoradas.
5. Qualquer pessoa pode começar uma nova partida, que substitui a anterior.

### Turnos

O jogo **não tem turnos**. Qualquer jogador pode chutar uma letra a qualquer momento, e quem
chuta primeiro leva os pontos daquela letra. As únicas restrições são:

- quem escolheu a palavra não pode chutar;
- cada letra só pode ser chutada uma vez por partida;
- depois que a partida termina, ninguém chuta até começar uma nova.

As três são verificadas no computador de quem chuta, antes de enviar. As duas últimas também
são verificadas em quem recebe: `aplicarLetra` ignora letra repetida e partida encerrada.

### Mensagens perdidas, duplicadas ou fora de ordem

O UDP multicast não garante entrega nem ordem, e o plugin **não** faz confirmação nem reenvio.
O que acontece em cada caso:

| Situação | O que o plugin faz | Consequência |
|---|---|---|
| A própria mensagem volta pelo multicast (eco) | NovaPartidaMessage com o mesmo `idPartida` é ignorada; LetraMessage com letra já chutada é ignorada | Nada muda: o eco nunca conta duas vezes |
| Mensagem duplicada | Mesma regra do eco | Nada muda |
| NovaPartidaMessage perdida | Não há reenvio | Aquele computador não entra na partida; as LetraMessage dela são ignoradas, porque o `idPartida` não é o da partida atual |
| LetraMessage perdida | Não há reenvio | Aquele computador fica sem a letra: palavra, erros e placar diferentes dos outros até a próxima partida |
| LetraMessage de uma partida antiga chega depois de uma nova começar | Ignorada, porque o `idPartida` não bate | Nada muda |
| LetraMessage chega antes da NovaPartidaMessage | Ignorada, porque a partida ainda não existe ali | Aquele computador fica sem essa letra |
| Duas pessoas chutam a mesma letra quase ao mesmo tempo | Cada computador aceita a que chegou primeiro e ignora a outra | Os pontos daquela letra podem ir para pessoas diferentes em computadores diferentes |

Numa rede local, perda e troca de ordem são raras, então o jogo funciona bem na prática.
Para garantir consistência, uma melhoria possível seria o criador da palavra ser a autoridade
da partida: ele numeraria cada letra aceita e reenviaria periodicamente o estado completo
(letras, erros e placar) para todos.

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
- Não há confirmação nem reenvio de mensagens (veja a seção sobre mensagens perdidas).
- Quem abrir o plugin no meio de uma partida só entra na próxima.
- Duas pessoas com o mesmo nome somam pontos no mesmo lugar do placar.
- Uma partida nova substitui a que estava acontecendo.
