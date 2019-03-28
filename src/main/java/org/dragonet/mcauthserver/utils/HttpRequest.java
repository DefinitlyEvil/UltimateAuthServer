package org.dragonet.mcauthserver.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;


public class HttpRequest {
    public static String sendGet(String url) {
        try {
            URL realUrl = new URL(url);
            URLConnection connection = realUrl.openConnection();
            connection.setRequestProperty("User-Agent", "UltimateRoles-AuthServer");
            connection.connect();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            InputStream is = connection.getInputStream();
            byte[] buff = new byte[256];
            int read;
            while ((read = is.read(buff)) != -1) {
                bos.write(buff, 0, read);
            }
            is.close();
            bos.close();
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}