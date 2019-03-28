package org.dragonet.mcauthserver;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.server.ServerAdapter;
import com.github.steveice10.packetlib.event.server.SessionRemovedEvent;
import org.dragonet.mcauthserver.tasks.PlayerStatusChecker;
import org.dragonet.mcauthserver.utils.Lang;
import org.dragonet.mcauthserver.utils.SessionUtils;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created on 2017/9/26.
 */
public class AuthProcessor extends ServerAdapter {

    public final static long CACHE_INVALIDATE_TIME = 10*60*1000L;
    public final static long CACHE_INVALIDATE_CHECK_INTERVAL = 5*60*1000L;

    public final static String FLAG_LOGIN_KEY = "AuthServer_Login";
    public final static String FLAG_CHECK_TASK_KEY = "AuthServer_CheckTask";
    public final static String FLAG_ACCOUNT_TASK_KEY = "AuthServer_AccountTask";

    private final AuthServer server;

    private ExecutorService threads;

    private Set<PlayerStatusInfo> playerCache = Collections.synchronizedSet(new HashSet<>());
    private Thread cacheCleaner = new Thread(){
        @Override
        public void run() {
            try{
                Thread.sleep(CACHE_INVALIDATE_CHECK_INTERVAL);
            } catch (Exception e){}
            cleanCache();
        }
    };

    public AuthProcessor(AuthServer server, int thread_count) {
        this.server = server;
        threads = Executors.newFixedThreadPool(thread_count);
        cacheCleaner.start();
    }

    public void onPlayerLoggedIn(Session session){
        GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
        session.addListener(new AuthSession(session));
        server.getLogger().info(Lang.SERVER_PLAYER_JOINED.build(profile.getName(), session.getRemoteAddress().toString()));
        boolean cached = playerCache.contains(profile.getName().toLowerCase());
        if(cached) {
            onPlayerStatusFetched(session, true);
        } else {
            SessionUtils.sendChat(session, Lang.PLAYER_LOADING.build());
            Future f = threads.submit(new PlayerStatusChecker(session, profile, (r) -> onPlayerStatusFetched(session, r)));
            session.setFlag(FLAG_CHECK_TASK_KEY, f);
        }
    }

    public void onPlayerStatusFetched(Session session, boolean registered) {
        if(registered) {
            SessionUtils.sendChat(session, Lang.PLAYER_NOTIFY_REGISTERED.build());
            session.setFlag(FLAG_LOGIN_KEY, true);
        } else {
            SessionUtils.sendChat(session, Lang.PLAYER_NOTIFY_NONREGISTERED.build());
            session.setFlag(FLAG_LOGIN_KEY, false);
        }
    }

    @Override
    public void sessionRemoved(SessionRemovedEvent event) {
        Session session = event.getSession();
        GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
        // clear up stuffs
        checkFlagAndCancelTask(session, FLAG_CHECK_TASK_KEY);
        checkFlagAndCancelTask(session, FLAG_ACCOUNT_TASK_KEY);
        server.getLogger().info(Lang.SERVER_PLAYER_DISCONNECT.build(profile.getName()));
    }

    public void cleanCache() {
        Iterator<PlayerStatusInfo> i = playerCache.iterator();
        while(i.hasNext()) {
            PlayerStatusInfo s = i.next();
            if(s.timeDiff() > CACHE_INVALIDATE_TIME) {
                i.remove();
            }
        }
    }

    private void checkFlagAndCancelTask(Session session, String flag) {
        if(session.hasFlag(flag)) {
            Future f = session.getFlag(flag);
            if(f != null && f.isDone() && !f.isCancelled()) {
                f.cancel(true);
            }
        }
    }

    public ExecutorService getThreads() {
        return threads;
    }
}
