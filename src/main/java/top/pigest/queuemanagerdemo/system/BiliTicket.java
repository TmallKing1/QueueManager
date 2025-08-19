package top.pigest.queuemanagerdemo.system;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.util.Pair;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import top.pigest.queuemanagerdemo.Settings;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class BiliTicket {
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public static String hmacSha256(String key, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    public static Pair<String, String> getBiliTicket() throws Exception {
        long ts = System.currentTimeMillis() / 1000;
        String csrf = Settings.hasCookie("bili_jct") ? Settings.getCookie("bili_jct") : "";
        String hexSign = hmacSha256("XgwSnGZ1p", "ts" + ts);
        URI uri = new URIBuilder("https://api.bilibili.com/bapis/bilibili.api.ticket.v1.Ticket/GenWebTicket")
                .addParameter("key_id", "ec02")
                .addParameter("hexsign", hexSign)
                .addParameter("context[ts]", String.valueOf(ts))
                .addParameter("csrf", csrf).build();
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setHeader("User-Agent", Settings.USER_AGENT);
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpResponse response = httpclient.execute(httpPost);
            JsonObject object = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
            if (object.get("code").getAsInt() == 0) {
                if (!Settings.hasCookie("bili_ticket")) {
                    String ticket = object.getAsJsonObject("data").get("ticket").getAsString();
                    long date = System.currentTimeMillis() + object.getAsJsonObject("data").get("ttl").getAsLong();
                    BasicClientCookie biliTicket = new BasicClientCookie("bili_ticket", ticket);
                    biliTicket.setDomain("bilibili.com");
                    biliTicket.setExpiryDate(new Date(date));
                    biliTicket.setPath("/");
                    Settings.getBiliCookieStore().addCookie(biliTicket);
                    BasicClientCookie biliTicketExpires = new BasicClientCookie("bili_ticket_expires", ticket);
                    biliTicketExpires.setDomain("bilibili.com");
                    biliTicketExpires.setExpiryDate(new Date(date));
                    biliTicket.setPath("/");
                    Settings.getBiliCookieStore().addCookie(biliTicketExpires);
                    Settings.saveCookie(false);
                }
                JsonObject nav = object.getAsJsonObject("data").getAsJsonObject("nav");
                String imgKey = nav.get("img").getAsString();
                imgKey = imgKey.substring(imgKey.indexOf("wbi/") + 4, imgKey.lastIndexOf("."));
                String subKey = nav.get("sub").getAsString();
                subKey = subKey.substring(subKey.indexOf("wbi/") + 4, subKey.lastIndexOf("."));
                return new Pair<>(imgKey, subKey);

            } else {
                throw new Exception(object.get("message").getAsString());
            }
        }
    }
}