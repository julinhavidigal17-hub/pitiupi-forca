package pitiupi.forca;

import java.util.List;
import javax.swing.SwingUtilities;
import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.application.messaging.MessageDispatcher;
import pitiupi.application.messaging.SendTarget;
import pitiupi.application.messaging.TransportMode;
import pitiupi.control.ConnectionManager;
import pitiupi.models.Messages.Message;
import pitiupi.models.Plugin;

public class ForcaPlugin extends Plugin {

    private ConnectionManager connectionManager;
    private ForcaPanel painel;

    @Override
    public String getName() {
        return "Forca";
    }

    @Override
    public String getAuthor() {
        return "Julia";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getIdentifier() {
        return "pitiupi.forca";
    }

    @Override
    public void createPlugin(ConnectionManager connectionManager, PluginWorkspace workspace) {
        this.connectionManager = connectionManager;
        painel = new ForcaPanel(this);
        bindShellWorkspaceContent(painel);
    }

    @Override
    public List<Class<? extends Message>> getMessages() {
        return List.of(NovaPartidaMessage.class, LetraMessage.class);
    }

    @Override
    public void receiveMessage(Message mensagem) {
        SwingUtilities.invokeLater(() -> {
            if (painel != null) {
                painel.receber(mensagem);
            }
        });
    }

    public void enviar(Message mensagem) {
        MessageDispatcher.send(connectionManager, mensagem, SendTarget.broadcastAll(), TransportMode.MULTICAST_UDP);
    }

    public String getMeuNome() {
        return connectionManager.getUserName();
    }
}
