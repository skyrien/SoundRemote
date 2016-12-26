package com.skyrien.soundremote;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends WearableActivity {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private ImageButton mImageButton1;
    private ImageButton mImageButton2;
    private ImageButton mImageButton3;
    private TextView mText1;
    private TextView mText2;
    private TextView mText3;
    private TextView mTitleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        // For V1, we'll include 3 variable sounds, though the layout is hardcoded.
        // For V2, we'll try to genericize this into a ListView
        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mImageButton1 = (ImageButton) findViewById(R.id.sound1_img);
        mImageButton2 = (ImageButton) findViewById(R.id.sound2_img);
        mImageButton3 = (ImageButton) findViewById(R.id.sound3_img);
        mText1 = (TextView) findViewById(R.id.sound1_txt);
        mText2 = (TextView) findViewById(R.id.sound2_txt);
        mText3 = (TextView) findViewById(R.id.sound3_txt);
        mTitleText = (TextView) findViewById(R.id.titleText);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mText1.setTextColor(getResources().getColor(android.R.color.white));
            mText2.setTextColor(getResources().getColor(android.R.color.white));
            mText3.setTextColor(getResources().getColor(android.R.color.white));

        } else {
            mContainerView.setBackground(null);
            mText1.setTextColor(getResources().getColor(android.R.color.black));
            mText2.setTextColor(getResources().getColor(android.R.color.black));
            mText3.setTextColor(getResources().getColor(android.R.color.black));
        }
    }
}
