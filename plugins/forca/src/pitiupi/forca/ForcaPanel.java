package pitiupi.forca;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import pitiupi.models.Messages.Message;

public class ForcaPanel extends JPanel {

    private static final int MAX_ERROS = 6;

    private static final String[] DESENHOS = {
        "  +---+\n  |   |\n      |\n      |\n      |\n      |\n=========",
        "  +---+\n  |   |\n  O   |\n      |\n      |\n      |\n=========",
        "  +---+\n  |   |\n  O   |\n  |   |\n      |\n      |\n=========",
        "  +---+\n  |   |\n  O   |\n /|   |\n      |\n      |\n=========",
        "  +---+\n  |   |\n  O   |\n /|\\  |\n      |\n      |\n=========",
        "  +---+\n  |   |\n  O   |\n /|\\  |\n /    |\n      |\n=========",
        "  +---+\n  |   |\n  O   |\n /|\\  |\n / \\  |\n      |\n========="
    };

    private ForcaPlugin plugin;

    private String idPartida;
    private String palavra;
    private String dica;
    private String criador;
    private List<Character> letrasChutadas = new ArrayList<>();
    private int erros;
    private Map<String, Integer> pontos = new HashMap<>();
    private boolean fimDeJogo = true;

    private JPasswordField campoPalavra = new JPasswordField();
    private JTextField campoDica = new JTextField();
    private JButton botaoNovaPartida = new JButton("Começar partida");

    private JTextArea areaJogo = new JTextArea();

    private JTextField campoLetra = new JTextField(3);
    private JButton botaoChutar = new JButton("Chutar letra");
    private JLabel labelStatus = new JLabel("Crie uma partida ou espere alguém criar.");

    public ForcaPanel(ForcaPlugin plugin) {
        this.plugin = plugin;
        setLayout(new BorderLayout());

        JPanel painelNova = new JPanel(new GridLayout(3, 2));
        painelNova.add(new JLabel("Palavra secreta:"));
        painelNova.add(campoPalavra);
        painelNova.add(new JLabel("Dica:"));
        painelNova.add(campoDica);
        painelNova.add(new JLabel(""));
        painelNova.add(botaoNovaPartida);
        add(painelNova, BorderLayout.NORTH);

        areaJogo.setEditable(false);
        areaJogo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        add(new JScrollPane(areaJogo), BorderLayout.CENTER);

        JPanel painelChute = new JPanel(new BorderLayout());
        JPanel linhaChute = new JPanel();
        linhaChute.add(new JLabel("Letra:"));
        linhaChute.add(campoLetra);
        linhaChute.add(botaoChutar);
        painelChute.add(linhaChute, BorderLayout.NORTH);
        painelChute.add(labelStatus, BorderLayout.SOUTH);
        add(painelChute, BorderLayout.SOUTH);

        botaoNovaPartida.addActionListener(e -> novaPartida());
        botaoChutar.addActionListener(e -> chutar());
        campoLetra.addActionListener(e -> chutar());

        atualizarTela();
    }

    private void novaPartida() {
        String novaPalavra = new String(campoPalavra.getPassword()).trim().toUpperCase();
        String novaDica = campoDica.getText().trim();

        if (!novaPalavra.matches("[A-Z]+") || novaPalavra.length() < 3) {
            JOptionPane.showMessageDialog(this, "A palavra precisa ter pelo menos 3 letras, sem acento e sem espaço.");
            return;
        }

        String id = UUID.randomUUID().toString();
        iniciar(id, novaPalavra, novaDica, plugin.getMeuNome());
        plugin.enviar(new NovaPartidaMessage(id, novaPalavra, novaDica, plugin.getMeuNome()));

        campoPalavra.setText("");
        campoDica.setText("");
    }

