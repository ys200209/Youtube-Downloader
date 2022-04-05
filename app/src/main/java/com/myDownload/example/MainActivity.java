package com.myDownload.example;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final int REQUEST_PERMISSION = 1;
    private ImageView saveButton;
    private Button btnDownloadingExample;
    //private Button twitchDownloadingButton;
    private boolean updating = false;
    private static final String TAG = "MainActivity";
    //private AdView mAdView;
    static private boolean checkPermission = false;
    // private ImageView imageView;
    private DbOpenHelper mDbOpenHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDbOpenHelper = new DbOpenHelper(MainActivity.this);
        mDbOpenHelper.openW();
        mDbOpenHelper.create();
        // mDbOpenHelper.update();
        mDbOpenHelper.close();

        initViews();
        initListeners();
    }

    private void initListeners() {
        saveButton.setOnClickListener(this);
        btnDownloadingExample.setOnClickListener(this);
        // imageView.setOnClickListener(this);
        //twitchDownloadingButton.setOnClickListener(this);
    }

    private void initViews() {
        saveButton = findViewById(R.id.saveButton);
        // imageView = findViewById(R.id.search_button);
        btnDownloadingExample = findViewById(R.id.btn_downloading_example);
        //twitchDownloadingButton = findViewById(R.id.twitch);
    }

    @Override
    public void onClick(View v) {
            switch (v.getId()) {
                case R.id.saveButton: { // 내 저장목록
                    if(isStoragePermissionGranted()) { // 권한을 획득했다면
                        Intent i = new Intent(MainActivity.this, CompletedDownloadActivity.class);
                        i.putExtra("downloadRequest", true);
                        startActivity(i);
                        break;
                    }
                }
                case R.id.btn_downloading_example: { // 다운로드
                    //Intent i = new Intent(MainActivity.this, DownloadingExampleActivity.class);
                    Intent i = new Intent(MainActivity.this, SearchUrlActivity.class);
                    startActivity(i);
                    break;
                /*case R.id.search_button: {
                    Intent i = new Intent(MainActivity.this, SearchUrlActivity.class);
                    startActivity(i);
                    break;
                }*/
                /**/
                }

            /*case R.id.twitch: { // 트위치 다운로드
                Intent i = new Intent(MainActivity.this, SearchUrlActivity.class);
                i.putExtra("isTwitch", true);
                startActivity(i);
            }*/
            }
    }

    public boolean isStoragePermissionGranted() {
        int writeCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        String[] params = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
        int permissionCode = 200;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (writeCheck != PackageManager.PERMISSION_GRANTED && readCheck != PackageManager.PERMISSION_GRANTED) {
                //Log.d("태그", "권한이 승인되지 않았습니다.");
                new AlertDialog.Builder(MainActivity.this)
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

    @Override
    protected void onPause() {
        super.onPause();

//        if(mAdView != null) {
//            mAdView.pause();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();

//        if(mAdView != null) {
//            mAdView.resume();
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mDbOpenHelper.isOpened()) {
            mDbOpenHelper.close();
        }
//        if(mAdView != null) {
//            mAdView.destroy();
//        }
    }

}