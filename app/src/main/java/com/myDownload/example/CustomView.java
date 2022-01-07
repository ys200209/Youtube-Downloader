package com.myDownload.example;

import android.graphics.Bitmap;
import android.widget.ImageView;

public class CustomView {
    private ImageView thumbnail;
    private String title;
    private int progressBar;
    private String progressPersent;
    private String fileSize;
    private Bitmap mBitmap;
    private String tvDownloadStatus;
    private String url; // 비디오 URL
    private String selectQuality; // 퀄리티 문자열
    private boolean isDispos;

    public CustomView() { }

    public CustomView(Bitmap mBitmap, String title, int progressBar,
                      String progressPersent, String fileSize, String tvDownloadStatus,
                      String url, String selectQuality, boolean isDispos) {
        this.mBitmap = mBitmap;
        this.title = title;
        this.progressBar = progressBar;
        this.progressPersent = progressPersent;
        this.fileSize = fileSize;
        this.tvDownloadStatus = tvDownloadStatus;
        this.url = url;
        this.selectQuality = selectQuality;
        this.isDispos = isDispos;
    }
    public boolean getIsDispos() {
        return isDispos;
    }

    public void setIsDispos() {
        this.isDispos = !isDispos;
    }

    public String getUrl() {
        return url;
    }

    public String getSelectQuality() {
        return selectQuality;
    }

    public ImageView getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(ImageView thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getTitle() {
        //Log.d("태그", "CustomView 안의 getTitle : " + title);
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getProgressBar() {
        //Log.d("태그", "CustomeView 안의 progress : " + progressBar);
        return progressBar;
    }

    public void setProgressBar(int progressBar) {
        this.progressBar = progressBar;
    }

    public String getProgressPersent() {
        return progressPersent;
    }

    public void setProgressPersent(String progressPersent) {
        this.progressPersent = progressPersent;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public Bitmap getmBitmap() {
        return mBitmap;
    }

    public void setmBitmap(Bitmap mBitmap) {
        this.mBitmap = mBitmap;
    }

    public String getTvDownloadStatus() {
        return tvDownloadStatus;
    }

    public void setTvDownloadStatus(String tvDownloadStatus) {
        this.tvDownloadStatus = tvDownloadStatus;
    }

}
