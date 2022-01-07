package com.myDownload.youtubedl_android;

public interface DownloadProgressCallback {
    void onProgressUpdate(float progress, long etaInSeconds, String title);
}
