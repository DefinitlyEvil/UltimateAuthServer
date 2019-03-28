package org.dragonet.mcauthserver.tasks;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.packetlib.Session;
import com.google.gson.JsonObject;
import org.dragonet.mcauthserver.AuthProcessor;
import org.dragonet.mcauthserver.AuthServer;
import org.dragonet.mcauthserver.utils.Lang;
import org.dragonet.mcauthserver.utils.URUtils;

import java.net.InetSocketAddress;
import java.net.URLEncoder;

/**
 * Created on 2017/9/26.
 */
public class PlayerStatusChecker implements Runnable {

    private final Session session;
    private final GameProfile player;
    private final PlayerStatusCallback callback;

    public PlayerStatusChecker(Session session, GameProfile player, PlayerStatusCallback callback) {
        this.session = session;
        this.player = player;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            JsonObject result = URUtils.request("/account/checkRegistered",
                    "username=" + URLEncoder.encode(player.getName(), "UTF-8") + "&" + "client_ip=" + URLEncoder.encode(((InetSocketAddress)session.getRemoteAddress()).getHostString(), "UTF-8")
            );
            if(result == null) {
                throw new RuntimeException(Lang.SERVER_API_FAILED.build());
            }
            String error = URUtils.checkError(result);
            if(error != null) {
                AuthServer.instance.getLogger().severe(Lang.SERVER_API_FAILED.build());
                session.disconnect(Lang.SERVER_API_FAILED.build());
                return;
            }
            session.getFlags().remove(AuthProcessor.FLAG_CHECK_TASK_KEY);
            callback.call(result.get("registered").getAsBoolean());
        } catch (Exception e) {
            e.printStackTrace();
            session.disconnect(Lang.SERVER_ERROR.build());
        }
    }

    public interface PlayerStatusCallback {
        void call(boolean registered);
    }
}
