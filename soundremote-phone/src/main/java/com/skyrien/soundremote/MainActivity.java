package com.skyrien.soundremote;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Setup App References here
    private SoundPool soundPool;
    private int soundId1, soundId2, soundId3;
    boolean plays = false, loaded = false;
    float actVolume, maxVolume, volume;
    AudioManager audioManager;
    int counter;

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
                playSoundId(soundId1);
            }
        });
        mSound2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Sound button 2 pressed");
                playSoundId(soundId2);
            }
        });
        mSound3Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Sound button 3 pressed");
                playSoundId(soundId3);
            }
        });

        // AudioManager stuff
        audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        actVolume = (float)audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        maxVolume = (float)audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume = actVolume / maxVolume;
        Log.d(TAG, "AudioManager created with vol: " + volume);


        // Setting hardware volume controls for music streaming control
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        counter = 0;

        // Creating AudioAttributes so we can create a SoundPool
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        // Loading sounds
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                loaded = true;
                Log.d(TAG, "SoundPool Created");

            }
        });


        // For some reason, this method doesn't work well... let's try hardcoded res paths:
/*
        soundId1 = soundPool.load(getString(R.string.sound1_path), 1);
        Log.d(TAG, "Loaded Sound 1 from path: " + getString(R.string.sound1_path));

        soundId2 = soundPool.load(getString(R.string.sound2_path), 1);
        Log.d(TAG, "Loaded Sound 2 from path: " + getString(R.string.sound2_path));

        soundId3 = soundPool.load(getString(R.string.sound3_path), 1);
        Log.d(TAG, "Loaded Sound 3 from path: " + getString(R.string.sound3_path));
*/
        soundId1 = soundPool.load(this, R.raw.sample1, 1);
        Log.d(TAG, "Loaded Sound 1");
        soundId2 = soundPool.load(this, R.raw.sample2, 1);
        Log.d(TAG, "Loaded Sound 2");
        soundId3 = soundPool.load(this, R.raw.sample3, 1);
        Log.d(TAG, "Loaded Sound 3");

    }

    public void playSoundId(int soundId) {

        soundPool.play(soundId, volume, volume, 1, 0, 1f);

    }

}
