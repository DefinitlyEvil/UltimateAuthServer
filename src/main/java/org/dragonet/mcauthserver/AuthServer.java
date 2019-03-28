package org.dragonet.mcauthserver;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.data.game.world.WorldType;
import com.github.steveice10.mc.protocol.data.message.TextMessage;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import org.dragonet.mcauthserver.utils.Lang;
import org.dragonet.mcauthserver.utils.SessionUtils;
import org.dragonet.mcauthserver.utils.SimpleLogger;
import org.dragonet.mcauthserver.utils.URUtils;

import java.io.*;
import java.util.Properties;

/**
 * Created on 2017/9/25.
 */
public class AuthServer {

    public final static AuthServer instance = new AuthServer();

    public static void main(String[] args) {
        instance.run();
    }

    public final static String VERSION = "1.2.1";
    public final static String MINECRAFT_VERSION = "1.12.2";

    private SimpleLogger logger = new SimpleLogger();

    private Properties properties = new Properties();
    private Server server;
    private AuthProcessor processor;

    private void run() {
        // create logging system
        logger.addOutput(System.out);
        try {
            logger.addOutput(new FileOutputStream("console.log"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        File file = new File("config.properties");
        File langFile = new File("lang.properties");
        if(!langFile.exists()) {
            saveResource("/lang.properties", langFile);
        }
        if(!file.exists()) {
            logger.info("AuthServer (first run! )");
            logger.info("====");
            saveResource("/config.properties", file);
            logger.info("Configurations generated! ");
            logger.info("Please edit configurations and restart the software! ");
            System.exit(0);
        }
        // load the configurations
        try {
            Lang.data.load(new FileInputStream("lang.properties"));
            properties.load(new FileInputStream("config.properties"));
        } catch(Exception e){
            e.printStackTrace();
        }
        logger.info(Lang.SERVER_STARTUP.build(VERSION));
        logger.info(Lang.SERVER_MINECRAFT_VERSION.build(MINECRAFT_VERSION));
        logger.info("====");
        // load stuffs from the configuration
        URUtils.API_URL = properties.getProperty("api-url");
        URUtils.API_KEY = properties.getProperty("api-key");

        logger.info(Lang.SERVER_CREATE_THREADPOOL.build());
        processor = new AuthProcessor(this, Integer.parseInt(properties.getProperty("threads")));
        String host = properties.getProperty("host");
        int port = Integer.parseInt(properties.getProperty("port"));
        logger.info(Lang.SERVER_STARTING.build(host, port));
        server = new Server(host, port, MinecraftProtocol.class, new TcpSessionFactory());
        server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, false);
        server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, (ServerInfoBuilder) session -> new ServerStatusInfo(new VersionInfo(MinecraftConstants.GAME_VERSION, MinecraftConstants.PROTOCOL_VERSION), new PlayerInfo(0, 0, new GameProfile[0]), new TextMessage("AuthServer by DefinitlyEvil"), null));
        server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, (ServerLoginHandler) session -> {
            session.send(new ServerJoinGamePacket(0, false, GameMode.SPECTATOR, 0, Difficulty.PEACEFUL, 10, WorldType.DEFAULT, false));
            session.send(new ServerPlayerPositionRotationPacket(0d, 128d, 0d, 0f, 0f, 0, null));
            SessionUtils.sendChat(session, Lang.PLAYER_WELCOME.build());
            processor.onPlayerLoggedIn(session);
        });
        server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 256);
        server.addListener(processor);
        server.bind();
    }

    public Properties getProperties() {
        return properties;
    }

    public SimpleLogger getLogger() {
        return logger;
    }

    public AuthProcessor getProcessor() {
        return processor;
    }

    private static void saveResource(String resourcePath, File saveTo) {
        try {
            InputStream i = AuthServer.class.getResourceAsStream(resourcePath);
            FileOutputStream fos = new FileOutputStream(saveTo);
            byte[] buffer = new byte[2048];
            int read;
            while((read = i.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            i.close();
            fos.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
