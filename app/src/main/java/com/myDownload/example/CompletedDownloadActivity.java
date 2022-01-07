package com.myDownload.example;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.myDownload.youtubedl_android.DownloadProgressCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.disposables.CompositeDisposable;

public class CompletedDownloadActivity extends AppCompatActivity {
    private int progressBar;
    private ListView CustomListView;
    static private CustomAdapter adapter;
    static private ArrayList<CustomView> customList; // 이건 지금까지의 모든 다운로드 리스트를 저장해두는곳 (DB 연동)
    static private ArrayList<CustomView> downloadWaitingList = new ArrayList<CustomView>(); // 여긴 다운로드중일떄 추가 다운로드를 임시로 저장해두는곳
    private ImageView imageView;
    static private Bitmap mBitmap = null;
    private String thumbnail_URL;
    private String title;
    private String fileSize;
    private String url;
    private String selectQuality;
    private String tvDownloadStatus = "다운로드중..";
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private DbOpenHelper mDbOpenHelper = new DbOpenHelper(CompletedDownloadActivity.this);
    private App isDownloading;
    private boolean restart = false;
    private boolean disconnection_restart = false;
    private String restartTitle;
    private String restartSelectQuality;
    private String restartUrl;
    private String videoPlayTitle; // 비디오 플레이를 원하는 비디오의 이름
    private ImageView back_image;
    private ImageView delete_image;
    //private AdView mAdView;
    private String path = Environment.getExternalStorageDirectory().toString() + "/Movies/Download/";
    boolean checkPersent = true;
    private String deleteCheckTitle = "";

    private MyDownloadService myService = null;
    private static boolean isService = false; // 서비스 중인 확인용

    private ConnectivityManager cm;
    private NetworkInfo ni;
    private int networkState;

    final ServiceConnection conn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            // 서비스와 연결되었을 때 호출되는 메서드
            // 서비스 객체를 전역변수로 저장
            MyDownloadService.LocalBinder mb = (MyDownloadService.LocalBinder) service;
            //Log.d("태그", "mb.getService() = " + mb.getService());
            myService = mb.getService(); // 서비스받아옴

