package com.myDownload.example.domain;

public enum MediaType {
    MP3,
    VIDEO,
    ;


    public static MediaType from(String type) {
        if (type.equals("mp3")) {
            return MP3;
        }
        return VIDEO;
    }
}
