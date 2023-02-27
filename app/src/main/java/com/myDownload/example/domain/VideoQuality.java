package com.myDownload.example.domain;

public class VideoQuality {
    private final int width;
    private final int height;
    private final Long size;

    public VideoQuality(int width, int height, Long size) {
        this.width = width;
        this.height = height;
        this.size = size;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Long getSize() {
        return size;
    }

    public boolean isPortraitMode() {
        return width <= height;
    }

    public String getQualityQuery() {
        /*if (portraitMode) {
            return "bv*[width<=" + width + "]+ba/b[width<=" + width + "]";
        }*/
//        System.out.println("height: " + height);
//        return "bv*[height<=" + height + "]+ba/b[height<=" + height + "]";
        return "bv*[height<=" + height + "]+bestaudio/best";
    }

    @Override
    public String toString() {
        return "\nVideoQuality{\n" +
                "width=" + width +
                ", \nheight=" + height +
                ", \nsize=" + size +
                "\n}";
    }
}
