package top.pigest.queuemanagerdemo.music;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import org.apache.http.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.util.Utils;

import java.io.FileOutputStream;
import java.util.concurrent.CompletableFuture;

public class MusicDownloader {
    private CompletableFuture<String> future;
    private Song downloading = null;
    private final MusicHandler musicHandler;
    public MusicDownloader(MusicHandler musicHandler) {
        this.musicHandler = musicHandler;
    }

    protected void downloadIfNotDownloading(Song song) {
        if (!isDownloading(song)) {
            download(song);
        }
    }

    protected void download(Song song) {
        stopDownload();
        downloading = song;
        future = CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject object = Music163Api.player(song.getId(), "aac", Settings.getMusicServiceSettings().musicLevel.toString());
                String url = object.getAsJsonArray("data").get(0).getAsJsonObject().get("url").getAsString();
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    HttpGet httpGet = new HttpGet(url);
                    HttpResponse response = httpClient.execute(httpGet);
                    HttpEntity entity = response.getEntity();
                    Header header = response.getFirstHeader("Content-Disposition");
                    String fileFormat = "m4a";
                    for (HeaderElement he : header.getElements()) {
                        NameValuePair np = he.getParameterByName("filename");
                        fileFormat = np.getValue().substring(np.getValue().lastIndexOf('.') + 1);
                    }
                    FileOutputStream fos = new FileOutputStream(MusicHandler.getFile(song, fileFormat));
                    fos.write(EntityUtils.toByteArray(entity));
                    fos.close();
                    return fileFormat;
                }
            } catch (Exception e) {
                Platform.runLater(() -> Utils.showDialogMessage("歌曲下载失败", true, this.musicHandler.player.getRootStackPane()));
                return null;
            }
        });
        future.whenComplete((result, throwable) -> {
            if (result != null) {
                downloading.setFormat(result);
                if (this.musicHandler.getCurrentSong().equals(downloading)) {
                    this.musicHandler.play(downloading);
                }
            }
            downloading = null;
        });
    }

    private void stopDownload() {
        Utils.onPresent(future, future -> future.cancel(true));
        this.downloading = null;
    }

    public boolean isDownloading() {
        return future != null && !future.isDone();
    }

    boolean isDownloading(Song song) {
        return isDownloading() && song.getId().equals(downloading.getId());
    }
}
