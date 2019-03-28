package org.dragonet.mcauthserver.utils;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.packetlib.Session;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Created on 2017/9/26.
 */
public class SessionUtils {

    public static void sendChat(Session session, String text) {
        String[] lines = text.split("\n");
        for(String l : lines) {
            session.send(new ServerChatPacket(l));
        }
    }

    public static UUID getUUID(GameProfile profile) {
        return UUID.nameUUIDFromBytes(profile.getName().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

}
