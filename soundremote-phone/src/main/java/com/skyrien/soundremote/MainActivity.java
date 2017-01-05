package com.skyrien.soundremote;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";



    private TextView mTitle;
    private TextView mSound1Txt;
    private TextView mSound2Txt;
    private TextView mSound3Txt;
    private TextView mSound1Path;
    private TextView mSound2Path;
    private TextView mSound3Path;
    private ImageButton mSound1Button;
    private ImageButton mSound2Button;
    private ImageButton mSound3Button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate() called");

        // Initialize stuff
        initUiFields();

        // Start listener service -- how do we know it's started?
        startService(new Intent(this, DataLayerListenerService.class));

        // Setting hardware volume controls for music streaming control
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

    }


    private void initUiFields() {

        // Setup App Resources here
        mTitle = (TextView) findViewById(R.id.title_txt);
        mSound1Txt = (TextView) findViewById(R.id.sound1_txt);
        mSound2Txt = (TextView) findViewById(R.id.sound2_txt);
        mSound3Txt = (TextView) findViewById(R.id.sound3_txt);
        mSound1Path = (TextView) findViewById(R.id.sound1_path);
        mSound2Path = (TextView) findViewById(R.id.sound2_path);
        mSound3Path = (TextView) findViewById(R.id.sound3_path);
        mSound1Button = (ImageButton) findViewById(R.id.sound1_img);
        mSound2Button = (ImageButton) findViewById(R.id.sound2_img);
        mSound3Button = (ImageButton) findViewById(R.id.sound3_img);

        // Set up the buttons so they play
        mSound1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Sound button 1 pressed");
                //playSoundId(soundId1);
            }
        });
        mSound2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Sound button 2 pressed");
                //playSoundId(soundId2);
            }
        });
        mSound3Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Sound button 3 pressed");
                //playSoundId(soundId3);
            }
        });
    }







}
