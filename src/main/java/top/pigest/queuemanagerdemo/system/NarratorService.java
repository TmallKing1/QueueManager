package top.pigest.queuemanagerdemo.system;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.system.settings.DanmakuServiceSettings;

import java.io.*;
import java.util.*;

public class NarratorService {
    private static Thread CHECK_THREAD;
    private static List<Process> PROCESSES;
    private static BufferedWriter WRITER;
    private static final List<String> WAIT_FOR_SPEAKING = new ArrayList<>();

    public static void startSpeaking(String text) {
        try {
            DanmakuServiceSettings settings = Settings.getDanmakuServiceSettings();
            startSpeaking(WAIT_FOR_SPEAKING.removeFirst(), settings.narratorRate, settings.narratorVolume, settings.narratorVoiceName);
        } catch (Exception ignored) {
        }
    }

    public static void startSpeaking(String text, double rate, int volume, String voice) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-NoExit", "-Command", "-");
        Process process = pb.start();
        PROCESSES.add(process);
        WRITER = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), "GBK"));

        String command = "Add-Type -AssemblyName System.speech\n" +
                "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer\n" +
                "$speak.Rate = " + rate + "\n" +
                "$speak.Volume = " + volume + "\n" +
                (voice != null ? "$speak.SelectVoice('" + voice.replace("'", "''") + "'); " : "") +
                "$speak.Speak('" + text.replace("'", "''") + "')\n" +
                "exit\n";
        WRITER.write(command);
        WRITER.flush();
        WRITER.close();
    }

    public static void stopSpeaking() {
        if (isSpeaking()) {
            try {
                WRITER.write("$speak.Speak('', [System.Speech.Synthesis.SpeakFlags]::PurgeBeforeSpeak)\n");
                WRITER.write("exit\n");
                WRITER.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                PROCESSES.forEach(Process::destroy);
                try {
                    WRITER.close();
                } catch (IOException ignored) {
                }
                PROCESSES.clear();
                WRITER = null;
            }
        }
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

    public static boolean isSpeaking() {
        return PROCESSES.stream().anyMatch(Process::isAlive);
    }

    public static void handleSingleDanmaku(JsonObject jsonObject) {
        if (Settings.getDanmakuServiceSettings().narratorEnabled && Settings.getDanmakuServiceSettings().acceptedTypes.contains(DanmakuServiceSettings.NarratableElement.DANMAKU)) {
            JsonArray info = jsonObject.getAsJsonArray("info");
            String text = info.get(1).getAsString();
            String userName = info.get(2).getAsJsonArray().get(1).getAsString();
            String bar = Settings.getDanmakuServiceSettings().getNarratorText(DanmakuServiceSettings.NarratableElement.DANMAKU);
            bar = bar.replace("{user}", userName).replace("{comment}", text);
            addString(bar);
        }
    }

    public static void handleInteract(JsonObject jsonObject) {
        if (Settings.getDanmakuServiceSettings().narratorEnabled && Settings.getDanmakuServiceSettings().acceptedTypes.contains(DanmakuServiceSettings.NarratableElement.ENTER)) {
            int type = jsonObject.getAsJsonObject("data").get("msg_type").getAsInt();
            if (type == 1) {
                String userName = jsonObject.getAsJsonObject("data").get("uname").getAsString();
                String bar = Settings.getDanmakuServiceSettings().getNarratorText(DanmakuServiceSettings.NarratableElement.ENTER);
                bar = bar.replace("{user}", userName);
                addString(bar);
            }
        }
    }

    public static void handleGift(JsonObject jsonObject) {
        if (Settings.getDanmakuServiceSettings().narratorEnabled && Settings.getDanmakuServiceSettings().acceptedTypes.contains(DanmakuServiceSettings.NarratableElement.GIFT)) {
            String userName = jsonObject.getAsJsonObject("data").get("uname").getAsString();
            String giftName = jsonObject.getAsJsonObject("data").get("giftName").getAsString();
            String bar = Settings.getDanmakuServiceSettings().getNarratorText(DanmakuServiceSettings.NarratableElement.GIFT);
            bar = bar.replace("{user}", userName);
            bar = bar.replace("{gift}", giftName);
            addString(bar);
        }
    }

    public static void handleGuard(JsonObject jsonObject) {
        if (Settings.getDanmakuServiceSettings().narratorEnabled && Settings.getDanmakuServiceSettings().acceptedTypes.contains(DanmakuServiceSettings.NarratableElement.GUARD)) {
            String userName = jsonObject.getAsJsonObject("data").get("username").getAsString();
            int guardLevel = jsonObject.getAsJsonObject("data").get("guard_level").getAsInt();
            String guardName = switch (guardLevel) {
                case 1 -> "舰长";
                case 2 -> "提督";
                case 3 -> "总督";
                default -> "鬼知道B站又出了什么新的东西";
            };
            String bar = Settings.getDanmakuServiceSettings().getNarratorText(DanmakuServiceSettings.NarratableElement.GUARD);
            bar = bar.replace("{user}", userName);
            bar = bar.replace("{guard}", guardName);
            addString(bar);
        }
    }

    public static void handleSuperChat(JsonObject jsonObject) {
        if (Settings.getDanmakuServiceSettings().narratorEnabled && Settings.getDanmakuServiceSettings().acceptedTypes.contains(DanmakuServiceSettings.NarratableElement.SUPER_CHAT)) {
            String userName = jsonObject.getAsJsonObject("data").getAsJsonObject("user_info").get("uname").getAsString();
            String message = jsonObject.getAsJsonObject("data").get("message").getAsString();
            String bar = Settings.getDanmakuServiceSettings().getNarratorText(DanmakuServiceSettings.NarratableElement.SUPER_CHAT);
            bar = bar.replace("{user}", userName);
            bar = bar.replace("{comment}", message);
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
                        case DEFAULT -> {
                            Timer timer = new Timer();
                            timer.scheduleAtFixedRate(new TimerTask() {
                                @Override
                                public void run() {
                                    if (!isSpeaking()) {
                                        PROCESSES.removeIf(process -> !process.isAlive());
                                        startSpeaking(WAIT_FOR_SPEAKING.removeFirst());
                                    }
                                }
                            }, 0, 200);
                        }
                        case INTERRUPTED -> {
                            stopSpeaking();
                            startSpeaking(WAIT_FOR_SPEAKING.removeFirst());
                        }
                        case STACKABLE -> {
                            PROCESSES.removeIf(process -> !process.isAlive());
                            startSpeaking(WAIT_FOR_SPEAKING.removeFirst());
                        }
                    }
                }
            });
            CHECK_THREAD.start();
        }
    }

    public record Voice(boolean requireRegistration, String name, String registryPath) {
        @Override
        public String toString() {
            return name;
        }
    }
}
