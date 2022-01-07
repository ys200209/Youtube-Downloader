package com.myDownload.example;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SearchUrlActivity extends AppCompatActivity {
    private ImageView backspace;
    private EditText urlText;
    private ImageView searchButton;
    private InputMethodManager imm;
    private boolean isTwitch;
    // private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_url);

        backspace = (ImageView) findViewById(R.id.backspace);
        urlText = (EditText) findViewById(R.id.urlText);
        searchButton = (ImageView) findViewById(R.id.search_button);

        Intent intent = getIntent();
        isTwitch = intent.getBooleanExtra("isTwitch", false);

        // 키보드 보이기
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
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

        backspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imm.isActive()) {
                    imm.hideSoftInputFromWindow(urlText.getWindowToken(), 0); // 숨기기 처리
                }
                /*Intent intent = new Intent(SearchUrlActivity.this, MainActivity.class);
                SearchUrlActivity.this.startActivity(intent);*/
                finish();
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imm.isActive()) {
                    imm.hideSoftInputFromWindow(urlText.getWindowToken(), 0); // 숨기기 처리
                }
                String url = urlText.getText().toString();

                if (url == null || url.trim().isEmpty()) { // 비어있거나 null 값을 주소창에 넣었다면
                    Toast.makeText(SearchUrlActivity.this, "유튜브 주소를 입력해주세요", Toast.LENGTH_SHORT).show();
                    urlText.setText("");
                } else {
                    urlText.setText("");
                    Intent intent = new Intent(SearchUrlActivity.this, DownloadingExampleActivity.class);
                    intent.putExtra("url", url);
                    intent.putExtra("isTwitch", isTwitch);
                    SearchUrlActivity.this.startActivity(intent);
                }
            }
        });

        urlText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if(actionId == EditorInfo.IME_ACTION_SEARCH){ // IME_ACTION_SEARCH , IME_ACTION_GO
                    imm.hideSoftInputFromWindow(urlText.getWindowToken(), 0); // 숨기기 처리
                    String url = urlText.getText().toString();

                    if (url == null || url.trim().isEmpty()) { // 비어있거나 null 값을 주소창에 넣었다면
                        Toast.makeText(SearchUrlActivity.this, "유튜브 주소를 입력해주세요", Toast.LENGTH_SHORT).show();
                        urlText.setText("");
                    } else {
                        urlText.setText("");
                        Intent intent = new Intent(SearchUrlActivity.this, DownloadingExampleActivity.class);
                        intent.putExtra("url", url);
                        SearchUrlActivity.this.startActivity(intent);
                    }
                }
                return false;
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
//        if(mAdView != null) {
//            mAdView.pause();
//        }
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(urlText.getWindowToken(), 0); // 숨기기 처리
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if(mAdView != null) {
//            mAdView.resume();
//        }
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(urlText.getWindowToken(), 0); // 숨기기 처리
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        if(mAdView != null) {
//            mAdView.destroy();
//        }
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(urlText.getWindowToken(), 0); // 숨기기 처리
        }
    }

}