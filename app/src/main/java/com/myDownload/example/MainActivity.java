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
                case R.id.saveButton: { // ??? ????????????
                    if(isStoragePermissionGranted()) { // ????????? ???????????????
                        Intent i = new Intent(MainActivity.this, CompletedDownloadActivity.class);
                        i.putExtra("downloadRequest", true);
                        startActivity(i);
                        break;
                    }
                }
                case R.id.btn_downloading_example: { // ????????????
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

            /*case R.id.twitch: { // ????????? ????????????
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
                //Log.d("??????", "????????? ???????????? ???????????????.");
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("?????? ??????")
                        .setMessage("????????? ?????? ???????????? ???????????? ??????\n?????? ????????? ???????????????.")
                        .setPositiveButton("??????", new DialogInterface.OnClickListener() {
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
                    if (!checkPermission) { // ????????? ???????????????
                        checkPermission = !checkPermission;
                    }
                } else {
                    //Toast.makeText(this, "????????? ?????????????????????.", Toast.LENGTH_SHORT).show();
                    AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
                    localBuilder.setTitle("?????? ??????")
                            .setMessage("????????? ??????????????? ?????? \n?????? -> ???????????? -> ?????? ????????????.")
                            .setPositiveButton("?????? ??????", new DialogInterface.OnClickListener(){
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
                            .setNegativeButton("??????", new DialogInterface.OnClickListener() {
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
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mDbOpenHelper.isOpened()) {
            mDbOpenHelper.close();
        }
    }

}