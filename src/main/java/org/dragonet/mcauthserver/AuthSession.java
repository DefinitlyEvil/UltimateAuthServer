package org.dragonet.mcauthserver;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import org.dragonet.mcauthserver.tasks.PlayerAccountTask;
import org.dragonet.mcauthserver.utils.Lang;
import org.dragonet.mcauthserver.utils.SessionUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.concurrent.Future;

/**
 * Created on 2017/9/26.
 */
public class AuthSession extends SessionAdapter {

    private final Session session;

    // store the first one to compare with the second one
    private String passwordCached = null;

    public AuthSession(Session session) {
        this.session = session;
    }

    @Override
    public void packetReceived(PacketReceivedEvent event) {
        if(ClientChatPacket.class.isAssignableFrom(event.getPacket().getClass())) {
            ClientChatPacket chat = event.getPacket();
            onChat(chat.getMessage());
        }
    }

    private void onChat(String message) {
        if(!session.hasFlag(AuthProcessor.FLAG_LOGIN_KEY)) {
            SessionUtils.sendChat(session, Lang.PLAYER_STILL_LOADING.build());
            return;
        }

        String lowered = message.toLowerCase();
        if(lowered.startsWith("/register") || lowered.startsWith("/login") || lowered.startsWith("/reg ") || lowered.startsWith("/l ")) {
            SessionUtils.sendChat(session, Lang.PLAYER_NOTICE_COMMAND.build());
            return;
        }

        boolean registered = session.getFlag(AuthProcessor.FLAG_LOGIN_KEY);
        if(!registered) {
            if(passwordCached == null) {
                // first password
                passwordCached = message;
                SessionUtils.sendChat(session, Lang.PLAYER_REGISTER_REPEAT.build());
            } else {
                if(message.equals(passwordCached)) {
                    onRegister();
                } else {
                    SessionUtils.sendChat(session, Lang.PLAYER_PASSWORD_MISMATCH.build());
                    passwordCached = null;
                }
            }
        } else {
            passwordCached = message;
            onLogin();
        }
    }

    private void onRegister() {
        // set into "loading" mode
        session.getFlags().remove(AuthProcessor.FLAG_LOGIN_KEY);
        SessionUtils.sendChat(session, Lang.PLAYER_REGISTERING.build());
        Future f = AuthServer.instance.getProcessor().getThreads().submit(new PlayerAccountTask(session, passwordCached, true, ((success, message) -> {
            if(!success) {
                session.disconnect(message);
            } else {
                SessionUtils.sendChat(session, Lang.PLAYER_SUCCESS.build());
                sendPlayer();
            }
        })));
        session.setFlag(AuthProcessor.FLAG_ACCOUNT_TASK_KEY, f);
    }

    private void onLogin() {
        // set into "loading" mode
        session.getFlags().remove(AuthProcessor.FLAG_LOGIN_KEY);
        SessionUtils.sendChat(session, Lang.PLAYER_LOGGING_IN.build());
        Future f = AuthServer.instance.getProcessor().getThreads().submit(new PlayerAccountTask(session, passwordCached, false, ((success, message) -> {
            if(!success) {
                session.disconnect(message);
            } else {
                SessionUtils.sendChat(session, Lang.PLAYER_SUCCESS.build());
                sendPlayer();
            }
        })));
        session.setFlag(AuthProcessor.FLAG_ACCOUNT_TASK_KEY, f);
    }

    private void sendPlayer() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF("Connect");
            dos.writeUTF(AuthServer.instance.getProperties().getProperty("lobby-server"));
            dos.close();
            bos.close();
            byte[] payload = bos.toByteArray();
            ServerPluginMessagePacket pluginMessage = new ServerPluginMessagePacket("BungeeCord", payload);
            session.getFlags().remove(AuthProcessor.FLAG_ACCOUNT_TASK_KEY);
            session.send(pluginMessage);
        }catch (Exception e){
            e.printStackTrace();
            session.disconnect(Lang.SERVER_ERROR.build());
        }
    }
}
