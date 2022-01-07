package com.myDownload.example;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.myDownload.youtubedl_android.DownloadProgressCallback;
import com.myDownload.youtubedl_android.YoutubeDL;
import com.myDownload.youtubedl_android.YoutubeDLRequest;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MyDownloadService extends Service {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private BroadcastReceiver mReceiver;
    private final IBinder mBinder = new LocalBinder();
    private String FilterTitle;
    private String title;
    private String url;
    private String selectQuality;
    private String thumbnail_URL;
    private String tvDownloadStatus = "다운로드중...";
    private boolean downloading = false;
    boolean checkPersent = true;
    static private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ListView CustomListView;
    private CompletedDownloadActivity complet = new CompletedDownloadActivity();
    private CompletedDownloadActivity.CustomAdapter adapter;
    static private ArrayList<CustomView> customList;
    private Bitmap mBitmap;
    private View mView;
    private BackThread thread;
    private boolean isDownloadingService = false;
    static Disposable disposable;
    private static Map<String, Disposable> disposMap = new HashMap<>(); // static을 앞으로 선언하도록 바꿈.. 21.07.16
    private static ArrayList<Disposable> test = new ArrayList<>(); // static 으로 선언해야됨
    private String path = Environment.getExternalStorageDirectory().toString() + "/Movies/Download/";
    private DbOpenHelper mDbOpenHelper = new DbOpenHelper(MyDownloadService.this);
    private String title_;
    // private int networkState; // 네트워크 상태감지 변수
    // private int checkNetworkState; // 네트워크 상태감지 변수
    private ConnectivityManager cm;
    // private NetworkInfo ni;

    private DownloadProgressCallback callback = new DownloadProgressCallback() {
        @Override
        public void onProgressUpdate(float progress, long etaInSeconds, String T) {

            runOnUiThread(() -> {
                //checkNetworkState = isConnected(MyDownloadService.this);
                //Log.d("태그", "networkState : " + networkState + ", checkNetworkState = " + checkNetworkState);
                        /*if(networkState == 1 && checkNetworkState == 2) { // WIFI로 다운하다가 데이터로 변경했을 때.
                            Log.d("태그", "(MyDownloadService.java) 다운로드가 중지되었습니다.");
                            Toast.makeText(MyDownloadService.this, "다운로드가 중지되었습니다.", Toast.LENGTH_SHORT).show();

                            Log.d("태그", "networkState = " + networkState + ", checkNetworkState = " + checkNetworkState);
                            // setThreadInterruptAll();
                        }*/
                        mCallback.recvData(T, progress);
                        adapter.public_progress = (int) progress;
                        adapter.public_percent = String.valueOf(progress) + "%";

                    }
            );
        }
    };

    //콜백 인터페이스 선언
    public interface ICallback {
        public void recvData(String title, float a); //액티비티에서 선언한 콜백 함수.
        public void setIsDownloading(boolean alive); //액티비티에서 선언한 콜백 함수.
        public void downloadSuccess();
    }

    private ICallback mCallback;

    //액티비티에서 콜백 함수를 등록하기 위함.
    public void registerCallback(ICallback cb) {
        mCallback = cb;
    }

    // 서비스가 진행중인지 확인하여 진행중일때만 종료시키는 메서드
    public boolean isServiceRunningCheck() {
        ActivityManager manager = (ActivityManager) this.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.myDownload.example.MyDownloadService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    // 서비스 포그라운드를 종료시키는 메서드
    public void setStopForeground() {
        mDbOpenHelper.openR();
        ArrayList<Map<String, String>> downloadList = mDbOpenHelper.selectDownloadList();
        for (int i = 0; i < downloadList.size(); i++) {
            //Log.d("태그", downloadList.get(i).get("title"+i)+" 다운? : " + downloadList.get(i).get("isdownload"+i));
            //Log.d("태그", "state : " + downloadList.get(i).get("isdownload"+i));
            if(downloadList.get(i).get("isdownload"+i).equals("다운로드중..")) {
                //Log.d("태그", "서비스의 if문");
                // 현재 다운중인 파일이 존재한다면

                //stopForeground(true);
                //stopSelf();
                //mCallback.downloadSuccess();
                mDbOpenHelper.close();
                Log.d("태그", "아직 다운로드중.. return.");
                return;
            }
        }
        mDbOpenHelper.close();
        Log.d("태그", "stopForeground");
        stopForeground(true);
        /*if (!compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }*/
    }

    //액티비티에서 서비스 함수를 호출하기 위한 함수 생성
    public void myServiceFunc(String title, String selectQuality, String url, boolean disconnection_restart) {
        if(disconnection_restart) {
            //Log.d("태그", "(return!) disconnection_restart = " + disconnection_restart);
            return;
        }
        // networkState = isConnected(MyDownloadService.this); // 현재 네트워크 상태를 다운로드하기 전에 가져온다.

        test.add(0, disposable);
        this.title = title;
        this.selectQuality = selectQuality;
        this.url = url;
        LayoutInflater inflate = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = inflate.inflate(R.layout.activity_completed_download, null);


        CustomListView = (ListView) mView.findViewById(R.id.listView);
        customList = new ArrayList<>();
        adapter = complet.new CustomAdapter(MyDownloadService.this, customList);

        CustomListView.setAdapter(adapter);
        thread = new BackThread();
        thread.start();
    }

    public void setThreadInterrupt(String title) { // dispose() 호출시 포지션이 한칸씩 앞당겨짐
        //Log.d("태그", "(setThreadInterrupt) title = " + title);
        if(!compositeDisposable.isDisposed()) {
            compositeDisposable.remove(disposMap.get(title));
        }

        disposMap.remove(title);
    }

    public void setThreadInterruptAll() {
        mDbOpenHelper.openR();
        ArrayList<Map<String, String>> downloadList = mDbOpenHelper.selectDownloadList();
        mDbOpenHelper.close();
        mDbOpenHelper.openW();
        try {
            for (int i = 0; i < downloadList.size(); i++) {
                if (downloadList.get(i).get("isdownload" + i).equals("다운로드중..")) {
                    mDbOpenHelper.updateDownloadPause(downloadList.get(i).get("title"+i));
                    compositeDisposable.remove(disposMap.get(downloadList.get(i).get("title"+i)));
                }
            }
        } catch (Exception e) {
            Log.d("태그", "(MyDownloadService.java) Exception!!");
        } finally {
            mDbOpenHelper.close();
        }
    }

    public void setThreadRemove(String title) {
        disposMap.remove(title);
    }

    Handler handler = new Handler();
    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    public MyDownloadService() {
    }

    class LocalBinder extends Binder {
        MyDownloadService getService() {
            return MyDownloadService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d("태그", "onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("태그", "onStartCommand");
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("다운로드중...")
                .setSmallIcon(R.mipmap.app_icon)
                .setContentIntent(pendingIntent).build();
        startForeground(2, notification);

        return START_STICKY;
    }

    private void createNotificationChannel() {
        Log.d("태그", "createNotificationChannel");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel =
                    new NotificationChannel( CHANNEL_ID, "MP3/Video 추출",
                            NotificationManager.IMPORTANCE_NONE );
            serviceChannel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            assert manager != null;
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public void fileSetDate() {

        File file = new File(path);
        File[] childFileList = file.listFiles();
        for(File childFile : childFileList) {
            if(childFile.toString().substring(0,
                    childFile.toString().lastIndexOf(".")).equals(path + FilterTitle)) {
                file = childFile;
                break;
            }
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Calendar cal = Calendar.getInstance();
        Date date = new Date();
        cal.setTime(date);
        String modDate = dateFormat.format(cal.getTime());
        try {
            date = dateFormat.parse(modDate);
            file.setLastModified(date.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        galleryAddPic(file.toString());
    }

    private void galleryAddPic(String currentPhotoPath) {
        //Log.d("태그", "Wls path = " + currentPhotoPath);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);

        /*File f = new File(path);
        File[] childFileList = f.listFiles();
        for(File childFile : childFileList) {
            if(childFile.toString().substring(0,
                    childFile.toString().lastIndexOf(".")).equals(path + FilterTitle)) {
                f = childFile;
                break;
            }
        }*/

        //새로고침할 사진경로 (폴더 X)
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);

        //Log.d("태그", "미디어 스캐닝을 실행합니다.");

    }

    private class BackThread extends Thread {
        @Override
        public void run() {
            Log.d("태그", "BackThread.run()");
            YoutubeDLRequest request = new YoutubeDLRequest(url);
            File youtubeDLDir = getDownloadLocation();

            FilterTitle = title;
            FilterTitle = FilterTitle.replace("/", "_");
            FilterTitle = FilterTitle.replace(":", "_");
            FilterTitle = FilterTitle.replace("'", "\'");
            FilterTitle = FilterTitle.replace("|", "l");
            FilterTitle = FilterTitle.replace("?", "_");
            FilterTitle = FilterTitle.replaceAll("\\\"", "\'");
            Log.d("태그", "(BackThread) FilterTitle = " + FilterTitle);
            request.addOption("-o", youtubeDLDir.getAbsolutePath() + "/"+FilterTitle+".%(ext)s");
            //request.addOption("--extract-audio", "--audio-format mp3");
            request.addOption("-f", selectQuality);

            /*
            Twi 를 다운받게 하려면 이런식으로 다운해야 받아지는거같다.
            request.addOption("-f", "bestvideo[height<=360]+bestaudio/best[height<=360]");
            */

            //Example: --metadata-from-title  "%(artist)s - %(title)s"

            downloading = true;

            try {
                Log.d("태그", "Observable.fromCallable....!");
                disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, callback, title))
                            //.subscribeOn(Schedulers.io())
                            .subscribeOn(Schedulers.newThread()) // 얘가 없으면 메인스레드로 돌려서 화면이 멈춤
                            .observeOn(AndroidSchedulers.mainThread()) // 얘가 없으면 백키누르면 스레드가 멈춤
                            .subscribe(youtubeDLResponse -> {
                                tvDownloadStatus = "다운로드됨";
                                Toast.makeText(MyDownloadService.this, "다운로드를 완료하였습니다", Toast.LENGTH_SHORT).show();
                                //Log.d("태그", "Download Success...!");
                                fileSetDate(); // 파일 생성날짜 갱신

                                setStopForeground(); // 다운이 모두 종료됐다면 포그라운드 제거

                            }, e -> {
                                tvDownloadStatus = "다운로드 실패";
                                //Log.d("태그", "결과창 : " + e.getMessage());
                                //Log.d("태그", "Download Failed...!");
                                //downloading = false;
                                //mCallback.setIsDownloading(false);
                            });
                //Log.d("태그", "disposMap 객체에 저장된 키값 : " + title);
                disposMap.put(title, disposable);
                compositeDisposable.add(disposMap.get(title));
            } catch (Exception e) {
                //Log.d("태그", "인터럽트 익셉션에 걸림..");
            }
        }
    }

    @NonNull
    private File getDownloadLocation() { // 다운로드로 저장되는 Path 주소
        File youtubeDLDir = new File(Environment.getExternalStorageDirectory()+"/Movies/", "Download");
        if (!youtubeDLDir.exists()) youtubeDLDir.mkdir();
        return youtubeDLDir;
    }
/*
    public int isConnected(Context context) {
        cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        ni = cm.getActiveNetworkInfo();
        if( ni != null && ni.isConnected()) {
            if(ni.getType() == ConnectivityManager.TYPE_WIFI) {
                // WIFI인 경우
                return 1;
            } else if (ni.getType() == ConnectivityManager.TYPE_MOBILE) {
                // 3g, 4g 인 경우
                return 2;
            }
        }
        // 연결 안됨
        return 0;
    }
*/
    @Override
    public void onDestroy() {
        //Log.d("태그", "서비스가 종료됩니다...");
        super.onDestroy();
        if (!compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
    }
}
