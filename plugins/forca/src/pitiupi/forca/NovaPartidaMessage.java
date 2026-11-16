package pitiupi.forca;

import pitiupi.models.Messages.Message;

public class NovaPartidaMessage extends Message {

    private String idPartida;
    private String palavra;
    private String dica;
    private String criador;

    public NovaPartidaMessage() {
    }

    public NovaPartidaMessage(String idPartida, String palavra, String dica, String criador) {
        this.idPartida = idPartida;
        this.palavra = palavra;
        this.dica = dica;
        this.criador = criador;
    }

    public String getIdPartida() {
        return idPartida;
    }

    public String getPalavra() {
        return palavra;
    }

    public String getDica() {
        return dica;
    }

    public String getCriador() {
        return criador;
    }
}
