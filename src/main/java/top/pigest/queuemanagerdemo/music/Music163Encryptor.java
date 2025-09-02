package top.pigest.queuemanagerdemo.music;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import top.pigest.queuemanagerdemo.Settings;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

public class Music163Encryptor {
    private static final String E = "010001";
    private static final String MODULES = "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7";
    private static final String KEY = "0CoJUm6Qyw8W8jud";
    private static final String VI = "0102030405060708";

    public static JsonObject postRequest(String url, JsonObject cs) throws Exception {
        String csrfToken = Settings.hasCookie("__csrf") ? Settings.getCookie("__csrf"): "";
        String url1 = url + "?csrf_token=" + csrfToken;

        cs.addProperty("csrf_token", csrfToken);
        String cipherTextFirst = encryptBytes(cs.toString(), KEY);
        String random16 = generateRandomString();
        String params = encryptBytes(cipherTextFirst, random16);
        String encSecKey = encryptedString(random16);

        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("params", params));
        nameValuePairs.add(new BasicNameValuePair("encSecKey", encSecKey));
        HttpPost post = getHttpPost(url1, nameValuePairs);

        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build();
             CloseableHttpResponse response = httpClient.execute(post)) {
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            return JsonParser.parseString(result).getAsJsonObject();
        }
    }

    private static HttpPost getHttpPost(String url1, List<NameValuePair> nameValuePairs) throws UnsupportedEncodingException {
        HttpPost post = new HttpPost(url1);
        post.setConfig(Settings.DEFAULT_REQUEST_CONFIG);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setHeader("User-Agent", Settings.USER_AGENT);
        post.setHeader("Accept", "*/*");
        post.setHeader("Host", "music.163.com");
        post.setHeader("Accept-Encoding", "gzip, deflate");
        post.setHeader("Referer", "https://music.163.com/search/");

        post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        return post;
    }

    private static String encryptBytes(String plainText, String key) throws Exception {
        byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
        int padLength = 16 - (plainBytes.length % 16);
        byte[] paddedBytes = new byte[plainBytes.length + padLength];
        System.arraycopy(plainBytes, 0, paddedBytes, 0, plainBytes.length);
        for (int i = plainBytes.length; i < paddedBytes.length; i++) {
            paddedBytes[i] = (byte) padLength;
        }

        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(Music163Encryptor.VI.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        byte[] encryptedBytes = cipher.doFinal(paddedBytes);

        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private static String encryptedString(String text) {
        String reversed = new StringBuilder(text).reverse().toString();
        BigInteger bigInt = new BigInteger(hexlify(reversed.getBytes(StandardCharsets.UTF_8)), 16);
        BigInteger exponent = new BigInteger(E, 16);
        BigInteger modulus = new BigInteger(MODULES, 16);
        BigInteger result = bigInt.modPow(exponent, modulus);
        return String.format("%256s", result.toString(16)).replace(' ', '0');
    }

    private static String hexlify(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String generateRandomString() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
