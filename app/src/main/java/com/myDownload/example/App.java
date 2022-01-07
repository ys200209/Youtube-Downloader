package com.myDownload.example;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.devbrackets.android.exomedia.BuildConfig;
import com.myDownload.ffmpeg.FFmpeg;
import com.myDownload.youtubedl_android.YoutubeDL;
import com.myDownload.youtubedl_android.YoutubeDLException;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class App extends Application {

    private static final String TAG = "App";
    private boolean isDownloading = false;

    public void setDownloading(boolean b) {
        isDownloading = b;
    }

    public boolean getDownloading() {
        return isDownloading;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        configureRxJavaErrorHandler();
        Completable.fromAction(this::initLibraries).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                // it worked
            }

            @Override
            public void onError(Throwable e) {
                if(BuildConfig.DEBUG) Log.e(TAG, "failed to initialize youtubedl-android", e);
                Toast.makeText(getApplicationContext(), "initialization failed: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }

        });

    }

    private void configureRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler(e -> {

            if (e instanceof UndeliverableException) {
                // As UndeliverableException is a wrapper, get the cause of it to get the "real" exception
                e = e.getCause();
            }

            if (e instanceof InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return;
            }

            Log.e(TAG, "Undeliverable exception received, not sure what to do", e);
        });
    }

    private void initLibraries() throws YoutubeDLException {
        try {
            YoutubeDL.getInstance().init(this);
            //FFmpeg.getInstance().init(this);
            FFmpeg.getInstance().init(getApplicationContext());
        } catch (YoutubeDLException e) {
            Log.e(TAG, "failed to initialize youtubedl-android", e);
        }
    }

    public static int isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if( ni != null && ni.isConnected()) {
            if( ni.getType() == ConnectivityManager.TYPE_MOBILE) {
                // 3g, 4g 인 경우
                return 1;
            } else if (ni.getType() == ConnectivityManager.TYPE_WIFI) {
                // WIFI인 경우
                return 2;
            }
        }
        // 연결 안됨
        return 0;
    }

}
