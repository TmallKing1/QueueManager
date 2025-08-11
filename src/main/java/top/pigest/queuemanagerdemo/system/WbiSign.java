package top.pigest.queuemanagerdemo.system;

import javafx.util.Pair;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class WbiSign {
    private static final int[] mixinKeyEncTab = new int[]{
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
            33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
            61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
            36, 20, 34, 44, 52
    };

    private static final char[] hexDigits = "0123456789abcdef".toCharArray();

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            char[] result = new char[messageDigest.length * 2];
            for (int i = 0; i < messageDigest.length; i++) {
                result[i * 2] = hexDigits[(messageDigest[i] >> 4) & 0xF];
                result[i * 2 + 1] = hexDigits[messageDigest[i] & 0xF];
            }
            return new String(result);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String getMixinKey(String imgKey, String subKey) {
        String s = imgKey + subKey;
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 32; i++)
            key.append(s.charAt(mixinKeyEncTab[i]));
        return key.toString();
    }

    private static String encodeURIComponent(Object o) {
        return URLEncoder.encode(o.toString(), StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static URI getSignedUri(URIBuilder originalURI, TreeMap<String, Object> originalParameters) throws URISyntaxException {
        Pair<String, Long> pair = getWbiSign(originalParameters);
        if (pair == null) {
            return null;
        }
        originalParameters.forEach((k, v) -> originalURI.addParameter(k, encodeURIComponent(v)));
        originalURI.addParameter("w_rid", pair.getKey());
        return originalURI.build();
    }

    public static Pair<String, Long> getWbiSign(TreeMap<String, Object> original) {
        try {
            Pair<String, String > pair = BiliTicket.getBiliTicket();
            long timestamp = System.currentTimeMillis() / 1000;
            return new Pair<>(WbiSign.getWbiSign(pair.getKey(), pair.getValue(), timestamp, original), timestamp);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getWbiSign(String imgKey, String subKey, long timestamp, TreeMap<String, Object> map) {
        String mixinKey = getMixinKey(imgKey, subKey);

        map.put("wts", timestamp);
        String param = map.entrySet().stream()
                .map(it -> String.format("%s=%s", it.getKey(), encodeURIComponent(it.getValue())))
                .collect(Collectors.joining("&"));
        String s = param + mixinKey;

        return md5(s);
    }
}