package org.dragonet.mcauthserver;

/**
 * Created on 2017/9/26.
 */
public class PlayerStatusInfo {

    public final String username;

    public final long time = System.currentTimeMillis();

    public PlayerStatusInfo(String username) {
        this.username = username;
    }

    public long timeDiff() {
        return System.currentTimeMillis() - time;
    }

    @Override
    public int hashCode() {
        return username.toLowerCase().hashCode();
    }
}
