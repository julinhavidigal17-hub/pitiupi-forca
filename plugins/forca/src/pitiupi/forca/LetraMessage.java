package pitiupi.forca;

import pitiupi.models.Messages.Message;

public class LetraMessage extends Message {

    private String idPartida;
    private char letra;
    private String nome;

    public LetraMessage() {
    }

    public LetraMessage(String idPartida, char letra, String nome) {
        this.idPartida = idPartida;
        this.letra = letra;
        this.nome = nome;
    }

    public String getIdPartida() {
        return idPartida;
    }

    public char getLetra() {
        return letra;
    }

    public String getNome() {
        return nome;
    }
}
