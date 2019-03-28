package org.dragonet.mcauthserver.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.dragonet.mcauthserver.AuthServer;

/**
 * Created on 2017/9/26.
 */
public class URUtils {

    public static String API_URL;
    public static String API_KEY;

    /**
     * Request to the UltimateRoles server API
     * @param endpoint
     * @param args
     * @return
     */
    public static JsonObject request(String endpoint, String args) {
        String final_url = API_URL + endpoint + "?key=" + API_KEY + "&" + args;
        AuthServer.instance.getLogger().info("DEBUG: " + final_url);
        String data = HttpRequest.sendGet(final_url);
        if(data == null) return null;
        return new JsonParser().parse(data).getAsJsonObject();
    }

    /**
     * Check the result
     * @param data
     * @return NULL if success
     */
    public static String checkError(JsonObject data) {
        if(data == null) return Lang.SERVER_API_FAILED.build();
        if(data.get("status").getAsString().equalsIgnoreCase("success")) return null;
        return data.get("message").getAsString();
    }

}
