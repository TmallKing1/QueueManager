package top.pigest.queuemanagerdemo.music;

import top.pigest.queuemanagerdemo.util.IndexedArrayList;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lyrics {
    public static final String D_D_D = "\\[(\\d+):(\\d+)\\.(\\d+)](.*)";
    public static final Lyrics LOADING = new Lyrics("[00:00.00]加载歌词中");
    public static final Lyrics NONE = new Lyrics("[00:00.00]暂无歌词");
    public IndexedArrayList<Lyric> lyrics = new IndexedArrayList<>();

    public Lyrics(String lyric) {
        for (String s : lyric.split("\n")) {
            Matcher m = Pattern.compile(D_D_D).matcher(s);
            if (m.matches()) {
                int min = Integer.parseInt(m.group(1));
                int sec = Integer.parseInt(m.group(2));
                int mil = Integer.parseInt(m.group(3));
                String text = m.group(4).trim();
                if (!text.isEmpty()) {
                    int time = min * 60 * 1000 + sec * 1000 + mil;
                    this.lyrics.add(new Lyric(time, text));
                }
            }
        }
        lyrics.sort(Comparator.comparingInt(Lyric::time));
        if (lyrics.isEmpty()) {
            lyrics.add(new Lyric(0, "暂无歌词"));
        }
    }

    public String getLyric(int time) {
        while (lyrics.hasNext() && lyrics.getNext().time < time) {
            lyrics.next();
        }
        return lyrics.current().text;
    }

    public void reset() {
        lyrics.setIndex(0);
    }

    public record Lyric(int time, String text) {

    }
}
