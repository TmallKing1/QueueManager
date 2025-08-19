package top.pigest.queuemanagerdemo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import javafx.scene.text.Font;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import top.pigest.queuemanagerdemo.system.settings.DanmakuServiceSettings;
import top.pigest.queuemanagerdemo.system.settings.SaveSettings;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class Settings {
    public static final File DATA_DIRECTORY = new File(System.getProperty("user.dir")+"\\.PPDD");
    public static final Font DEFAULT_FONT;
    public static final Font SPEC_FONT;
    public static final RequestConfig DEFAULT_REQUEST_CONFIG = RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(5000).setCookieSpec(CookieSpecs.STANDARD).build();
    private static final File COOKIE_STORE_FILE = DATA_DIRECTORY.toPath().resolve("cookies.ser").toFile();
    private static final File SAVE_SETTINGS_FILE = DATA_DIRECTORY.toPath().resolve("settings.json").toFile();
    private static final String REFRESH_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n" +
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDLgd2OAkcGVtoE3ThUREbio0Eg\n" +
            "Uc/prcajMKXvkCKFCWhJYJcLkcM2DKKcSeFpD/j6Boy538YXnR6VhcuUJOhH2x71\n" +
            "nzPjfdTcqMz7djHum0qSZA0AyCBDABUqCrfNgCiJ00Ra7GmRj+YCK1NJEuewlb40\n" +
            "JNrRuoEUXpabUzGB8QIDAQAB\n" +
            "-----END PUBLIC KEY-----";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:141.0) Gecko/20100101 Firefox/141.0";
    private static CookieStore BILI_COOKIE_STORE = new BasicCookieStore();
    private static SaveSettings SAVE_SETTINGS;

    public static long MID = -1;

    static {
        if (!DATA_DIRECTORY.exists()) {
            DATA_DIRECTORY.mkdir();
        }
        try {
            DEFAULT_FONT = loadFont("font.otf", 20);
            SPEC_FONT = loadFont("font_spec.ttf", 20);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getRefreshToken() {
        return SAVE_SETTINGS.getRefreshToken();
    }

    public static CookieStore getBiliCookieStore() {
        LocalDate date = LocalDate.now();
        LocalDate lastRefreshDate = Instant.ofEpochMilli(SAVE_SETTINGS.getLastRefreshTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        if (!date.equals(lastRefreshDate)) {
            Optional<Cookie> biliJct = BILI_COOKIE_STORE.getCookies().stream().filter(cookie -> cookie.getName().equals("bili_jct")).findAny();
            if (biliJct.isPresent()) {
                refreshCookie(biliJct.get().getValue());
            } else {
                SAVE_SETTINGS.setLastRefreshTime(System.currentTimeMillis());
            }
        }
        return BILI_COOKIE_STORE;
    }

    private static void refreshCookie(String biliJct) {
        try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(BILI_COOKIE_STORE).build()) {
            URI uri = new URIBuilder("https://passport.bilibili.com/x/passport-login/web/cookie/info")
                    .addParameter("csrf", biliJct).build();
            HttpGet httpGet = new HttpGet(uri);
            httpGet.setConfig(DEFAULT_REQUEST_CONFIG);
            CloseableHttpResponse response = client.execute(httpGet);
            JsonObject object = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
            if (object.get("code").getAsInt() == -101) {
                BILI_COOKIE_STORE.clear();
                SAVE_SETTINGS.setLastRefreshTime(System.currentTimeMillis());
            } else if (object.get("code").getAsInt() == 0) {
                if (object.getAsJsonObject("data").get("refresh").getAsBoolean()) {
                    long timestamp = object.getAsJsonObject("data").get("timestamp").getAsLong();
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    String publicKeyStr = REFRESH_PUBLIC_KEY
                            .replace("-----BEGIN PUBLIC KEY-----", "")
                            .replace("-----END PUBLIC KEY-----", "")
                            .replace("\n", "")
                            .trim();
                    byte[] publicBytes = Base64.getDecoder().decode(publicKeyStr);
                    X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicBytes);
                    PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);

                    String algorithm = "RSA/ECB/OAEPPadding";
                    Cipher cipher = Cipher.getInstance(algorithm);
                    cipher.init(Cipher.ENCRYPT_MODE, publicKey);

                    byte[] plaintextBytes = String.format("refresh_%d", timestamp).getBytes(StandardCharsets.UTF_8);
                    OAEPParameterSpec oaepParams = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
                    cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);
                    byte[] encryptedBytes = cipher.doFinal(plaintextBytes);
                    String correspondPath = new BigInteger(1, encryptedBytes).toString(16);
                    String url = "https://www.bilibili.com/correspond/1/" + correspondPath;
                    HttpGet httpget = new HttpGet(url);
                    httpget.setConfig(DEFAULT_REQUEST_CONFIG);
                    CloseableHttpResponse response1 = client.execute(httpget);
                    String s = EntityUtils.toString(response1.getEntity());
                    String refreshCsrf = s.substring(s.indexOf("<div id=\"1-name\">") + "<div id=\"1-name\">".length());
                    refreshCsrf = refreshCsrf.substring(0, refreshCsrf.indexOf("</div>"));
                    HttpPost httppost = new HttpPost("https://passport.bilibili.com/x/passport-login/web/cookie/refresh");
                    httppost.setConfig(DEFAULT_REQUEST_CONFIG);
                    httppost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                    List<NameValuePair> lp = new ArrayList<>();
                    lp.add(new BasicNameValuePair("csrf", biliJct));
                    lp.add(new BasicNameValuePair("refresh_csrf", refreshCsrf));
                    lp.add(new BasicNameValuePair("source", "main_web"));
                    lp.add(new BasicNameValuePair("refresh_token", SAVE_SETTINGS.getRefreshToken()));
                    httppost.setEntity(new UrlEncodedFormEntity(lp));
                    CloseableHttpResponse response2 = client.execute(httppost);
                    JsonObject object1 = JsonParser.parseString(EntityUtils.toString(response2.getEntity())).getAsJsonObject();
                    if (object1.get("code").getAsInt() == 0) {
                        String newRefreshToken =  object1.getAsJsonObject("data").get("refresh_token").getAsString();
                        HttpPost httpPost = new HttpPost("https://passport.bilibili.com/x/passport-login/web/confirm/refresh");
                        httpPost.setConfig(DEFAULT_REQUEST_CONFIG);
                        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                        Optional<Cookie> biliJct1 = BILI_COOKIE_STORE.getCookies().stream().filter(cookie -> cookie.getName().equals("bili_jct")).findAny();
                        assert biliJct1.isPresent();
                        List<NameValuePair> lp2 = new ArrayList<>();
                        lp2.add(new BasicNameValuePair("refresh_token", SAVE_SETTINGS.getRefreshToken()));
                        lp2.add(new BasicNameValuePair("csrf", biliJct1.get().getValue()));
                        httpPost.setEntity(new UrlEncodedFormEntity(lp2));
                        CloseableHttpResponse response3 = client.execute(httpPost);
                        JsonObject object2 = JsonParser.parseString(EntityUtils.toString(response3.getEntity())).getAsJsonObject();
                        if (object2.get("code").getAsInt() == 0) {
                            SAVE_SETTINGS.setLastRefreshTime(System.currentTimeMillis());
                        } else {
                            QueueManager.INSTANCE.getMainScene().showDialogMessage("刷新 Cookie 失败，建议重新登录\n%s(%s)".formatted(object2.get("message").getAsString(), object2.get("code").getAsInt()), true);
                        }
                        SAVE_SETTINGS.setRefreshToken(newRefreshToken);
                    } else {
                        QueueManager.INSTANCE.getMainScene().showDialogMessage("刷新 Cookie 失败，建议重新登录\n%s(%s)".formatted(object1.get("message").getAsString(), object1.get("code").getAsInt()), true);
                    }
                }
            }
            SAVE_SETTINGS.setLastRefreshTime(System.currentTimeMillis());
            saveSettings();
        } catch (Exception e) {
            System.err.println("检测Cookie刷新状态失败，请注意Cookie可用性");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static DanmakuServiceSettings getDanmakuServiceSettings() {
        return SAVE_SETTINGS.getDanmakuServiceSettings();
    }

    public static void resetDanmakuServiceSettings() {
        SAVE_SETTINGS.resetDanmakuServiceSettings();
    }

    public static void setRefreshToken(String refreshToken) {
        SAVE_SETTINGS.setRefreshToken(refreshToken);
        saveSettings();
    }

    public static Font loadFont(String fontFileName, double size) throws IOException {
        return Font.loadFont(Settings.class.getResourceAsStream(fontFileName), size);
    }

    public static String loadCaptcha() {
        return Objects.requireNonNull(Settings.class.getResource("captcha.html")).toExternalForm();
    }

    public static void loadSettings() {
        Gson gson = new Gson();
        try {
            SAVE_SETTINGS = gson.fromJson(new JsonReader(new FileReader(SAVE_SETTINGS_FILE)), SaveSettings.class);
        } catch (FileNotFoundException e) {
            SAVE_SETTINGS = new SaveSettings();
        }
    }

    public static void loadCookie() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(COOKIE_STORE_FILE))) {
            BILI_COOKIE_STORE = (CookieStore) ois.readObject();
        } catch (Exception e) {
            BILI_COOKIE_STORE = new BasicCookieStore();
        }
    }

    public static void saveCookie(boolean updateRefreshTime) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(COOKIE_STORE_FILE))) {
            out.writeObject(BILI_COOKIE_STORE);
            if (updateRefreshTime) {
                SAVE_SETTINGS.setLastRefreshTime(System.currentTimeMillis());
            }
        } catch (Exception e) {
            System.err.println("保存 Cookie 失败");
            System.err.println(e.getMessage());
        }
    }

    public static void saveSettings() {
        Gson gson = new Gson();
        String s = gson.toJson(SAVE_SETTINGS, SaveSettings.class);
        try (FileWriter fw = new FileWriter(SAVE_SETTINGS_FILE)) {
            fw.write(s);
        } catch (IOException e) {
            System.err.println("保存设置文件失败");
            System.err.println(e.getMessage());
        }
    }

    public static String getCookie(String name) {
        return getBiliCookieStore().getCookies().stream().filter(cookie -> cookie.getName().equalsIgnoreCase(name)).findFirst().orElseThrow().getValue();
    }

    public static boolean hasCookie(String name) {
        return getBiliCookieStore().getCookies().stream().anyMatch(cookie -> cookie.getName().equalsIgnoreCase(name));
    }

    public static void setMID(long MID) {
        Settings.MID = MID;
    }
}
