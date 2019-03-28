package org.dragonet.mcauthserver;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientPluginMessagePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import org.dragonet.mcauthserver.tasks.PlayerAccountTask;
import org.dragonet.mcauthserver.utils.Lang;
import org.dragonet.mcauthserver.utils.SessionUtils;

import java.io.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 2017/9/26.
 */
public class AuthSession extends SessionAdapter {

    public static final String FLAG_REAL_IP_ADDRESS = "AuthServer_RealIP";


    private final PlayerAccountTask.AccountCallback cb;
    private final Session session;

    private final AtomicBoolean logged_in = new AtomicBoolean(false);

    // store the first one to compare with the second one
    private String passwordCached = null;

    public AuthSession(Session session) {
        this.session = session;
        cb = ((success, message, username, uuid) -> {
            if(!success) {
                session.disconnect(message);
            } else {
                logged_in.set(true);
                SessionUtils.sendChat(session, Lang.PLAYER_SUCCESS.build());
                // update username and uuid to make it case friendly
                // or one username with different cases can have many accounts!
                sendBungeeCordMessage("UltimateRoles", c -> {
                    c.writeUTF("UpdateProfile");
                    c.writeUTF(username);
                    c.writeUTF(uuid);
                });
                // sendPlayer();
            }
        });
    }

    @Override
    public void packetReceived(PacketReceivedEvent event) {
        if(ClientPluginMessagePacket.class.isAssignableFrom(event.getPacket().getClass())) {
            ClientPluginMessagePacket pm = event.getPacket();
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(pm.getData()));
            try {
                if(pm.getChannel().equals("BungeeCord")) {
                    String subchannel = dis.readUTF();
                    if (subchannel.equals("IP")) {
                        String ip = dis.readUTF();
                        session.setFlag(FLAG_REAL_IP_ADDRESS, ip);
                        GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
                        AuthServer.instance.getLogger().info(Lang.SERVER_PLAYER_IP.build(profile.getName(), ip));
                    }
                }
                if(pm.getChannel().equals("UltimateRoles")) {
                    String subchannel = dis.readUTF();
                    if (subchannel.equals("UpdateProfile")) {
                        if(logged_in.get()) {
                            sendPlayer();
                        }
                    }
                }
            }catch (Exception e) {
                e.printStackTrace();
            }finally {
                try {dis.close();} catch (Exception e) {}
            }
        }
        if(ClientChatPacket.class.isAssignableFrom(event.getPacket().getClass())) {
            ClientChatPacket chat = event.getPacket();
            onChat(chat.getMessage());
        }
    }

    private void onChat(String message) {
        if(!session.hasFlag(AuthProcessor.FLAG_LOGIN_KEY) || ! session.hasFlag(FLAG_REAL_IP_ADDRESS)) {
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
        Future f = AuthServer.instance.getProcessor().getThreads().submit(new PlayerAccountTask(session, passwordCached, true, cb));
        session.setFlag(AuthProcessor.FLAG_ACCOUNT_TASK_KEY, f);
    }

    private void onLogin() {
        // set into "loading" mode
        session.getFlags().remove(AuthProcessor.FLAG_LOGIN_KEY);
        SessionUtils.sendChat(session, Lang.PLAYER_LOGGING_IN.build());
        Future f = AuthServer.instance.getProcessor().getThreads().submit(new PlayerAccountTask(session, passwordCached, false, cb));
        session.setFlag(AuthProcessor.FLAG_ACCOUNT_TASK_KEY, f);
    }

    private void sendPlayer() {
        sendBungeeCordMessage("Connect", (dos) ->
            dos.writeUTF(AuthServer.instance.getProperties().getProperty("lobby-server"))
        );
    }

    public void sendRequestForIP() {
        sendBungeeCordMessage("IP");
    }

    public void sendBungeeCordMessage(byte[] data) {
        ServerPluginMessagePacket pluginMessage = new ServerPluginMessagePacket("BungeeCord", data);
        session.getFlags().remove(AuthProcessor.FLAG_ACCOUNT_TASK_KEY);
        session.send(pluginMessage);
    }

    public void sendBungeeCordMessage(String subchannel) {
        sendBungeeCordMessage(subchannel, null);
    }

    public void sendBungeeCordMessage(String subchannel, PluginMessageConstructor constructor) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(subchannel);
            if(constructor != null) constructor.construct(dos);
            dos.close();
            byte[] payload = bos.toByteArray();
            sendBungeeCordMessage(payload);
        }catch (Exception e){
            e.printStackTrace();
            session.disconnect(Lang.SERVER_ERROR.build());
        }
    }

    public interface PluginMessageConstructor {
        void construct(DataOutputStream dos) throws IOException;
    }
}
