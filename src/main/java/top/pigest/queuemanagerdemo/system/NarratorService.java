package top.pigest.queuemanagerdemo.system;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.system.settings.DanmakuServiceSettings;

import java.io.*;
import java.util.*;

public class NarratorService {
    private static Thread CHECK_THREAD;
    private static final List<Process> PROCESSES = new ArrayList<>();
    private static BufferedWriter WRITER;
    private static final List<String> WAIT_FOR_SPEAKING = new ArrayList<>();
    private static final List<GiftComboSession> GIFT_COMBO_SESSIONS = new ArrayList<>();

    public static void speakNext(String text) {
        try {
            DanmakuServiceSettings settings = Settings.getDanmakuServiceSettings();
            speakNext(text, settings.narratorRate, settings.narratorVolume, settings.narratorVoiceName);
        } catch (Exception ignored) {
        }
    }

    public static void startSpeaking() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-NoExit", "-Command", "-");
        Process process = pb.start();
        PROCESSES.add(process);
        WRITER = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), "GBK"));

        String command = """
                Add-Type -AssemblyName System.speech
                $speak = New-Object System.Speech.Synthesis.SpeechSynthesizer
                """;
        WRITER.write(command);
        WRITER.flush();
        //WRITER.close();
    }

    public static void speakNext(String text, double rate, int volume, String voice) throws IOException {
        if (PROCESSES.stream().noneMatch(Process::isAlive)) {
            startSpeaking();
        }
        String command = "$speak.Rate = %s\n$speak.Volume = %d\n%s$speak.SpeakAsync('%s')\n".formatted(rate, volume, voice != null ? "$speak.SelectVoice('" + voice.replace("'", "''") + "'); " : "", text.replace("'", "''"));
        WRITER.write(command);
        WRITER.flush();
    }

    public static void speakIndependent(String text, double rate, int volume, String voice) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("powershell", "-NoExit", "-Command", "-");
        Process process = pb.start();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), "GBK"));
        String command = "Add-Type -AssemblyName System.speech\n$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer\n$speak.Rate = %s\n$speak.Volume = %d\n%s$speak.SpeakAsync('%s')\n".formatted(rate, volume, voice != null ? "$speak.SelectVoice('" + voice.replace("'", "''") + "'); " : "", text.replace("'", "''"));
        writer.write(command);
        writer.flush();
    }

    public static void stopSpeaking() {
        PROCESSES.forEach(process -> {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            try {
                writer.write("$speak.SpeakAsyncCancelAll()\n");
                writer.flush();
            } catch (IOException e) {
            }
            process.destroy();
        });
        PROCESSES.clear();
    }

    public static List<Voice> getAvailableVoices() {
        List<Voice> voices = new ArrayList<>();
        try {

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoExit", "-Command", "-");
            String command = """
                    Add-Type -AssemblyName System.speech
                    $speak = New-Object System.Speech.Synthesis.SpeechSynthesizer
                    $speak.GetInstalledVoices('zh-CN') | \
                    ForEach-Object { $_.VoiceInfo.Name }
                    exit
                    """;

            Process process = pb.start();
            Writer writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), "GBK"));
            writer.write(command);
            writer.flush();
            writer.close();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        voices.add(new Voice(false, line.trim(), null));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return voices;
    }

    public static List<Voice> getRegistrableVoices() {
        List<Voice> voices = new ArrayList<>();
        String windir = System.getenv("windir");
        File file = new File(windir + "\\Speech_OneCore\\Engines\\TTS\\zh-CN");
        String[] list = file.list();
        if (file.isDirectory() && list != null) {
            if (Arrays.stream(list).anyMatch(s -> s.contains("M2052Yaoyao"))) {
                voices.add(new Voice(true, "Microsoft Yaoyao Desktop", Objects.requireNonNull(QueueManager.class.getResource("yy.reg")).getFile()));
            }
            if (Arrays.stream(list).anyMatch(s -> s.contains("M2052Kangkang"))) {
                voices.add(new Voice(true, "Microsoft Kangkang Desktop", Objects.requireNonNull(QueueManager.class.getResource("kk.reg")).getFile()));
            }
        }
        return voices;
    }

    public static void handleSingleDanmaku(JsonObject jsonObject) {
        DanmakuServiceSettings settings = Settings.getDanmakuServiceSettings();
        if (settings.narratorEnabled && settings.acceptedTypes.contains(DanmakuServiceSettings.NarratableElement.DANMAKU)) {
            JsonArray info = jsonObject.getAsJsonArray("info");
            String text = info.get(1).getAsString();
            String userName = info.get(2).getAsJsonArray().get(1).getAsString();
            String bar = settings.getNarratorText(DanmakuServiceSettings.NarratableElement.DANMAKU)
                    .replace("{user}", userName)
                    .replace("{comment}", text);
            addString(bar);
        }
    }

    public static void handleInteract(JsonObject jsonObject) {
        DanmakuServiceSettings settings = Settings.getDanmakuServiceSettings();
        if (settings.narratorEnabled && settings.acceptedTypes.contains(DanmakuServiceSettings.NarratableElement.ENTER)) {
            byte[] bytes = Base64.getDecoder().decode(jsonObject.getAsJsonObject("data").get("pb").getAsString());
            try {
                InteractWordOuterClass.InteractWord interactWord = InteractWordOuterClass.InteractWord.parseFrom(bytes);
                if (interactWord.getMsgType() == 1) {
                    String userName = interactWord.getUname();
                    String bar = settings.getNarratorText(DanmakuServiceSettings.NarratableElement.ENTER)
                            .replace("{user}", userName);
                    addString(bar);
                }
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void handleGift(JsonObject jsonObject) {
        GIFT_COMBO_SESSIONS.removeIf(GiftComboSession::isDead);
        DanmakuServiceSettings settings = Settings.getDanmakuServiceSettings();
        if (settings.narratorEnabled && settings.acceptedTypes.contains(DanmakuServiceSettings.NarratableElement.GIFT)) {
            String userName = jsonObject.getAsJsonObject("data").get("uname").getAsString();
            int num =  jsonObject.getAsJsonObject("data").get("num").getAsInt();
            String giftName = jsonObject.getAsJsonObject("data").get("giftName").getAsString();
            if (settings.giftComboOptimization) {
                Optional<GiftComboSession> optional = GIFT_COMBO_SESSIONS.stream().filter(s -> s.getUserName().equals(userName) && s.getGiftName().equals(giftName) && !s.isDead()).findFirst();
                if (optional.isPresent()) {
                    optional.get().onNewGiftReceived(num);
                    return;
                } else {
                    GIFT_COMBO_SESSIONS.add(new GiftComboSession(userName, giftName, num));
                }
            }
            String bar = settings.getNarratorText(DanmakuServiceSettings.NarratableElement.GIFT)
                    .replace("{user}", userName)
                    .replace("{amount}", String.valueOf(num))
                    .replace("{gift}", giftName);
            addString(bar);
        }
    }

    public static void handleGuard(JsonObject jsonObject) {
        DanmakuServiceSettings settings = Settings.getDanmakuServiceSettings();
        if (settings.narratorEnabled && settings.acceptedTypes.contains(DanmakuServiceSettings.NarratableElement.GUARD)) {
            String userName = jsonObject.getAsJsonObject("data").get("username").getAsString();
            int num = jsonObject.getAsJsonObject("data").get("num").getAsInt();
            int guardLevel = jsonObject.getAsJsonObject("data").get("guard_level").getAsInt();
            String guardName = switch (guardLevel) {
                case 1 -> "舰长";
                case 2 -> "提督";
                case 3 -> "总督";
                default -> "鬼知道B站又出了什么新的东西";
            };
            String bar;
            if (settings.multiGuardOptimization) {
                bar = settings.multiGuardText;

            } else {
                bar = settings.getNarratorText(DanmakuServiceSettings.NarratableElement.GUARD);
            }
            bar = bar.replace("{user}", userName)
                    .replace("{amount}", String.valueOf(num))
                    .replace("{guard}", guardName);
            addString(bar);
        }
    }

    public static void handleSuperChat(JsonObject jsonObject) {
        DanmakuServiceSettings settings = Settings.getDanmakuServiceSettings();
        if (settings.narratorEnabled && settings.acceptedTypes.contains(DanmakuServiceSettings.NarratableElement.SUPER_CHAT)) {
            String userName = jsonObject.getAsJsonObject("data").getAsJsonObject("user_info").get("uname").getAsString();
            String message = jsonObject.getAsJsonObject("data").get("message").getAsString();
            String bar = settings.getNarratorText(DanmakuServiceSettings.NarratableElement.SUPER_CHAT)
                    .replace("{user}", userName)
                    .replace("{comment}", message);
            addString(bar);
        }
    }

    public static void addString(String s) {
        WAIT_FOR_SPEAKING.add(s);
        if (CHECK_THREAD == null || !CHECK_THREAD.isAlive()) {
            CHECK_THREAD = new Thread(() -> {
                while (!WAIT_FOR_SPEAKING.isEmpty()) {
                    DanmakuServiceSettings settings = Settings.getDanmakuServiceSettings();
                    switch (settings.narratorType) {
                        case DEFAULT -> speakNext(WAIT_FOR_SPEAKING.removeFirst());
                        case INTERRUPTED -> {
                            stopSpeaking();
                            speakNext(WAIT_FOR_SPEAKING.removeFirst());
                        }
                        case STACKABLE -> {
                            try {
                                speakIndependent(WAIT_FOR_SPEAKING.removeFirst(), settings.narratorRate, settings.narratorVolume, settings.narratorVoiceName);
                            } catch (IOException ignored) {
                            }
                        }
                    }
                }
            });
            CHECK_THREAD.start();
        }
    }

    public static class GiftComboSession {
        private Timer timer;
        private final String userName;
        private final String giftName;
        private final int firstCount;
        private long startTime;
        private int count;
        public GiftComboSession(String userName, String giftName, int count) {
            this.userName = userName;
            this.giftName = giftName;
            this.firstCount = count;
            this.onNewGiftReceived(count);
        }

        public String getUserName() {
            return userName;
        }

        public String getGiftName() {
            return giftName;
        }

        public void onNewGiftReceived(int count) {
            this.count += count;
            if (this.timer != null) {
                this.timer.cancel();
            }
            this.timer = new Timer();
            this.timer.schedule(new TimerTask() {
                public void run() {
                    if (Settings.getDanmakuServiceSettings().giftComboOptimization && GiftComboSession.this.count > GiftComboSession.this.firstCount) {
                        String bar = Settings.getDanmakuServiceSettings().giftComboEndText
                                .replace("{user}", userName)
                                .replace("{amount}", String.valueOf(GiftComboSession.this.count))
                                .replace("{gift}", giftName);
                        addString(bar);
                    }
                }
            }, 5000);
            this.startTime = System.currentTimeMillis();
        }

        public boolean isDead() {
            return System.currentTimeMillis() - this.startTime > 5000;
        }
    }

    public record Voice(boolean requireRegistration, String name, String registryPath) {
        @Override
        public String toString() {
            return name;
        }
    }
}
