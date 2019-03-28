package org.dragonet.mcauthserver.utils;

import java.util.Properties;

/**
 * Created on 2017/9/26.
 */
public enum Lang {
    SERVER_STARTUP,
    SERVER_MINECRAFT_VERSION,
    SERVER_CREATE_THREADPOOL,
    SERVER_STARTING,

    SERVER_PLAYER_JOINED,
    SERVER_PLAYER_DISCONNECT,
    SERVER_PLAYER_IP,

    SERVER_API_FAILED,
    SERVER_ERROR,

    ACTION_LOGIN,
    ACTION_REGISTRATION,

    PLAYER_WELCOME,
    PLAYER_LOADING,
    PLAYER_STILL_LOADING,
    PLAYER_NOTIFY_REGISTERED,
    PLAYER_NOTIFY_NONREGISTERED,
    PLAYER_REGISTER_REPEAT,
    PLAYER_PASSWORD_MISMATCH,
    PLAYER_REGISTERING,
    PLAYER_LOGGING_IN,
    PLAYER_FAILED,
    PLAYER_SUCCESS,
    PLAYER_NOTICE_COMMAND
    ;



    public static Properties data = new Properties();

    public String build(Object... args) {
        return String.format(data.getProperty(name()), args);
    }
}
