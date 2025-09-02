package top.pigest.queuemanagerdemo.music;

import com.google.gson.JsonObject;
import top.pigest.queuemanagerdemo.Settings;

public class Music163Api {
    // 获取二维码登录 key
    public static JsonObject qrLoginUniKey() throws Exception {
        String url = "https://music.163.com/weapi/login/qrcode/unikey";
        JsonObject o = new JsonObject();
        o.addProperty("noCheckToken", true);
        o.addProperty("type", 1);
        return Music163Encryptor.postRequest(url, o);
    }

    // 检测二维码登录状态
    public static JsonObject qrLoginCheck(String key) throws Exception {
        String url = "https://music.163.com/weapi/login/qrcode/client/login";
        JsonObject o = new JsonObject();
        o.addProperty("noCheckToken", true);
        o.addProperty("type", 1);
        o.addProperty("key", key);
        return Music163Encryptor.postRequest(url, o);
    }

    // 登出
    public static JsonObject logout() throws Exception {
        String url = "https://music.163.com/weapi/logout";
        Settings.removeCookie("MUSIC_U", null, null);
        return Music163Encryptor.postRequest(url, new JsonObject());
    }

    // 搜索歌曲
    public static JsonObject searchMusic(String s) throws Exception {
        String url = "https://music.163.com/weapi/cloudsearch/get/web";
        JsonObject o = new JsonObject();
        o.addProperty("hlposttag", "</span>");
        o.addProperty("hlpretag", "<span class=\"s-fc7\">");
        o.addProperty("limit", 30);
        o.addProperty("offset", 0);
        o.addProperty("s", s);
        o.addProperty("type", 1);
        return Music163Encryptor.postRequest(url, o);
    }

    // 获取当前 Cookie 主体的用户信息
    public static JsonObject userInfo() throws Exception {
        String url = "https://music.163.com/weapi/w/nuser/account/get";
        JsonObject o = new JsonObject();
        return Music163Encryptor.postRequest(url, o);
    }

    // 获取用户 VIP 信息
    public static JsonObject vipInfo(String uid) throws Exception {
        String url = "https://music.163.com/weapi/music-vip-membership/front/vip/info";
        JsonObject o = new JsonObject();
        o.addProperty("userId", uid);
        return Music163Encryptor.postRequest(url, o);
    }

    // 获取用户歌单列表
    public static JsonObject playLists(String uid) throws Exception {
        String url = "https://music.163.com/weapi/user/playlist";
        JsonObject o = new JsonObject();
        o.addProperty("limit", 36);
        o.addProperty("offset", 0);
        o.addProperty("uid", uid);
        o.addProperty("wordwrap", 7);
        return Music163Encryptor.postRequest(url, o);
    }

    // 获取单个歌单信息
    public static JsonObject playlist(String id) throws Exception {
        String url = "https://music.163.com/weapi/v6/playlist/detail";
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("limit", 10000);
        o.addProperty("offset", 0);
        return Music163Encryptor.postRequest(url, o);
    }

    // 获取歌词
    public static JsonObject lyric(String id) throws Exception {
        String url = "https://music.163.com/weapi/song/lyric";
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("lv", -1);
        o.addProperty("tv", -1);
        return Music163Encryptor.postRequest(url, o);
    }

    public static JsonObject songDetail(String id) throws Exception {
        String url = "https://music.163.com/weapi/v3/song/detail";
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("c", "[{\"id\":\"%s\"}]".formatted(id));
        return Music163Encryptor.postRequest(url, o);
    }

    /**
     * 获取歌曲下载链接
     * @param id 歌曲id
     * @param encodeType 编码类型 aac/flac
     * @param level 音质 standard/higher/exhigh/lossless/hires
     * @return 响应，链接在根对象.data[0].url
     * @throws Exception 请求时抛出的异常
     */
    public static JsonObject player(String id, String encodeType, String level) throws Exception {
        String url = "https://music.163.com/weapi/song/enhance/player/url/v1";
        JsonObject o = new JsonObject();
        o.addProperty("ids", "[%s]".formatted(id));
        o.addProperty("encodeType", encodeType);
        o.addProperty("level", level);
        return Music163Encryptor.postRequest(url, o);
    }
}
