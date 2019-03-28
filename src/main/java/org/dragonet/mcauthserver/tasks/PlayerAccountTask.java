package org.dragonet.mcauthserver.tasks;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.packetlib.Session;
import com.google.gson.JsonObject;
import org.dragonet.mcauthserver.AuthProcessor;
import org.dragonet.mcauthserver.AuthSession;
import org.dragonet.mcauthserver.utils.Lang;
import org.dragonet.mcauthserver.utils.SessionUtils;
import org.dragonet.mcauthserver.utils.URUtils;

import java.net.URLEncoder;

/**
 * Created on 2017/9/26.
 */
public class PlayerAccountTask implements Runnable {

    private final Session session;
    private final String password;
    private final boolean register;
    private final AccountCallback callback;

    public PlayerAccountTask(Session session, String password, boolean register, AccountCallback callback) {
        this.session = session;
        this.password = password;
        this.register = register;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
            String ep = "/account/" + (register ? "register" : "checkLogin");
            String args = "username=" + URLEncoder.encode(profile.getName(), "UTF-8")
                    + "&password=" + URLEncoder.encode(password, "UTF-8")
                    + "&client_ip=" + URLEncoder.encode(session.getFlag(AuthSession.FLAG_REAL_IP_ADDRESS), "UTF-8");
            if(register) {
                args += "&uuid=" + URLEncoder.encode(SessionUtils.getUUID(profile).toString(), "UTF-8");
            }
            JsonObject result = URUtils.request(ep, args);
            String error = URUtils.checkError(result);
            if(error != null) {
                session.getFlags().remove(AuthProcessor.FLAG_ACCOUNT_TASK_KEY);
                callback.call(false, Lang.PLAYER_FAILED.build(register ? Lang.ACTION_REGISTRATION.build() : Lang.ACTION_LOGIN.build(), error), null, null);
                return;
            }
            callback.call(true, null, result.get("username").getAsString(), result.get("uuid").getAsString());
        }catch (Exception e){
            e.printStackTrace();
            callback.call(false, Lang.SERVER_ERROR.build(), null, null);
        }
    }

    public interface AccountCallback {
        void call(boolean success, String message, String username, String uuid);
    }
}
