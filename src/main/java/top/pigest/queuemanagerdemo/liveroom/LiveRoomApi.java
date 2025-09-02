package top.pigest.queuemanagerdemo.liveroom;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import top.pigest.queuemanagerdemo.Settings;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class LiveRoomApi {
    public static Map<Long, String> getRoomAdmins() throws Exception {
        try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build()) {
            Map<Long, String> map = new HashMap<>();
            for (int page = 1; ; page++) {
                URI uri = new URIBuilder("https://api.live.bilibili.com/xlive/app-ucenter/v1/roomAdmin/get_by_anchor")
                        .addParameter("page", String.valueOf(page))
                        .build();
                HttpGet httpGet = new HttpGet(uri);
                CloseableHttpResponse response = client.execute(httpGet);
                JsonObject obj = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
                if (obj.get("code").getAsInt() == 0) {
                    JsonElement element = obj.getAsJsonObject("data").get("data");
                    if (element.isJsonNull()) {
                        break;
                    } else {
                        element.getAsJsonArray().forEach(elem -> {
                            JsonObject obj2 = elem.getAsJsonObject();
                            long uid = obj2.get("uid").getAsLong();
                            String uname = obj2.get("uname").getAsString();
                            map.put(uid, uname);
                        });
                        if (page == obj.getAsJsonObject("data").getAsJsonObject("page").get("total_page").getAsInt()) {
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
            return map;
        }
    }

    public static Map<Long, String> getGuards() throws Exception {
        try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build()) {
            Map<Long, String> map = new HashMap<>();
            for (int page = 1; ; page++) {
                URI uri = new URIBuilder("https://api.live.bilibili.com/xlive/app-room/v2/guardTab/topListNew")
                        .addParameter("roomid", String.valueOf(LiveMessageService.getInstance().getRoomId()))
                        .addParameter("page", String.valueOf(page))
                        .addParameter("ruid", String.valueOf(Settings.MID))
                        .addParameter("typ", String.valueOf(5))
                        .build();
                HttpGet httpGet = new HttpGet(uri);
                CloseableHttpResponse response = client.execute(httpGet);
                JsonObject obj = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
                if (obj.get("code").getAsInt() == 0) {
                    JsonObject element = obj.getAsJsonObject("data");
                        if (page == 1) {
                            element.getAsJsonArray("top3").forEach(elem -> {
                                JsonObject obj2 = elem.getAsJsonObject().getAsJsonObject("uinfo");
                                long uid = obj2.get("uid").getAsLong();
                                String uname = obj2.getAsJsonObject("base").get("name").getAsString();
                                map.put(uid, uname);
                            });
                        }
                        element.getAsJsonArray("list").forEach(elem -> {
                            JsonObject obj2 = elem.getAsJsonObject().getAsJsonObject("uinfo");
                            long uid = obj2.get("uid").getAsLong();
                            String uname = obj2.getAsJsonObject("base").get("name").getAsString();
                            map.put(uid, uname);
                        });
                        if (page == element.getAsJsonObject("info").get("page").getAsInt() || element.getAsJsonArray("list").isEmpty()) {
                            break;
                        }
                } else {
                    break;
                }
            }
            return map;
        }
    }

    public static Map<Long, String> getFans() throws Exception {
        try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build()) {
            Map<Long, String> map = new HashMap<>();
            for (int page = 1; ; page++) {
                URI uri = new URIBuilder("https://api.live.bilibili.com/xlive/general-interface/v1/rank/getFansMembersRank")
                        .addParameter("page_size", String.valueOf(15))
                        .addParameter("rank_type", String.valueOf(0))
                        .addParameter("page", String.valueOf(page))
                        .addParameter("ruid", String.valueOf(Settings.MID))
                        .addParameter("ts", String.valueOf(System.currentTimeMillis() / 1000))
                        .build();
                HttpGet httpGet = new HttpGet(uri);
                CloseableHttpResponse response = client.execute(httpGet);
                JsonObject obj = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
                if (obj.get("code").getAsInt() == 0) {
                    JsonElement element = obj.getAsJsonObject("data").get("item");
                    if (element.isJsonNull()) {
                        break;
                    } else {
                        int count = obj.getAsJsonObject("data").get("num").getAsInt();
                        element.getAsJsonArray().forEach(elem -> {
                            JsonObject obj2 = elem.getAsJsonObject();
                            long uid = obj2.get("uid").getAsLong();
                            String uname = obj2.get("name").getAsString();
                            map.put(uid, uname);
                        });
                        if (page * 15 >= count) {
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
            return map;
        }
    }
}