    private void chutar() {
        if (fimDeJogo) {
            JOptionPane.showMessageDialog(this, "Não tem partida acontecendo.");
            return;
        }

        String nome = plugin.getMeuNome();
        if (nome.equals(criador)) {
            JOptionPane.showMessageDialog(this, "Você escolheu a palavra, então não pode chutar!");
            return;
        }

        String texto = campoLetra.getText().trim().toUpperCase();
        campoLetra.setText("");
        if (!texto.matches("[A-Z]")) {
            JOptionPane.showMessageDialog(this, "Digite só uma letra, sem acento.");
            return;
        }

        char letra = texto.charAt(0);
        if (letrasChutadas.contains(letra)) {
            JOptionPane.showMessageDialog(this, "A letra " + letra + " já foi chutada.");
            return;
        }

        plugin.enviar(new LetraMessage(idPartida, letra, nome));
        aplicarLetra(letra, nome);
    }

    public void receber(Message mensagem) {
        if (mensagem instanceof NovaPartidaMessage) {
            NovaPartidaMessage nova = (NovaPartidaMessage) mensagem;
            if (!nova.getIdPartida().equals(idPartida)) {
                iniciar(nova.getIdPartida(), nova.getPalavra(), nova.getDica(), nova.getCriador());
            }
        } else if (mensagem instanceof LetraMessage) {
            LetraMessage chute = (LetraMessage) mensagem;
            if (chute.getIdPartida().equals(idPartida)) {
                aplicarLetra(chute.getLetra(), chute.getNome());
            }
        }
    }

    private void iniciar(String id, String novaPalavra, String novaDica, String novoCriador) {
        idPartida = id;
        palavra = novaPalavra;
        dica = novaDica;
        criador = novoCriador;
        letrasChutadas.clear();
        erros = 0;
        pontos.clear();
        fimDeJogo = false;
        labelStatus.setText(criador + " começou uma partida!");
        atualizarTela();
    }

    private void aplicarLetra(char letra, String nome) {
        if (fimDeJogo || letrasChutadas.contains(letra)) {
            return;
        }
        letrasChutadas.add(letra);

        int acertos = 0;
        for (int i = 0; i < palavra.length(); i++) {
            if (palavra.charAt(i) == letra) {
                acertos++;
            }
        }

        if (acertos > 0) {
            pontos.put(nome, pontos.getOrDefault(nome, 0) + acertos);
            labelStatus.setText(nome + " acertou a letra " + letra + "!");
        } else {
            erros++;
            labelStatus.setText(nome + " errou a letra " + letra + ".");
        }

        if (palavraCompleta()) {
            fimDeJogo = true;
            labelStatus.setText("Descobriram a palavra! Era " + palavra + ".");
        } else if (erros >= MAX_ERROS) {
            fimDeJogo = true;
            labelStatus.setText("Enforcado! A palavra era " + palavra + ".");
        }
        atualizarTela();
    }

    private boolean palavraCompleta() {
        for (int i = 0; i < palavra.length(); i++) {
            if (!letrasChutadas.contains(palavra.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String palavraEscondida() {
        String texto = "";
        for (int i = 0; i < palavra.length(); i++) {
            char c = palavra.charAt(i);
            if (letrasChutadas.contains(c) || fimDeJogo) {
                texto += c + " ";
            } else {
                texto += "_ ";
            }
        }
        return texto;
    }

    private void atualizarTela() {
        if (palavra == null) {
            areaJogo.setText(DESENHOS[0] + "\n\nNenhuma partida ainda.");
            return;
        }

        String erradas = "";
        for (char c : letrasChutadas) {
            if (palavra.indexOf(c) < 0) {
                erradas += c + " ";
            }
        }

        String texto = DESENHOS[erros] + "\n\n";
        texto += "Dica: " + dica + "   (palavra de " + criador + ")\n\n";
        texto += "Palavra: " + palavraEscondida() + "\n\n";
        texto += "Letras erradas: " + erradas + "\n";
        texto += "Vidas: " + (MAX_ERROS - erros) + "\n\n";
        texto += "Placar:\n";
        for (String nome : pontos.keySet()) {
            texto += "  " + nome + ": " + pontos.get(nome) + " ponto(s)\n";
        }
        areaJogo.setText(texto);
    }
}
