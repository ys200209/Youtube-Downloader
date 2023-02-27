package com.myDownload.example.domain;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import java.util.HashMap;
import java.util.Map;

public class VideoQualities {
    public static final String AUDIO_IDENTIFIER = "medium";
    public static final String AUDIO_QUALITY_QUERY = "bestaudio[ext=m4a]";
    //    private List<VideoQuality> videoQualities = new ArrayList<>();
    private Map<String, VideoQuality> videoQualities = new HashMap<>();
    private String recentlyQuality = null;

    public boolean isContainsQuality(String qualityFormat) {
        return videoQualities.containsKey(qualityFormat);
    }

    public boolean isRecentlySaveFormat(String qualityFormat) {
        return qualityFormat.equals(recentlyQuality);
    }

    public void add(String qualityFormat, VideoQuality newVideoQuality) {
        videoQualities.put(qualityFormat, newVideoQuality);
        recentlyQuality = qualityFormat;
    }

    public void update(String qualityFormat, VideoQuality updateVideoQuality) {
        videoQualities.put(qualityFormat, updateVideoQuality);
    }

    public Long getAudioSize() {
        VideoQuality audioQuality = videoQualities.get(AUDIO_IDENTIFIER);
        if (audioQuality == null) {
            return null;
        }
        return audioQuality.getSize();
    }

    public Long getVideoSize(String qualityFormat) {
        return videoQualities.get(qualityFormat).getSize();
    }

    public boolean isPortraitMode() {
        if (VERSION.SDK_INT >= VERSION_CODES.N) {
            return videoQualities.values().stream()
                    .allMatch(VideoQuality::isPortraitMode);
        }
        return false;
    }

    public String getQualityQuery(MediaType mediaType, String selectedItem) { // 타입, 화질, 세로모드 여부
        if (mediaType == MediaType.MP3) {
            return AUDIO_QUALITY_QUERY;
        }
        return videoQualities.get(getSelectedQuality(selectedItem)).getQualityQuery();
    }

    private String getSelectedQuality(String selectedItem) {
        String result = selectedItem.substring(0, selectedItem.indexOf(" - ") - 1);
        System.out.println("result = " + result);
        return result;
    }

    @Override
    public String toString() {
        return "VideoQualities{\n" +
                "videoQualities=" + videoQualities +
                "}\n";
    }
}