            if(myService != null){ // 비동기 메서드라 언제 받아올지 몰라서.. 받아오면 처리하도록 구현.
                myService.registerCallback(mCallback);
                isService = true;

                //액티비티에서 서비스 함수 호출
                //myService.myServiceFunc(downloadWaitingList.get(0).getTitle(), selectQuality, url, thumbnail_URL);
                //myService.myServiceFunc(title, selectQuality, url/* thumbnail_URL,*/, 0, false);
                if (!restart) { // 일반적인 다운로드 호출이라면.
                    //Log.d("태그", "restart = " + restart + ", title = " + title);
                    Log.d("태그", "다운로드 요청 시작. (myServiceFunc 호출!)");

                    myService.myServiceFunc(title, selectQuality, url, disconnection_restart);
                } else { // 재시작 요청했다면..
                    // Log.d("태그", "restart..  , restartTitle = " + restartTitle);
                    myService.myServiceFunc(restartTitle, restartSelectQuality, restartUrl, disconnection_restart);
                    restart = !restart;
                    if (disconnection_restart) {
                        //Log.d("태그", "아직도 null인가..? " + myService);
                        //Log.d("태그", "setThreadInterrupted를 시도한 restartTitle = " + restartTitle);
                        myService.setThreadInterrupt(restartTitle);
                        disconnection_restart = !disconnection_restart;
                    }
                }

                //do whatever you want to do after successful binding
                //closeConnection();
            }
            // 서비스쪽 객체를 전달받을수 있음
        }

        public void onServiceDisconnected(ComponentName name) {
            // 서비스와 연결이 끊겼을 때 호출되는 메서드
            //Log.d("태그", "conn 객체 서비스 연결 해제...");
            /*Toast.makeText(getApplicationContext(),
                    "서비스 연결 해제",
                    Toast.LENGTH_LONG).show();*/
            isService = false;
            myService = null;
        }
    };

    //서비스에서 아래의 콜백 함수를 호출하며, 콜백 함수에서는 액티비티에서 처리할 내용 입력
    private MyDownloadService.ICallback mCallback = new MyDownloadService.ICallback() {
        public void recvData(String T, float progress) {
            mDbOpenHelper.openW();
            //처리할 일들..
            runOnUiThread(() -> {
                        mDbOpenHelper.updateProgress(T, (int) progress);

                        if ((int) progress / 50 == 1 && checkPersent) {
                            //myService.getThreadIsAlive();
                            checkPersent = false;
                        }
                        if ((int) progress == 100) {
                            try {
                                adapter.downloadStatus = "다운로드됨";
                                adapter.notifyDataSetChanged();
                                CustomListView.setAdapter(adapter);
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else if ((int) progress != 100 && "다운로드됨".equals(adapter.downloadStatus)) {
                            //adapter.downloadStatus = "다운로드중...";
                            adapter.notifyDataSetChanged();
                            CustomListView.setAdapter(adapter);
                        }

                    }
            );
            adapter.notifyDataSetChanged();
            CustomListView.setAdapter(adapter);
            mDbOpenHelper.close();

        }

        public void setIsDownloading(boolean alive) {

        }
        /////////////////////////////////////////////////////////////// 중요함 ------------------------
        public void downloadSuccess() { // 하나의 다운로드가 완료되었을때 아직 남은 다운로드 리스트가 있는지 확인하는곳
            closeConnection(); // unbindService(conn);
        }

    };

    //서비스 시작.
    public void startServiceMethod(){
        Intent Service = new Intent(this, MyDownloadService.class);
        bindService(Service, conn, Context.BIND_AUTO_CREATE);
    }

    public void closeConnection() {
        if(isService) {
            //Log.d("태그", "해제됨... unbindService...");
            unbindService(conn);


            isService = false;
        }
    }

    private DownloadProgressCallback callback = new DownloadProgressCallback() {
        @Override
        public void onProgressUpdate(float progress, long etaInSeconds, String T) {
            runOnUiThread(() -> {
                adapter.public_progress = (int) progress;
                adapter.public_percent = String.valueOf(progress) + "%";

                CustomListView.setAdapter(adapter);
                        if ((int) progress / 50 == 1 && checkPersent) {

                            checkPersent = false;
                        }

                    }
            );
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_download);

        isDownloading = ((App)getApplicationContext());

        Intent intent = getIntent();
        boolean downloadRequest = intent.getBooleanExtra("downloadRequest", false);

        back_image = (ImageView) findViewById(R.id.backspace);
        delete_image = (ImageView) findViewById(R.id.delete_image);
        CustomListView = (ListView) findViewById(R.id.listView);
        customList = new ArrayList<CustomView>(); // 이건 지금까지의 모든 다운로드 리스트를 저장해두는곳 (DB 연동)

        adapter = new CustomAdapter(CompletedDownloadActivity.this, customList);
/*
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                //Log.d("태그", "onAdLoaded");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                // Code to be executed when an ad request fails.
                //Log.d("태그", "onAdFailedToLoad " + adError);
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }

            @Override
            public void onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }
        });*/

        // 다운로드를 요청한것인지, 아니면 그냥 다운목록을 누른것인지 확인하기 위한 조건문
        if (!downloadRequest) { // 실제 다운로드를 요청했음
            thumbnail_URL = intent.getStringExtra("thumbnail");
            title = intent.getStringExtra("title");
            //Log.d("태그", "getIntent title = " + title);
            fileSize = intent.getStringExtra("fileSize");
            url = intent.getStringExtra("videoURL");
            selectQuality = intent.getStringExtra("selectQuality");

            Log.d("태그", "title="+title+", selectQuality+"+selectQuality+", url="+url);

            new LoadImage().execute(thumbnail_URL);
            CustomListView.setAdapter(adapter);
        } else { // 다운로드 요청이 아닌 목록만 클릭함
            openDownload();
            //Log.d("태그", "서비스 실행중인가?? : " + isServiceRunningCheck());
            //Intent intent_1 = new Intent(getApplicationContext(), MyDownloadService.class);
            //Log.d("태그", "다운로드 목록 확인..");
        }

        back_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        delete_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(CompletedDownloadActivity.this)
                        .setTitle("다운기록 삭제하기")
                        .setMessage("정말로 삭제하시겠습니까?\n\n※모든 다운기록이 삭제됩니다.\n실제 파일은 삭제되지 않으며 \n현재 다운중인 파일은 중단됩니다.")
                        .setPositiveButton("예", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 다운기록 모두 삭제 후 서비스 종료
                                customList.clear();
                                downloadWaitingList.clear();
                                mDbOpenHelper.openW();
                                mDbOpenHelper.update();
                                mDbOpenHelper.close();
                                if(myService != null) {
                                    closeConnection();
                                }
                                adapter.notifyDataSetChanged();
                                CustomListView.setAdapter(adapter);
                                Toast.makeText(isDownloading, "다운기록을 삭제했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
            }
        });

        CustomListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int a;
                mDbOpenHelper.openR();
                Map<String, String> map = mDbOpenHelper.selectProgress();
                mDbOpenHelper.close();
                String isPause = map.get("isdownload"+position);

                if (isPause.equals("일시정지")) { // 일시정지라면
                    a = R.array.vuswlq_play;
                } else if (isPause.equals("다운로드중..")) { // 재생중이라면
                    a = R.array.vuswlq_pause;
                } else {
                    a = R.array.vuswlq_success;
                }
                if (isPause.equals("일시정지") | isPause.equals("다운로드중..")) {
                    new AlertDialog.Builder(CompletedDownloadActivity.this)
                            .setTitle("편집")
                            .setItems(a, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which==0) { // 일시정지 | 재생하기
                                        if (isPause.equals("일시정지")) { // 일시정지라면 재생하자
                                            networkState = isConnected(CompletedDownloadActivity.this);

                                            if(networkState == 0) { // 인터넷이 끊겨있다면
                                                Toast.makeText(CompletedDownloadActivity.this, "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT).show();
                                            } else if (networkState == 2) { // 데이터로 연결되어있다면
                                                new AlertDialog.Builder(CompletedDownloadActivity.this)
                                                        .setTitle("모바일 데이터")
                                                        .setMessage("현재 모바일 데이터가 켜져있습니다. \n데이터를 사용하여 다운로드를 하시겠습니까?")
                                                        .setPositiveButton("예", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                FileDownload();
                                                                mDbOpenHelper.openW();
                                                                mDbOpenHelper.updateProgress(customList.get(position).getTitle(), Integer.parseInt(map.get("progress"+position)));
                                                                mDbOpenHelper.close();
                                                                adapter.notifyDataSetChanged();
                                                                CustomListView.setAdapter(adapter);

                                                                restart = true;
                                                                restartTitle = customList.get(position).getTitle();
                                                                restartSelectQuality = customList.get(position).getSelectQuality();
                                                                restartUrl = customList.get(position).getUrl();
                                                                //Log.d("태그", "재시작 시도 Title = " + restartTitle);
                                                                if(myService != null) {
                                                                    //Log.d("태그", "myService = " + myService);
                                                                    closeConnection();
                                                                }
                                                                startConnection(); // 이거 안되면 closeConnection 호출
                                                            }
                                                        })
                                                        .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {

                                                            }
                                                        }).show();
                                            } else {
                                                mDbOpenHelper.openW();
                                                mDbOpenHelper.updateProgress(customList.get(position).getTitle(), Integer.parseInt(map.get("progress"+position)));
                                                mDbOpenHelper.close();
                                                adapter.notifyDataSetChanged();
                                                CustomListView.setAdapter(adapter);

                                                restart = true;
                                                restartTitle = customList.get(position).getTitle();
                                                restartSelectQuality = customList.get(position).getSelectQuality();
                                                restartUrl = customList.get(position).getUrl();
                                                //Log.d("태그", "재시작 시도 Title = " + restartTitle);
                                                if(myService != null) {
                                                    //Log.d("태그", "myService = " + myService);
                                                    closeConnection();
                                                }
                                                startConnection(); // 이거 안되면 closeConnection 호출
                                            }

                                        } else if(isPause.equals("다운로드중..")) { // 재생중이라면 일시정지하자
                                            mDbOpenHelper.openW();
                                            mDbOpenHelper.updateDownloadPause(customList.get(position).getTitle());
                                            mDbOpenHelper.close();
                                            adapter.notifyDataSetChanged();
                                            CustomListView.setAdapter(adapter);
                                            if (/*!isService*/ myService == null) {
                                                //Log.d("태그", "재접속 문구를 띄운 후, 재연결 시도...");

                                                restart = true;
                                                disconnection_restart = true;
                                                restartTitle = customList.get(position).getTitle();
                                                restartSelectQuality = customList.get(position).getSelectQuality();
                                                restartUrl = customList.get(position).getUrl();

                                                if(myService != null) {
                                                    //Log.d("태그", "myService != null -> closeConnection!");
                                                    closeConnection();
                                                }
                                                startConnection(); // 이거 안되면 closeConnection 호출해보셈

                                                //Log.d("태그", "다시 인터럽트 시도.");

                                                if(myService != null) {
                                                    //myService.setThreadInterrupt(customList.get(position).getTitle());
                                                } else {
                                                    //Log.d("태그", "다시 인터럽트를 시도하던 중 myService 값을 아직 받아오지 못했습니다.");
                                                }

                                            } else {
                                                myService.setThreadInterrupt(customList.get(position).getTitle());
                                            }
                                        }  else {
                                            Toast.makeText(isDownloading, "파일이 잘못되었습니다.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    else if (which==1) { // 다운로드 도중에 삭제하기
                                        new AlertDialog.Builder(CompletedDownloadActivity.this)
                                                .setTitle("다운기록 삭제하기")
                                                .setMessage("정말로 삭제하시겠습니까?\n\n※실제 해당 파일이 삭제됩니다.\n다운로드 기록만 삭제되기 원한다면 \n휴지통을 눌러주세요")
                                                .setPositiveButton("예", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        String FilterTitle = customList.get(position).getTitle();
                                                        FilterTitle = FilterTitle.replace("/", "_");
                                                        FilterTitle = FilterTitle.replace(":", "_");
                                                        FilterTitle = FilterTitle.replace("'", "\'");
                                                        FilterTitle = FilterTitle.replace("|", "l");
                                                        FilterTitle = FilterTitle.replace("?", "_");
                                                        FilterTitle = FilterTitle.replaceAll("\\\"", "\'");

                                                        File file = new File(path);
                                                        File[] childFileList = file.listFiles();
                                                        File deleteFile = null; // 실제 삭제될 파일 변수
                                                        int deleteFileSize_Front = 0;
                                                        int deleteFileSize_Back = 0;
                                                        int deleteFileSize_Test_Front = 0; // 삭제될 파일명으로 적합한지 타이틀 앞부분을 검사하는 변수
                                                        int deleteFileSize_Test_Back = 0; // 삭제될 파일명으로 적합한지 타이틀 뒷부분을 검사하는 변수

                                                        for(File childFile : childFileList) {
                                                            if(childFile.toString().substring(
                                                                    childFile.toString().lastIndexOf(".") + 1,
                                                                    childFile.toString().length()).equals("part")
                                                                    && childFile.toString().contains(FilterTitle)) {
                                                                // 다운로드중에 삭제를 요청한 경우 확장자가 .part이면서 원하는 제목명인 파일이 존재한다면.
                                                                deleteFileSize_Test_Front = childFile.toString().indexOf(FilterTitle);
                                                                // 동일한 이름이 포함되는데 이름 앞부분에 별도로 제목을 추가했다면 Front 변수에서 검열됨
                                                                deleteFileSize_Test_Back = childFile.toString().lastIndexOf(".") - deleteFileSize_Test_Front;
                                                                // 동일한 이름이 포함되는데 이름 뒷부분에 별도로 제목을 추가했다면 Back 변수에서 검열됨

                                                                if (deleteFileSize_Front == 0 || deleteFileSize_Back == 0 ||
                                                                        deleteFileSize_Front >= deleteFileSize_Test_Front && deleteFileSize_Back >= deleteFileSize_Test_Back) {
                                                                    // 어차피 무조건 파일이 두개 이상이기 때문에
                                                                    // 포함되는 제목 앞부분에 제목을 더 붙였다면 마주치는 인덱스가 원본보다 점점 늘어나기 때문에 더 일찍 만나는 인덱스가 원본이다.
                                                                    // 그리고 뒷부분에 제목을 더 붙였다면 확장자를 마주치기까지 더 멀기때문에 뺄셈을 한 값이 더 적은쪽이 원본이다.

                                                                    deleteFileSize_Front = deleteFileSize_Test_Front;
                                                                    deleteFileSize_Back = deleteFileSize_Test_Back;
                                                                    deleteFile = childFile;
                                                                    // 그 파일을 삭제 예정 변수에 담도록 한다.
                                                                }
                                                            }
                                                        }
                                                        if(deleteFile != null) {
                                                            FileDelete(deleteFile.toString());
                                                        }

                                                        mDbOpenHelper.openW();
                                                        try {
                                                            mDbOpenHelper.deleteDownload(customList.get(position).getTitle());
                                                        } catch (SQLException e) { }

                                                        mDbOpenHelper.close();
                                                        deleteCheckTitle = customList.get(position).getTitle();
                                                        Log.d("태그", "customList.size() = " + customList.size());
                                                        Log.d("태그", "FilterTitle = " + FilterTitle);
                                                        Log.d("태그", "deleteCheckTitle = " + deleteCheckTitle);
                                                        if (customList.get(position).getTitle().contains(deleteCheckTitle)) {
                                                            //Log.d("태그", customList.get(position).getTitle() + " 은 이미 삭제.");

                                                            customList.remove(position);
                                                        }
                                                        if (downloadWaitingList.size() > position) {
                                                            //Log.d("태그", "(Completed) Line. 467");
                                                            //Log.d("태그", "downloadWaitingList.get(position).getTitle() = " + downloadWaitingList.get(position).getTitle());
                                                            //Log.d("태그", "customList.size() = " + customList.size());

                                                            downloadWaitingList.remove(position); // 이거는 원래 아래 if문 안에 있었다. 어떻게 있었는지는 모름.
                                                            /*if(downloadWaitingList.get(position).getTitle().contains(customList.get(position).getTitle())) {

                                                            }*/
                                                        } else {

                                                        }

                                                        adapter.notifyDataSetChanged();
                                                        CustomListView.setAdapter(adapter);
                                                    }
                                                })
                                                .setNegativeButton("아니오",
                                                        new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {

                                                            }
                                                        }).show();
                                    }
                                }
                            })
                            .show();
                } else if (isPause.equals("다운로드됨")){
                    String fileName = customList.get(position).getTitle();
                    //Log.d("태그", "DB에서 삭제할 fileName = " + fileName);
                    new AlertDialog.Builder(CompletedDownloadActivity.this)
                            .setTitle("파일 삭제하기")
                            .setMessage("정말로 삭제하시겠습니까?\n\n※실제 해당 파일이 삭제됩니다.\n다운로드 기록만 삭제되기 원한다면 \n휴지통을 눌러주세요")
                            .setPositiveButton("예", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Log.d("태그", "삭제를 원함");
                                    String FilterTitle = customList.get(position).getTitle();
                                    FilterTitle = FilterTitle.replace("/", "_");
                                    FilterTitle = FilterTitle.replace(":", "_");
                                    FilterTitle = FilterTitle.replace("'", "\'");
                                    FilterTitle = FilterTitle.replace("|", "l");
                                    FilterTitle = FilterTitle.replace("?", "_");
                                    FilterTitle = FilterTitle.replaceAll("\\\"", "\'");

                                    File deleteFile = null; // 실제 삭제될 파일 변수
                                    File file = new File(path);
                                    File[] childFileList = file.listFiles();
                                    for(File childFile : childFileList) {
                                        if(childFile.toString().substring(0,
                                                childFile.toString().lastIndexOf(".")).equals(path+FilterTitle)) {
                                            //Log.d("태그", "동일한 파일 발견 : " + childFile.toString());
                                            deleteFile = childFile;
                                            break;
                                        }
                                    }
                                    if (deleteFile != null) {
                                        FileDelete(deleteFile.toString());
                                    } else {
                                        Toast.makeText(isDownloading, "파일 삭제에 실패했거나 이미 삭제하였습니다.", Toast.LENGTH_SHORT).show();
                                    }

                                    mDbOpenHelper.openW();
                                    try {
                                        mDbOpenHelper.deleteDownload(customList.get(position).getTitle());
                                    } catch (SQLException e) { }

                                    mDbOpenHelper.close();
                                    Log.d("태그", "FilterTitle = " + FilterTitle);
                                    Log.d("태그", "deleteCheckTitle = " + deleteCheckTitle);
                                    //////////////////////////////////////////////////////////////
                                    /*if (customList.get(position).getTitle().contains(customList.get(position).getTitle())) {
                                        customList.remove(position);
                                    }

                                    if (downloadWaitingList.size() > position) {
                                        if(downloadWaitingList.get(position).getTitle().contains(customList.get(position).getTitle())) {
                                            downloadWaitingList.remove(position);
                                        }
                                    } else {

                                    }*/
                                    //////////////////////////////////////////////////////////////
                                    deleteCheckTitle = customList.get(position).getTitle();
                                    if (customList.get(position).getTitle().contains(deleteCheckTitle)) {
                                        customList.remove(position);
                                    }

                                    if (downloadWaitingList.size() > position) {
                                        if(downloadWaitingList.get(position).getTitle().contains(deleteCheckTitle)) {
                                            downloadWaitingList.remove(position);
                                        }
                                    } else {

                                    }
                                    adapter.notifyDataSetChanged();
                                    CustomListView.setAdapter(adapter);
                                }
                            })
                            .setNegativeButton("아니오",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    }).show();
                }
                // return true;
            }
        });
    }

    public void FileDownload() {

    }

    public void FileDelete(String strFileName) {
        try {

            File file = new File(strFileName);
            if (file.isFile()) {
                file.delete();
                Toast.makeText(getApplicationContext(),"파일을 정상적으로 삭제하였습니다.",Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(isDownloading, "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e){
            Toast.makeText(getApplicationContext(),"파일 삭제하는데 실패하였습니다.",Toast.LENGTH_SHORT).show();
        }
    }


    public boolean isServiceRunningCheck() {
        ActivityManager manager = (ActivityManager) this.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ((com.myDownload.example.MyDownloadService.class.getName()).equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void startConnection() {
        //Log.d("태그", "startConnection()");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("태그", "(startConnection)");
            Intent intent = new Intent(CompletedDownloadActivity.this, MyDownloadService.class); // getApplicationContext()
            intent.putExtra("title", title);
            intent.putExtra("selectQuality", selectQuality);
            intent.putExtra("url", url);
            intent.putExtra("thumbnail_URL", thumbnail_URL);


            bindService(intent, conn, Context.BIND_AUTO_CREATE);
            startForegroundService(intent); // 21.07.18 binService가 이것보다 위에 있었는데 위치를 변경함.

        } else {
            Intent intent = new Intent(getApplicationContext(), MyDownloadService.class);
            startService(intent);
        }
    }

    private class LoadImage extends AsyncTask<String, String, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... args) {
            try {
                mBitmap = BitmapFactory
                                    .decodeStream((InputStream) new URL(args[0])
                                            .getContent());
            } catch (Exception e) {
                //("태그", "이미지 다운로드 실패");

                //mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.white);
                return mBitmap;
            }
            return mBitmap;
        }

        protected void onPostExecute(Bitmap image) {
            if (image != null) {
                CustomView customView = new CustomView();
                customView.setmBitmap(image);

                downloadWaitingList.add(0, new CustomView(image, title, 0, "0%",fileSize, tvDownloadStatus, url, selectQuality, false));

                ByteArrayOutputStream stream = new ByteArrayOutputStream(); // 여기서부터
                mBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); // 여기랑
                byte[] thumb = stream.toByteArray(); // 여기까지 이미지뷰의 비트맵을 DB로 넣기위해 byte로 변환

                mDbOpenHelper.openW();
                /*Log.d("태그", "update() 생성");
                mDbOpenHelper.update();*/
                mDbOpenHelper.create();
                try {
                    mDbOpenHelper.insertDownload(title, thumb, "0", tvDownloadStatus, fileSize, url, selectQuality);
                } catch (Exception e) {
                    Log.d("태그", "insert Error! : " + e.getMessage());
                }

                mDbOpenHelper.close();

                openDownload();

                startConnection(); //////// 꺼냈음
            } else {
                //Log.d("태그", "이미지가 존재하지 않습니다.");
            }
        }
    }
    ///////// 프로그레스바를 실시간으로 바꿔줘야해서... 그냥 어댑터 내부에 스레드를 담아두기위해
    // 어댑터를 여기로 빼내고 어댑터 내부에는 holder?? 를 써주라는데 전에 했던 프로젝트 참조나 구글링

    public class CustomAdapter extends BaseAdapter {

        private Context context;
        private List<CustomView> customViewList;
        public String downloadStatus = "다운로드중...";
        public int public_progress = 0;
        public String public_percent;

        public CustomAdapter(Context context, List<CustomView> customViewList) {
            this.context = context;
            this.customViewList = customViewList;
        }

        public class ViewHolder {
            public ImageView thumbnail;
            public TextView title;
            public int int_progress = public_progress;
            public ProgressBar progressBar;
            public String string_persent = public_percent;
            public TextView progressPersent;
            public TextView fileSize;
            public Bitmap mBitmap;
            public TextView tvDownloadStatus;
            public String holder_downloadStatus = downloadStatus;
        }

        @Override
        public int getCount() {
            return customViewList.size();
        }

        @Override
        public Object getItem(int i) {
            return customViewList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            ViewHolder holder = null;
            if (view == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                view = vi.inflate(R.layout.custom_view, null);
                //final TextView work = (TextView) view.findViewById(R.id.titl);
                holder = new ViewHolder();
                holder.thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
                holder.title = (TextView) view.findViewById(R.id.title);
                holder.progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
                holder.progressPersent = (TextView) view.findViewById(R.id.progress_percent);
                holder.fileSize = (TextView) view.findViewById(R.id.fileSize);
                holder.tvDownloadStatus = (TextView) view.findViewById(R.id.downloadStatus);

                view.setTag(holder);


            } else {
                holder = (ViewHolder) view.getTag();
            }

            CustomView country = customList.get(i);

            mDbOpenHelper.openR();
            Map<String, String> map = mDbOpenHelper.selectProgress();
            mDbOpenHelper.close();

            holder.thumbnail.setImageBitmap(country.getmBitmap());
            holder.title.setText(country.getTitle()+"  ");
            holder.progressBar.setProgress(Integer.parseInt(map.get("progress"+i)));
            holder.progressPersent.setText(map.get("progress"+i) + "%");
            holder.fileSize.setText(country.getFileSize());
            holder.tvDownloadStatus.setText(map.get("isdownload"+i));
            holder.title.setTag(country);
            return view;
        }
    }

    public void openDownload() {
        mDbOpenHelper.openR();
        ArrayList<Map<String, byte[]>> downloadBitmap = mDbOpenHelper.selectDownloadBitmap();
        ArrayList<Map<String, String>> downloadList = mDbOpenHelper.selectDownloadList();
        for (int i = 0; i < downloadList.size(); i++) {
            byte[] img = downloadBitmap.get(i).get("thumbnail"+i);
            BitmapFactory.decodeByteArray(img, 0, img.length);

            customList.add(0, new CustomView(BitmapFactory.decodeByteArray(img, 0, img.length),
                    downloadList.get(i).get("title"+i), Integer.parseInt(downloadList.get(i).get("progress"+i)),
                    downloadList.get(i).get("progress"+i)+"%", downloadList.get(i).get("filesize"+i),
                    downloadList.get(i).get("isdownload"+i), downloadList.get(i).get("videourl"+i),
                    downloadList.get(i).get("quality"+i), false));

        }
        mDbOpenHelper.close();

        adapter = new CustomAdapter(CompletedDownloadActivity.this, customList);
        CustomListView.setAdapter(adapter);
    }

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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        if(mAdView != null) {
//            mAdView.pause();
//        }
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        if(mAdView != null) {
//            mAdView.pause();
//        }
        if (myService != null) {
            if(myService.isServiceRunningCheck()) { // 액티비티를 종료했는데도 서비스가 살아있다면
                //myService.setStopForeground(); // 다운로드중인 파일이 하나도 없다면 포그라운드를 그냥 꺼버려라
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if(mAdView != null) {
//            mAdView.resume();
//        }
        if (myService != null) {
            if(myService.isServiceRunningCheck()) { // 액티비티를 종료했는데도 서비스가 살아있다면
                //myService.setStopForeground(); // 다운로드중인 파일이 하나도 없다면 포그라운드를 그냥 꺼버려라
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (myService != null) {
            Log.d("태그", "myService.isServiceRunningCheck() = " + myService.isServiceRunningCheck());
            if(myService.isServiceRunningCheck()) { // 액티비티를 종료했는데도 서비스가 살아있다면
                myService.setStopForeground(); // 다운로드중인 파일이 하나도 없다면 포그라운드를 그냥 꺼버려라
            }
        }
//        if(mAdView != null) {
//            mAdView.pause();
//        }
        isService = false;
        myService = null;
    }

}