package com.myDownload.example;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.graphics.Color;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.devbrackets.android.exomedia.BuildConfig;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.myDownload.youtubedl_android.YoutubeDL;
import com.myDownload.youtubedl_android.YoutubeDLRequest;
import com.myDownload.youtubedl_android.mapper.VideoFormat;
import com.myDownload.youtubedl_android.mapper.VideoThumbnail;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class DownloadingExampleActivity extends AppCompatActivity implements View.OnClickListener {
    private final int REQUEST_PERMISSION = 1;
    private Button btnStartDownload;
    //private TextView tvDownloadStatus;
    private ProgressBar pbLoading;
    private VideoView videoView;
    private ArrayAdapter adapter;
    private TextView title; // TextView 그 자체 공간
    private String str_title = ""; // TextView 에 들어갈 타이틀 문자열
    private TextView uploader;
    private TextView line;
    private Map<String,String> qualityMap = new HashMap<>();
    private Map<String,Long> sizeMap = new HashMap<>();
    private String selectQuality = "";
    private EditText editTitle; // 제목 EditText에 들어가는 타이틀
    private String thumbnail;
    private String urlText; // 검색한 URL을 저장해두는곳
    private StringBuilder sb;
    private boolean isTwitch;
    private ImageView backspace;
    private String spinnerSize;
    private DbOpenHelper mDbOpenHelper = new DbOpenHelper(DownloadingExampleActivity.this);
    // private AdView mAdView;

    static private boolean checkPermission = false;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private ImageView saveButton;
    private String isDownload = "";
    private String path = Environment.getExternalStorageDirectory().toString() + "/Movies/Download/";
    private ConnectivityManager cm;
    private NetworkInfo ni;
    private int networkState;
    private Long audioSize;
    private Spinner spinner;
    private Dialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloading_example);

        //checkPermission = isStoragePermissionGranted();

        Intent intent = getIntent();
        urlText = intent.getStringExtra("url");
        isTwitch = intent.getBooleanExtra("isTwitch", false);

        if(isStoragePermissionGranted()) { // 권한을 획득했다면

        }

        initViews();
        initListeners();
        startStream(urlText);

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
    }

    private void initViews() {
        saveButton = findViewById(R.id.saveButton);
        btnStartDownload = findViewById(R.id.btn_start_download);
        //tvDownloadStatus = findViewById(R.id.tv_status);
        pbLoading = findViewById(R.id.pb_status);
        videoView = findViewById(R.id.video_view);
        title = findViewById(R.id.title);
        uploader = findViewById(R.id.uploader);
        line = findViewById(R.id.line);
        backspace = findViewById(R.id.backspace);

    }

    private void initListeners() {
        saveButton.setOnClickListener(this);
        btnStartDownload.setOnClickListener(this);
        videoView.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared() {
                // 준비완료시 자동실행
                // videoView.start();
                btnStartDownload.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onClick(View v) {
        if(isStoragePermissionGranted()) { // 권한을 획득했다면
            switch (v.getId()) {
                case R.id.saveButton: { // 내 저장목록
                    Intent i = new Intent(DownloadingExampleActivity.this, CompletedDownloadActivity.class);
                    i.putExtra("downloadRequest", true);
                    startActivity(i);
                    break;
                }
                case R.id.btn_start_download: {
                    dialog = new Dialog(this);
                    dialog.setContentView(R.layout.custom_alertdialog);

                    spinner = (Spinner) dialog.findViewById(R.id.formatSpinner);
                    editTitle = (EditText) dialog.findViewById(R.id.editTitle);
                    final TextView saveButton = (TextView) dialog.findViewById(R.id.saveButton);
                    editTitle.setText(str_title);

                    ArrayList<String> arrayList = new ArrayList<>();
                    audioSize = sizeMap.get("tiny");
                    if (!isTwitch) { // 유튜브 다운로드
                    /*String[] existsArr = {"144p", "240p", "360p", "480p", "720p" ,"720p60", "1080p"
                                        , "1080p60", "1440p", "1440p60", "2160p", "2160p60"};*/
                        String[] existsArr = {"2160p60", "2160p", "1440p60", "1440p", "1080p60", "1080p"
                                , "720p60", "720p", "480p", "360p", "240p", "144p"};

                        arrayList.add("mp3");
                        boolean highQuality = true;

                        for (int i = 0; i < existsArr.length; i++) {
                            if (qualityMap.containsKey(existsArr[i]) && sizeMap.containsKey(existsArr[i])) {
                                if (existsArr[i].substring(existsArr[i].length() - 2, existsArr[i].length()).equals("60") && highQuality) {
                                    // 고화질이 존재한다면 ( 60프레임 )
                                    Long videoSize = sizeMap.get(existsArr[i]);
                                    String size = "";

                                    if (videoSize != 0 && audioSize != null) {
                                        // videoSize와 audioSize를 불러오지 못하는 경우엔 값을 아예 주지말고 존재할때만 이렇게 주자.
                                        size = Long.toString(videoSize + audioSize);
                                        size = size.substring(0, size.length() - 4);
                                        sb = new StringBuilder(size);
                                        sb.insert(size.length() - 2, ".");
                                        if (sb.indexOf(".") == 0) {
                                            sb.insert(0, "0");
                                        }
                                    } else {
                                        sb = new StringBuilder("?");
                                    }
                                    arrayList.add(existsArr[i] + "  -  ( " + sb.toString() + " MB )");
                                    highQuality = false;

                                } else if (existsArr[i].substring(existsArr[i].length() - 1, existsArr[i].length()).equals("p") && !highQuality) {
                                    // 60 프레임을 만나고도 30프레임이 또 존재한다면 그것을 무시하고 고화질만 띄워라.
                                    highQuality = true;
                                    continue;
                                } else if ((!qualityMap.containsKey("2160p") || !qualityMap.containsKey("1440p")
                                        || !qualityMap.containsKey("1080p") || !qualityMap.containsKey("720p")) && !highQuality) {
                                    // 고화질을 만나서 bool값을 변경했음에도 일반 프레임을 만나지 못했을때.
                                    highQuality = true;
                                    continue;
                                } else {
                                    // 고화질이 아니라면 그냥 상관없이 넣음
                                    Long videoSize = sizeMap.get(existsArr[i]);
                                    String size = "";

                                    if (videoSize != 0 && audioSize != null) {
                                        // videoSize와 audioSize를 불러오지 못하는 경우엔 값을 아예 주지말고 존재할때만 이렇게 주자.
                                        size = Long.toString(videoSize + audioSize);
                                        size = size.substring(0, size.length() - 4);
                                        sb = new StringBuilder(size);
                                        sb.insert(size.length() - 2, ".");
                                        //Log.d("태그", "sb = " + sb+ ", sb.indexOf(\".\") = " + sb.indexOf("."));
                                        if (sb.indexOf(".") == 0) {
                                            sb.insert(0, "0");
                                        }
                                    } else {
                                        sb = new StringBuilder("?");
                                    }

                                    arrayList.add(existsArr[i] + "  -  ( " + sb.toString() + " MB )");
                                }
                            } else {
                                highQuality = true;
                            }
                        }
                    } /*else { // 트위터 전용 다운로드라면
                        arrayList.add("1080p60");
                        arrayList.add("720p60");
                        arrayList.add("480p");
                        arrayList.add("360p");
                    }*/

                    adapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item,
                            arrayList);
                    spinner.setAdapter(adapter);

                    dialog.show(); // 다이얼로그 보여주기

                    // 다이얼로그 사이즈 조절

                    Display display = getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);

                    WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                    lp.copyFrom(dialog.getWindow().getAttributes());
                    //lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
                    lp.width = (int) (size.x * 1.0f);
                    //lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                    lp.height = (int) (size.x * 1.2f);
                    Window window = dialog.getWindow();
                    window.setAttributes(lp);

                    saveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            // if ("".equals(editTitle.getText().toString())) {
                            if (editTitle.getText().toString() == null || editTitle.getText().toString().trim().isEmpty()) {
                                // 제목에 아무것도 안넣었으면 돌려보냄
                                Toast.makeText(DownloadingExampleActivity.this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                return;
                            }

                            FileDownload(); // WIFI든 데이터든 그냥 다운로드 해버리도록.

                            /*  // 네트워크 상태를 가져와 현재 연결된 네트워크가 WIFI인지, 데이터인지 체크

                            networkState = isConnected(DownloadingExampleActivity.this);
                            // 이곳에서 네트워크 상태를 체크하여 데이터라면 다이얼로그 출력
                            if (networkState == 0) {
                                Toast.makeText(DownloadingExampleActivity.this, "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT).show();
                            } else if (networkState == 2) {
                                new AlertDialog.Builder(DownloadingExampleActivity.this)
                                        .setTitle("모바일 데이터")
                                        .setMessage("현재 모바일 데이터가 켜져있습니다. \n데이터를 사용하여 다운로드를 하시겠습니까?")
                                        .setPositiveButton("예", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                FileDownload();
                                            }
                                        })
                                        .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        }).show();
                            } else {
                                FileDownload();
                            }
                            */
                        }
                    });
                    break;
                }

            }
        }

        backspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public void FileDownload() {
        File existsFile = null;

        if (!isTwitch) {
            int existsFileLength = path.length() + editTitle.getText().toString().length();

            String FilterTitle = editTitle.getText().toString();
            FilterTitle = FilterTitle.replace("/", "_");
            FilterTitle = FilterTitle.replace(":", "_");
            FilterTitle = FilterTitle.replace("'", "\'");
            FilterTitle = FilterTitle.replace("|", "l");
            FilterTitle = FilterTitle.replace("?", "_");
            FilterTitle = FilterTitle.replaceAll("\\\"", "\'");

            File file = new File(path);

            File[] childFileList = file.listFiles();
            if (file.exists()) {
                for (File childFile : childFileList) {
                    if (childFile.toString().length() > existsFileLength) {
                        if (childFile.toString().substring(0,
                                childFile.toString().lastIndexOf(".")).equals(path + FilterTitle)) {
                            //Log.d("태그", "동일한 파일 발견 : " + childFile.toString());
                            //Log.d("태그", "비교 대상 파일명 : " + path + FilterTitle);
                            // 이 파일이 다운로드중인지 완료된건지 확인하기위해..
                            if (childFile.toString().length() - (path + FilterTitle).length() < 6) {
                                isDownload = "다운로드됨";
                            } else {
                                isDownload = "다운로드 안됨";
                            }
                            existsFile = childFile;
                            break;
                        }
                    }
                }
            }

            if (existsFile == null) { // 파일을 처음 다운한다면
                selectQuality = spinner.getSelectedItem().toString();
                String fileSize;
                if (selectQuality.equals("mp3")) {
                    selectQuality = "bestaudio[ext=m4a]";
                    StringBuilder sb;
                    if (audioSize != null) {
                        fileSize = Long.toString(audioSize);
                        fileSize = fileSize.substring(0, fileSize.length() - 4);
                        sb = new StringBuilder(fileSize);
                        sb.insert(fileSize.length() - 2, ".");
                        if (sb.indexOf(".") == 0) {
                            sb.insert(0, "0");
                        }
                    } else {
                        sb = new StringBuilder("?");
                    }

                    fileSize = sb.toString() + " MB";
                } else {
                    selectQuality = selectQuality.substring(0, selectQuality.indexOf("p"));
                    selectQuality = "bestvideo[height<=" + selectQuality + "]+bestaudio/best[height<=" + selectQuality + "]";

                    if (!isTwitch) {
                        spinnerSize = spinner.getSelectedItem().toString();
                        fileSize = spinnerSize.substring(spinnerSize.indexOf("(") + 1, spinnerSize.indexOf(")"));
                    } else {
                        fileSize = "";
                    }
                    //Log.d("태그", "중간과정 fileSize = " + fileSize);
                    //selectQuality = "[height=?"+selectQuality+"][fps<=30]";
                }
                //Log.d("태그", "selectQuality = " + selectQuality);
                dialog.dismiss();

                Intent intent = new Intent(DownloadingExampleActivity.this, CompletedDownloadActivity.class);
                intent.putExtra("thumbnail", thumbnail);
                intent.putExtra("title", editTitle.getText().toString());
                intent.putExtra("fileSize", fileSize);
                intent.putExtra("videoURL", urlText);
                intent.putExtra("selectQuality", selectQuality);
                DownloadingExampleActivity.this.startActivity(intent);
            } else { // 파일이 이미 존재한다면
                File finalExistsFile = existsFile;
                String finalFilterTitle = FilterTitle;
                new AlertDialog.Builder(DownloadingExampleActivity.this)
                        .setTitle("덮어쓰기")
                        .setMessage("이미 같은 이름의 파일이 존재합니다.\n덮어쓰시겠습니까?")
                        .setPositiveButton("덮어쓰기", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (isDownload.equals("다운로드됨")) {
                                    if (finalExistsFile != null) {
                                        FileDelete(finalExistsFile.toString());
                                    } else {
                                        Toast.makeText(DownloadingExampleActivity.this, "파일이 삭제되지 못했습니다.", Toast.LENGTH_SHORT).show();
                                    }

                                    mDbOpenHelper.openW();
                                    try {
                                        int result = mDbOpenHelper.deleteDownload(editTitle.getText().toString());
                                        Log.d("태그", "mDbOpenHelper.deleteDownload(" + editTitle.getText().toString() + ") = " + result);
                                    } catch (SQLException e) {
                                        Log.d("태그", "(DownloadingExample) Exception!!! 1.");
                                    }
                                    mDbOpenHelper.close();
                                } else { // 다운로드중 혹은 일시정지 상태일때 삭제하는 방식
                                    File file = new File(path);
                                    File[] childFileList = file.listFiles();
                                    File deleteFile = null; // 실제 삭제될 파일 변수
                                    int deleteFileSize_Front = 0;
                                    int deleteFileSize_Back = 0;
                                    int deleteFileSize_Test_Front = 0; // 삭제될 파일명으로 적합한지 타이틀 앞부분을 검사하는 변수
                                    int deleteFileSize_Test_Back = 0; // 삭제될 파일명으로 적합한지 타이틀 뒷부분을 검사하는 변수

                                    for (File childFile : childFileList) {
                                        //Log.d("태그", "모든 파일명 : " + childFile.toString());
                                        if (childFile.toString().length() > existsFileLength) {
                                            if (childFile.toString().substring(
                                                    childFile.toString().lastIndexOf(".") + 1,
                                                    childFile.toString().length()).equals("part")
                                                    && childFile.toString().substring(0,
                                                    existsFileLength).equals(path + finalFilterTitle)) {
                                                // 다운로드중에 삭제를 요청한 경우 확장자가 .part이면서 원하는 제목명인 파일이 존재한다면.
                                                //Log.d("태그", "다운로드중에 삭제를 원하는 파일명 : " + path + finalFilterTitle);
                                                deleteFileSize_Test_Front = childFile.toString().indexOf(finalFilterTitle);
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

                                    }
                                    if (deleteFile != null) {
                                        FileDelete(deleteFile.toString());
                                    } else {
                                        Toast.makeText(DownloadingExampleActivity.this, "삭제할 파일을 찾지 못했습니다.", Toast.LENGTH_SHORT).show();
                                    }
                                    mDbOpenHelper.openW();
                                    try {
                                        mDbOpenHelper.deleteDownload(editTitle.getText().toString());
                                    } catch (SQLException e) {
                                    }

                                    mDbOpenHelper.close();
                                }
                                selectQuality = spinner.getSelectedItem().toString();
                                String fileSize;
                                if (selectQuality.equals("mp3")) {
                                    selectQuality = "bestaudio[ext=m4a]";
                                    StringBuilder sb;
                                    if (audioSize != null) {
                                        fileSize = Long.toString(audioSize);
                                        fileSize = fileSize.substring(0, fileSize.length() - 4);
                                        sb = new StringBuilder(fileSize);
                                        sb.insert(fileSize.length() - 2, ".");
                                        if (sb.indexOf(".") == 0) {
                                            sb.insert(0, "0");
                                        }
                                    } else {
                                        sb = new StringBuilder("?");
                                    }
                                    fileSize = sb.toString() + " MB";
                                } else {
                                    selectQuality = selectQuality.substring(0, selectQuality.indexOf("p"));
                                    selectQuality = "bestvideo[height<=" + selectQuality + "]+bestaudio/best[height<=" + selectQuality + "]";


                                    if (!isTwitch) {
                                        spinnerSize = spinner.getSelectedItem().toString();
                                        fileSize = spinnerSize.substring(spinnerSize.indexOf("(") + 1, spinnerSize.indexOf(")"));
                                    } else {
                                        fileSize = "";
                                    }
                                }
                                dialog.dismiss();

                                Intent intent = new Intent(DownloadingExampleActivity.this, CompletedDownloadActivity.class);
                                intent.putExtra("thumbnail", thumbnail);
                                intent.putExtra("title", editTitle.getText().toString());
                                intent.putExtra("fileSize", fileSize);
                                intent.putExtra("videoURL", urlText);
                                intent.putExtra("selectQuality", selectQuality);
                                DownloadingExampleActivity.this.startActivity(intent);
                            }
                        })
                        .setNegativeButton("취소",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                }).show();
            }
        } else { // 유튜브가 아닌 다른 플랫폼에서의 다운로드를 원한다면
            selectQuality = spinner.getSelectedItem().toString();
            selectQuality = selectQuality.substring(0, selectQuality.indexOf("p"));
            selectQuality = "bestvideo[height<=" + selectQuality + "]+bestaudio/best[height<=" + selectQuality + "]";
            //fileSize = selectQuality.substring(selectQuality.indexOf("(")+1, selectQuality.indexOf(")"));

            //Log.d("태그", "selectQuality = " + selectQuality);
            dialog.dismiss();
            //startDownload();

            Intent intent = new Intent(DownloadingExampleActivity.this, CompletedDownloadActivity.class);
            intent.putExtra("thumbnail", thumbnail);
            intent.putExtra("title", editTitle.getText().toString());
            //Log.d("태그", "intent putExtra title : " + editTitle.getText().toString());
            intent.putExtra("fileSize", "-");
            //Log.d("태그", "intent putExtra fileSize = " + "-");
            intent.putExtra("videoURL", urlText);
            intent.putExtra("selectQuality", selectQuality);
            DownloadingExampleActivity.this.startActivity(intent);
        }
    }

    public void FileDelete(String strFileName) {
        try {

            File file = new File(strFileName);
            if (file.isFile()) {
                file.delete();
                Toast.makeText(getApplicationContext(),"파일을 정상적으로 삭제하였습니다.",Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(DownloadingExampleActivity.this, "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e){
            Toast.makeText(getApplicationContext(),"파일 삭제하는데 실패하였습니다.",Toast.LENGTH_SHORT).show();
        }
    }

    // 영상 재생하는 메서드
    private void startStream(String url) {
        url = url.trim();

        pbLoading.setVisibility(View.VISIBLE);
        String finalUrl = url;
        Disposable disposable = Observable.fromCallable(() -> {
            YoutubeDLRequest request = new YoutubeDLRequest(finalUrl);
            // best stream containing video+audio
            //request.addOption("-f", "bestvideo[height<=1080]+bestaudio/best[height<=1080]");
            request.addOption("-f", "best");
            return YoutubeDL.getInstance().getInfo(request); // VideoInfo 객체 반환받아옴
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(streamInfo -> {
                    pbLoading.setVisibility(View.GONE);
                    String videoUrl = streamInfo.getUrl();
                    str_title = streamInfo.getTitle();
                    ArrayList<VideoFormat> formats = streamInfo.getFormats();
                    ArrayList<VideoThumbnail> thumbnails = streamInfo.getThumbnails();
                    title.setText(str_title);
                    uploader.setText("- " + streamInfo.getUploader());
                    line.setBackgroundColor(Color.parseColor("#000000"));

                    thumbnail = thumbnails.get(0).getUrl();

                    ArrayList<String> checkQuality = new ArrayList<>();

                    for(int i = 0; i < formats.size(); i++) {
                        /*Log.d("태그", "formats("+i+") ? : " + formats.get(i).getFormat()
                        + " - " + formats.get(i).getFilesize());*/
                        //Log.d("태그", "getFps = " + formats.get(i).getFps());
                        //Log.d("태그", "getFormatId = " + formats.get(i).getFormatId());
                        //Log.d("태그", "getFormatNote = " + formats.get(i).getFormatNote());

                        if (qualityMap.containsKey(formats.get(i).getFormatNote()) && formats.get(i).getFilesize() != 0) {
                            // 중복된 값이 들어와 갱신을 해주어야 한다면
                            if (!formats.get(i).getFormatNote().equals(checkQuality.get(checkQuality.size()-1)) && i == formats.size() - 1) {
                                // 갱신을 해주려고 했지만 들어온 화질값이 최근에 저장된값이 아니라 뒷북치는 값이라면...
                                // ex) 1080화질까지 다 저장했는데 이제와서 360 화질을 저장하려고 하면 거절하기.
                                // (수정) 뒷북쳐도 그것으로 저장하는 현상이 발생해 맨 마지막값 뒷북만을 버리기로 수정해봄
                                //Log.d("태그", formats.get(i).getFormatNote() + "값은 이미 저장되고 뒷북입니다.");
                            } else {
                                // 그렇지 않고 현재 활발히 갱신중인 화질이라면 정상적으로 갱신해주기.
                                qualityMap.put(formats.get(i).getFormatNote(), formats.get(i).getFormatId());
                                sizeMap.put(formats.get(i).getFormatNote(), formats.get(i).getFilesize());
                            }
                            //sizeCheck = formats.get(i).getFilesize(); // 사이즈 저장
                            btnStartDownload.setVisibility(View.VISIBLE);
                        } else {
                            // 만약 존재하지 않는 새로운 키값이 들어온다면
                            if (!qualityMap.containsKey(formats.get(i).getFormatNote())) {
                                //sizeCheck = formats.get(i).getFilesize(); // 사이즈 저장
                                qualityMap.put(formats.get(i).getFormatNote(), formats.get(i).getFormatId());
                                sizeMap.put(formats.get(i).getFormatNote(), formats.get(i).getFilesize());
                                checkQuality.add(formats.get(i).getFormatNote());
                            }
                        }

                    }
                    btnStartDownload.setVisibility(View.VISIBLE);
                    if (isTwitch) { // 트위치 전용 다운로드라면
                        //Log.d("태그", "View.VISIBLE");
                        btnStartDownload.setVisibility(View.VISIBLE);
                    }
                    //Log.d("태그", "qualityMap = " + qualityMap);
                    //Log.d("태그", "sizeMap = " + sizeMap);
                    //qualityList.add(qualityMap);
                    if (TextUtils.isEmpty(videoUrl)) {
                        Toast.makeText(DownloadingExampleActivity.this, "주소가 존재하지 않습니다.", Toast.LENGTH_LONG).show();
                    } else {
                        setupVideoView(videoUrl);
                    }
                }, e -> {
                    if(BuildConfig.DEBUG) Log.e("태그",  "failed to get stream info", e);
                    pbLoading.setVisibility(View.GONE);
                    Log.d("태그", "Video Loading Error! : " + e.getMessage());
                    Toast.makeText(DownloadingExampleActivity.this, "동영상 불러오기에 실패했습니다.", Toast.LENGTH_LONG).show();
                });
        compositeDisposable.add(disposable);
    }

    private void setupVideoView(String videoUrl) {
        videoView.setVideoURI(Uri.parse(videoUrl));
        videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // mAdView.setVisibility(View.INVISIBLE);
                videoView.start();
            }
        });
    }

    public boolean isStoragePermissionGranted() {
        int writeCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        String[] params = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
        int permissionCode = 200;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (writeCheck != PackageManager.PERMISSION_GRANTED && readCheck != PackageManager.PERMISSION_GRANTED) {
                //Log.d("태그", "권한이 승인되지 않았습니다.");
                new AlertDialog.Builder(DownloadingExampleActivity.this)
                        .setTitle("권한 설정")
                        .setMessage("파일을 내부 저장소에 저장하기 위해\n접근 권한이 필요합니다.")
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    requestPermissions(params, REQUEST_PERMISSION);
                                }
                            }
                        })
                        .setCancelable(false)
                        .show();

                return false;
            } else {
                //ActivityCompat.requestPermissions(this, params, REQUEST_PERMISSION);
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    if (!checkPermission) { // 권한을 획득했다면
                        checkPermission = !checkPermission;
                    }
                } else {
                    //Toast.makeText(this, "권한이 거절되었습니다.", Toast.LENGTH_SHORT).show();
                    AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
                    localBuilder.setTitle("권한 설정")
                            .setMessage("원활한 다운로드를 위해 \n권한 -> 저장공간 -> 허용 해주세요.")
                            .setPositiveButton("권한 설정", new DialogInterface.OnClickListener(){
                                public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt){
                                    try {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                                .setData(Uri.parse("package:" + getPackageName()));
                                        startActivity(intent);
                                    } catch (ActivityNotFoundException e) {
                                        e.printStackTrace();
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                                        startActivity(intent);
                                    }
                                }})
                            .setNegativeButton("거절", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt) {

                                }})
                            .setCancelable(false)
                            .create()
                            .show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
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
    protected void onPause() {
        super.onPause();
        if (videoView.isPlaying()) {
            videoView.pause();
        }
//         if(mAdView != null) {
//             mAdView.pause();
//         }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView.isPlaying()) {
            videoView.pause();
        }
//        if(mAdView != null) {
//            mAdView.resume();
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
        if (videoView.isPlaying()) {
            videoView.pause();
        }
//        if(mAdView != null) {
//            mAdView.destroy();
//        }
    }
}