package top.pigest.queuemanagerdemo.system;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.util.Pair;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.brotli.dec.BrotliInputStream;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.widget.QMButton;
import top.pigest.queuemanagerdemo.window.main.DanmakuServiceContainer;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.zip.InflaterInputStream;

public class LiveMessageService implements WebSocket.Listener {
    private static LiveMessageService INSTANCE;

    private final long uid;
    private final long roomId;
    private final String key;
    private final String buvid;
    private final List<MessageHandler> messageHandlers = new ArrayList<>();
    private WebSocket webSocket;
    private int sequence = 1;

    public LiveMessageService(long uid, long roomId, String key, String buvid) {
        this.uid = uid;
        this.roomId = roomId;
        this.key = key;
        this.buvid = buvid;
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        System.out.println("OnPong" + new String(message.array()));
        return WebSocket.Listener.super.onPong(webSocket, message);
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
        System.out.println("OnPing" + new String(message.array()));
        return WebSocket.Listener.super.onPing(webSocket, message);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        System.out.println("OnText" + data);
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    public CompletionStage<?> onBinary(WebSocket socket, ByteBuffer buffer, boolean last) {
        System.out.println("OnBinary" + new String(buffer.array()));
        Pair<Integer, Optional<JsonObject>> pair;
        try {
            pair = receivePacket(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (pair.getKey() != -1) {
            if (pair.getKey() == 8 && pair.getValue().isPresent()) {
                int s = pair.getValue().get().get("code").getAsInt();
                if (s != 0) {
                    Platform.runLater(() -> Utils.showDialogMessage("认证失败", "RED", QueueManager.INSTANCE.getMainScene().getRootStackPane()));
                } else {
                }
            }
            if (pair.getKey() == 5 && pair.getValue().isPresent()) {
                JsonObject payload = pair.getValue().get();
                this.messageHandlers.forEach(mh -> mh.handle(payload));
            }
        }
        return WebSocket.Listener.super.onBinary(socket, buffer, last);
    }

    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        System.out.println("OnClose" + new String(reason));
        Platform.runLater(() -> {
            if (QueueManager.INSTANCE.getMainScene().getBorderPane().getCenter() instanceof DanmakuServiceContainer container && container.getInnerContainer().getId().equals("c0")) {
                container.disconnectedButton(((QMButton) ((BorderPane) container.getInnerContainer().getChildren().getFirst()).getRight()));
            }
        });
        return null;
    }

    public void onOpen(WebSocket webSocket) {
        this.webSocket = webSocket;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uid", uid);
        jsonObject.addProperty("roomid", roomId);
        jsonObject.addProperty("buvid", buvid);
        jsonObject.addProperty("protover", 3);
        jsonObject.addProperty("platform", "web");
        jsonObject.addProperty("type", 2);
        jsonObject.addProperty("support_ack", true);
        jsonObject.addProperty("scene", "room");
        jsonObject.addProperty("key", key);
        String s = jsonObject.toString();
        byte[] bytes = s.getBytes();
        int size = bytes.length + 16;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.putInt(size);
        buffer.putShort((short) 16);
        buffer.putShort((short) 1);
        buffer.putInt(7);
        buffer.putInt(sequence);
        buffer.put(bytes);
        try {
            sendPacket(webSocket, buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.addMessageHandler("narrator_single", "DANMU_MSG", NarratorService::handleSingleDanmaku);
        this.addMessageHandler("narrator_enter", "INTERACT_WORD", NarratorService::handleInteract);
        this.addMessageHandler("narrator_enter", "SEND_GIFT", NarratorService::handleGift);
        this.addMessageHandler("narrator_enter", "GUARD_BUY", NarratorService::handleGuard);
        this.addMessageHandler("narrator_enter", "SUPER_CHAT_MESSAGE", NarratorService::handleSuperChat);
        Platform.runLater(() -> {
            Utils.showDialogMessage("连接直播弹幕服务成功", "BLACK", QueueManager.INSTANCE.getMainScene().getRootStackPane());
            if (QueueManager.INSTANCE.getMainScene().getBorderPane().getCenter() instanceof DanmakuServiceContainer container && container.getInnerContainer().getId().equals("c0")) {
                container.connectedButton(((QMButton) ((BorderPane) container.getInnerContainer().getChildren().getFirst()).getRight()));
            }
        });
        WebSocket.Listener.super.onOpen(webSocket);
    }

    public void sendHeartbeat(WebSocket socket) throws IOException {
        int size = 16;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.putInt(size);
        buffer.putShort((short) 16);
        buffer.putShort((short) 1);
        buffer.putInt(2);
        buffer.putInt(sequence);
        sequence++;
        sendPacket(socket, buffer);
    }

    public void sendPacket(WebSocket webSocket, ByteBuffer buffer) throws IOException {
        for (byte b : buffer.array()) {
            System.out.print(Integer.toHexString(b & 0xFF));
        }
        webSocket.sendBinary(buffer, true);
    }

    public Pair<Integer, Optional<JsonObject>> receivePacket(ByteBuffer buffer) throws IOException {
        int size = buffer.getInt();
        short headSize = buffer.getShort();
        short protocolVersion = buffer.getShort();
        int opcode = buffer.getInt();
        byte[] body = new byte[size - headSize];
        buffer.get(body);
        switch (protocolVersion) {
            case 2 -> body = decompress(body);
            case 3 -> body = decompressBrotli(body);
        }
        if (opcode == 3) {
            return new Pair<>(opcode, Optional.empty());
        } else if (opcode == 8) {
            JsonObject jsonObject = JsonParser.parseString(new String(body)).getAsJsonObject();
            if (jsonObject.has("code") && jsonObject.get("code").getAsInt() == 0) {
                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            if (webSocket.isInputClosed()) {
                                this.cancel();
                            }
                            sendHeartbeat(webSocket);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, 0, 30000);
                return new Pair<>(opcode, null);
            } else {
                return new Pair<>(opcode, Optional.of(jsonObject));
            }
        }
        return new Pair<>(opcode, Optional.of(JsonParser.parseString(new String(body)).getAsJsonObject()));
    }

    public void addMessageHandler(String id, String cmdType, Consumer<JsonObject> consumer) {
        this.messageHandlers.add(new MessageHandler(id, cmdType, consumer));
    }

    public boolean isSessionAvailable() {
        return webSocket != null && (webSocket.isInputClosed() ||  !webSocket.isOutputClosed());
    }

    public void close() throws IOException {
        this.webSocket.sendClose(0, "closed");
    }

    public static byte[] decompress(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        InflaterInputStream fis = new InflaterInputStream(bais);
        return outputDecompress(fis);
    }

    public static byte[] decompressBrotli(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        BrotliInputStream bis = new BrotliInputStream(bais);
        return outputDecompress(bis);
    }

    private static byte[] outputDecompress(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        is.close();
        return bos.toByteArray();
    }

    public static void connect() {
        try (CloseableHttpClient httpclient = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build()) {
            URI uri = new URIBuilder("https://api.live.bilibili.com/live_user/v1/Master/info")
                    .addParameter("uid", String.valueOf(Settings.MID)).build();
            HttpGet httpget = new HttpGet(uri);
            httpget.setConfig(Settings.DEFAULT_REQUEST_CONFIG);
            CloseableHttpResponse response = httpclient.execute(httpget);
            JsonObject object = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
            if (object.get("code").getAsInt() == 0) {
                long roomId = object.getAsJsonObject("data").get("room_id").getAsLong();
                if (roomId != 0) {
                    URI uri1 = new URIBuilder("https://api.live.bilibili.com/xlive/web-room/v1/index/getRoomBaseInfo")
                            .addParameter("req_biz", "web_room_componet")
                            .addParameter("room_ids", String.valueOf(roomId)).build();
                    HttpGet httpget1 = new HttpGet(uri1);
                    httpget1.setConfig(Settings.DEFAULT_REQUEST_CONFIG);
                    CloseableHttpResponse response1 = httpclient.execute(httpget1);
                    JsonObject obj1 = JsonParser.parseString(EntityUtils.toString(response1.getEntity())).getAsJsonObject();
                    if (obj1.get("code").getAsInt() == 0) {
                        roomId = Long.parseLong(obj1.getAsJsonObject("data").getAsJsonObject("by_room_ids").keySet().iterator().next());
                        LiveMessageService.connect(Settings.MID, roomId);
                    } else {
                        throw new RuntimeException(obj1.get("message").getAsString());
                    }
                } else {
                    throw new RuntimeException("请先在 B 站开通直播间");
                }
            } else {
                throw new RuntimeException(object.get("message").getAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ConnectFailedException(e.getMessage());
        }
    }

    public static void connect(long uid, long roomId) {
        MessageStreamVerification verification = getMessageStreamVerification(roomId);
        if (verification.hosts == null || verification.hosts.isEmpty()) {
            throw new RuntimeException(verification.token());
        } else {
            String hostname = verification.hosts.getFirst().hostname;
            Thread thread = new Thread(() -> {
                try {
                    URI uri = new URI("wss://%s:%s/sub".formatted(hostname, verification.hosts.getFirst().wssPort));
                    System.out.println(uri);
                    HttpClient client = HttpClient.newHttpClient();
                    INSTANCE = new LiveMessageService(uid, roomId, verification.token, Settings.getCookieStore().getCookies().stream().filter(cookie -> cookie.getName().equals("buvid3")).findFirst().get().getValue());
                    client.newWebSocketBuilder().header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:141.0) Gecko/20100101 Firefox/141.0").buildAsync(uri, INSTANCE).join();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
        }
    }

    public static LiveMessageService getInstance() {
        return INSTANCE;
    }

    private static MessageStreamVerification getMessageStreamVerification(long roomId) {
        TreeMap<String, Object> map = new TreeMap<>();
        map.put("id", roomId);
        map.put("type", 0);
        map.put("web_location", 444.8);
        try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build()) {
            Utils.fillCookies(Settings.getCookieStore());
            URI signedUri = WbiSign.getSignedUri(new URIBuilder("https://api.live.bilibili.com/xlive/web-room/v1/index/getDanmuInfo"), map);
            if (signedUri == null) {
                throw new RuntimeException("Failed to get signedUri");
            }
            HttpGet httpGet = new HttpGet(signedUri);
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:141.0) Gecko/20100101 Firefox/141.0");
            httpGet.setConfig(Settings.DEFAULT_REQUEST_CONFIG);
            HttpResponse response = client.execute(httpGet);
            JsonObject object = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
            if (object.get("code").getAsInt() == 0) {
                List<Host> hosts = new ArrayList<>();
                JsonObject data = object.getAsJsonObject("data");
                data.getAsJsonArray("host_list").forEach(host -> {
                    JsonObject hostObj = host.getAsJsonObject();
                    hosts.add(new Host(hostObj.get("host").getAsString(), hostObj.get("port").getAsInt(), hostObj.get("wss_port").getAsInt(), hostObj.get("ws_port").getAsInt()));
                });
                return new MessageStreamVerification(data.get("token").getAsString(), hosts);
            } else {
                return new MessageStreamVerification("%s(%s)".formatted(object.get("message").getAsString(), object.get("code").getAsInt()), null);
            }
        } catch (Exception e) {
            return new MessageStreamVerification("获取验证密钥失败", null);
        }
    }

    public record MessageStreamVerification(String token, List<Host> hosts) {
    }

    public record Host(String hostname, int port, int wssPort, int wsPort) {
    }

    public static class MessageHandler {
        private final String id;
        private final String cmdType;
        private final Consumer<JsonObject> consumer;

        public MessageHandler(String id, String cmdType, Consumer<JsonObject> consumer) {
            this.id = id;
            this.cmdType = cmdType;
            this.consumer = consumer;
        }

        public void handle(JsonObject obj) {
            if (obj.get("cmd").getAsString().equals(cmdType)) {
                this.consumer.accept(obj);
            }
        }

        public String getId() {
            return id;
        }
    }

    public static class ConnectFailedException extends RuntimeException {
        public ConnectFailedException(String message) {
            super(message);
        }
    }

    public static class VerifyFailedException extends RuntimeException {
        public VerifyFailedException(String message) {
            super(message);
        }
    }
}
