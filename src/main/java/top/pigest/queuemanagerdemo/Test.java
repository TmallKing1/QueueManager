package top.pigest.queuemanagerdemo;

import com.google.gson.JsonObject;
import top.pigest.queuemanagerdemo.system.LiveMessageService;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public class Test {

    public static void main(String[] args) throws URISyntaxException, InterruptedException, IOException {
        URI uri = new URI("wss://%s:%s/sub".formatted("broadcastlv.chat.bilibili.com", 2245));
        System.out.println(uri);
        HttpClient client = HttpClient.newHttpClient();
        WebSocket socket = client.newWebSocketBuilder().header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:141.0) Gecko/20100101 Firefox/141.0").buildAsync(uri,
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                        System.out.println(11111);
                        return WebSocket.Listener.super.onBinary(webSocket, data, last);
                    }
                }).join();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("roomid", 22472314);
        String s = jsonObject.toString();
        int size = 0xff;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.putInt(size);
        buffer.putShort((short) 16);
        buffer.putShort((short) 1);
        buffer.putInt(7);
        buffer.putInt(1);
        buffer.put(s.getBytes());
        socket.sendBinary(buffer, true);
    }
}
