package top.pigest.queuemanagerdemo.liveroom;

import com.google.gson.JsonArray;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LiveRoomApi {
    /**
     * 获取当前登录账号的所有房管信息
     * @return 一个 {@link Map} 对象，键为房管 UID，值为房管用户名
     */
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

    /**
     * 查询用户在当前登录主播账号的粉丝牌信息（{@code medal_info} 模板）
     * @param uid 需要查询的用户 UID
     * @return 一个 {@link FansMedal} 对象，表示粉丝牌信息，若用户没有已登录主播账号的粉丝牌则为 {@code null}
     */
    public static FansMedal getFansMedalInfo(long uid) throws Exception {
        return getFansMedalInfo(uid, Settings.MID);
    }

    /**
     * 查询用户在特定 UID 主播的粉丝牌信息（{@code medal_info} 模板）
     * @param user 需要查询的用户 UID
     * @param anchor 主播 UID
     * @return 一个 {@link FansMedal} 对象，表示粉丝牌信息，若用户没有该主播的粉丝牌则为 {@code null}
     */
    public static FansMedal getFansMedalInfo(long user, long anchor) throws Exception {
        try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build()) {
            URI uri = new URIBuilder("https://api.live.bilibili.com/xlive/app-ucenter/v1/fansMedal/user_medal_info")
                    .addParameter("uid", String.valueOf(user))
                    .addParameter("up_uid", String.valueOf(anchor))
                    .build();
            HttpGet httpGet = new HttpGet(uri);
            CloseableHttpResponse response = client.execute(httpGet);
            JsonObject obj = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
            if (obj.get("code").getAsInt() == 0) {
                JsonObject element = obj.getAsJsonObject("data");
                if (!element.get("lookup_medal").isJsonNull()) {
                    JsonObject lookupMedal = element.getAsJsonObject("lookup_medal");
                    return FansMedal.deserializeMedalInfo(lookupMedal);
                }
            }
        }
        return null;
    }

    /**
     * 查询用户在当前登录主播账号的粉丝牌信息（{@code uinfo_medal} 模板）
     * @param uid 需要查询的用户 UID
     * @return 一个 {@link FansMedal} 对象，表示粉丝牌信息，若用户没有已登录主播账号的粉丝牌则为 {@code null}
     */
    public static FansMedal getFansUInfoMedal(long uid) throws Exception {
        return getFansUInfoMedal(uid, Settings.MID);
    }

    /**
     * 查询用户在特定 UID 主播的粉丝牌信息（{@code uinfo_medal} 模板）
     * @param user 需要查询的用户 UID
     * @param anchor 主播 UID
     * @return 一个 {@link FansMedal} 对象，表示粉丝牌信息，若用户没有该主播的粉丝牌则为 {@code null}
     */
    public static FansMedal getFansUInfoMedal(long user, long anchor) throws Exception {
        try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build()) {
            URI uri = new URIBuilder("https://api.live.bilibili.com/xlive/app-ucenter/v1/fansMedal/user_medal_info")
                    .addParameter("uid", String.valueOf(user))
                    .addParameter("up_uid", String.valueOf(anchor))
                    .build();
            HttpGet httpGet = new HttpGet(uri);
            CloseableHttpResponse response = client.execute(httpGet);
            JsonObject obj = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
            if (obj.get("code").getAsInt() == 0) {
                JsonObject element = obj.getAsJsonObject("data");
                if (!element.get("lookup_v2").isJsonNull()) {
                    JsonObject lookupMedal = element.getAsJsonObject("lookup_v2");
                    return FansMedal.deserializeUInfoMedal(lookupMedal);
                }
            }
        }
        return null;
    }

    /**
     * 查询当前登录主播账号的舰长列表，返回为 {@link User} 对象列表，包含舰长的粉丝牌信息
     * @param page 页码，每页 20 个。特殊地，为 1 时返回前 23 个舰长（若存在），为 2 时返回第 24 - 43 个舰长（若存在）……
     * @return {@link User} 列表，表示获取到的舰长用户信息
     */
    public static List<User> getGuards(int page) throws Exception {
        try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build()) {
            List<User> users = new ArrayList<>();
                URI uri = new URIBuilder("https://api.live.bilibili.com/xlive/app-room/v2/guardTab/topListNew")
                        .addParameter("roomid", String.valueOf(LiveMessageService.getInstance().getRoomId()))
                        .addParameter("page", String.valueOf(page))
                        .addParameter("page_size", "20")
                        .addParameter("ruid", String.valueOf(Settings.MID))
                        .addParameter("typ", "5")
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
                            users.add(new User(uname, uid)
                                    .setFansMedal(obj2.getAsJsonObject("medal"))
                                    .setFace(obj2.getAsJsonObject("base").get("face").toString()));
                        });
                    }
                    element.getAsJsonArray("list").forEach(elem -> {
                        JsonObject obj2 = elem.getAsJsonObject().getAsJsonObject("uinfo");
                        long uid = obj2.get("uid").getAsLong();
                        String uname = obj2.getAsJsonObject("base").get("name").getAsString();
                        users.add(new User(uname, uid)
                                .setFansMedal(obj2.getAsJsonObject("medal"))
                                .setFace(obj2.getAsJsonObject("base").get("face").toString()));
                    });
                }
            return users;
        }
    }

    /**
     * 查询当前登录主播账号的舰长列表，包含舰长的到期时间信息
     * @param page 查询的页码，若为 {@code 0} 则返回所有舰长
     * @return {@link GuardInfo} 列表，表示获取到的舰长信息
     */
    public static List<GuardInfo> getGuardsWithExpireDate(int page) throws Exception {
        try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build()) {
            List<GuardInfo> guards = new ArrayList<>();
            if (page == 0) {
                for (int page1 = 1; ; page1++) {
                    URI uri = new URIBuilder("https://api.live.bilibili.com/xlive/web-ucenter/user/sailors")
                            .addParameter("page", String.valueOf(page1))
                            .addParameter("page_size", "20").build();
                    HttpGet httpGet = new HttpGet(uri);
                    CloseableHttpResponse response = client.execute(httpGet);
                    JsonObject obj = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
                    if (obj.get("code").getAsInt() == 0) {
                        JsonObject element = obj.getAsJsonObject("data");
                        JsonArray array = element.getAsJsonArray("list");
                        array.forEach(elem -> {
                            JsonObject object = elem.getAsJsonObject();
                            long uid = object.get("uid").getAsLong();
                            int guardLevel = object.get("guard_level").getAsInt();
                            String uname = object.get("username").getAsString();
                            String expiredTime = object.get("expired_time").getAsString();
                            LocalDate localDate = LocalDate.parse(expiredTime, DateTimeFormatter.ISO_LOCAL_DATE);
                            guards.add(new GuardInfo(uid, GuardType.valueOf(guardLevel), uname, localDate));
                        });
                        int totalPages = element.getAsJsonObject("pageInfo").get("totalPages").getAsInt();
                        if (page1 >= totalPages) {
                            return guards;
                        }
                    }
                }
            } else {
                URI uri = new URIBuilder("https://api.live.bilibili.com/xlive/web-ucenter/user/sailors")
                        .addParameter("page", String.valueOf(page)).build();
                HttpGet httpGet = new HttpGet(uri);
                CloseableHttpResponse response = client.execute(httpGet);
                JsonObject obj = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
                if (obj.get("code").getAsInt() == 0) {
                    JsonObject element = obj.getAsJsonObject("data");
                    JsonArray array = element.getAsJsonArray("list");
                    array.forEach(elem -> {
                        JsonObject object = elem.getAsJsonObject();
                        long uid = object.get("uid").getAsLong();
                        int guardLevel = object.get("guard_level").getAsInt();
                        String uname = object.get("username").getAsString();
                        String expiredTime = object.get("expired_time").getAsString();
                        LocalDate localDate = LocalDate.parse(expiredTime, DateTimeFormatter.ISO_LOCAL_DATE);
                        guards.add(new GuardInfo(uid, GuardType.valueOf(guardLevel), uname, localDate));
                    });
                    return guards;
                }
            }
        }
        return List.of();
    }

    public static List<User> getUserBriefInfo(Collection<Long> uids) throws Exception {
        try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build()) {
            List<User> users = new ArrayList<>();
            URI uri = new URIBuilder("https://api.vc.bilibili.com/x/im/user_infos")
                    .addParameter("uids", String.join(",", uids.stream().map(String::valueOf).toList()))
                    .build();
            HttpGet httpGet = new HttpGet(uri);
            CloseableHttpResponse response = client.execute(httpGet);
            JsonObject obj = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
            if (obj.get("code").getAsInt() == 0) {
                JsonArray element = obj.getAsJsonArray("data");
                element.forEach(elem -> {
                    JsonObject object = elem.getAsJsonObject();
                    long uid = object.get("mid").getAsLong();
                    String uname = object.get("name").getAsString();
                    String face = object.get("face").getAsString();
                    users.add(new User(uname, uid).setFace(face));
                });
            }
            return users;
        }
    }
}
