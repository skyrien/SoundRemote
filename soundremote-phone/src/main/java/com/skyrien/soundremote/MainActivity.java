package com.skyrien.soundremote;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String SETTINGS = "SoundRemote";
    private static final int READ_REQUEST_CODE = 42;
    private GoogleApiClient mGoogleApiClient;
    private String playerNodeId;

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
        initGoogleApiClient();

        SharedPreferences settings = getSharedPreferences(SETTINGS,0);

        // Let's update UI fields from sharedPrefs
        mSound1Path.setText(settings.getString("sound1Path","0"));
        mSound2Path.setText(settings.getString("sound2Path","0"));
        mSound3Path.setText(settings.getString("sound3Path","0"));


        // Let's check and request for permissions here
    if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                0);

    }

        // Start listener service
        if (!isMyServiceRunning(DataLayerListenerService.class))
        {
            Log.d(TAG, "Service isn't started so, Starting DataLayerListenerService");
            startService(new Intent(this, DataLayerListenerService.class));
        }
        // Setting hardware volume controls for music streaming control
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);


    }

    // loadFilePicker() provides a pickable index
    public void loadFilePicker(int requestCode) {
        Log.d(TAG,"loadFilePicker() called");

        // Currently, we're going to use the Ringtone manager to give us media URI to work with
        // but later on, this should be a generic file picker.
        //
        // Though... maybe... we should make it easy to use ringtones?

        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        //intent.addCategory(Intent.CATEGORY_OPENABLE);
        //intent.setType("audio/*");

        Log.d(TAG,"starting activity...");
        startActivityForResult(intent, requestCode);
        Log.d(TAG,"Moving on...");
    }


    // This is the activity result for the ringtone picker... i think?
    // Here's what we need to do -- we need to take the new file, update the local SharedPreferences, and reload
    // the new files in the data listener
    // Note: The requestCode basically is which individual sound was picked (1, 2, or 3).
    // The resultCode is the response from the Activity

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.d(TAG, "onActivityResult() called");

        if(resultCode == Activity.RESULT_OK) {
            String selectedSoundNum = "sound" + String.valueOf(requestCode) + "Path";

            if (resultData != null)
            {
                Log.d(TAG, "Request Code: " + String.valueOf(resultCode) +
                        " - Result OK: Fetching data, writing to SharedPrefs and toasting!");

                // This saves the URI of the returned ringtone to SharedPreferences
                Uri inputUri = resultData.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                Log.d(TAG, "Found Uri: " + inputUri.toString());

                // The value of 'selectedSoundNum' is "sound" + requestCode + Path; ex: sound1Path
                Log.d(TAG, "Writing to SharedPreferences: " + selectedSoundNum);
                SharedPreferences settings = getSharedPreferences(SETTINGS,0);
                SharedPreferences.Editor spEditor = settings.edit();
                spEditor.putString(selectedSoundNum,inputUri.toString());
                spEditor.commit();
                Toast.makeText(MainActivity.this, inputUri.toString(), Toast.LENGTH_SHORT).show();

                // Let's update UI fields from sharedPrefs
                mSound1Path.setText(settings.getString("sound1Path","0"));
                mSound2Path.setText(settings.getString("sound2Path","0"));
                mSound3Path.setText(settings.getString("sound3Path","0"));

                // We're overloading playRemoteSound 0 to trigger a reload of audio on the
                // DataLayerListenerService
                playRemoteSound(0);
            }
            else Log.d(TAG, "Result data null!");

        }

    }//onActivityResult

    // borrowed from
    // http://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-on-android
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        Log.d(TAG, "isMyServiceRunning() called");
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    private void initUiFields() {
        Log.d(TAG, "initUiFields() called");

        // Setup App Resources here
        mTitle = (TextView) findViewById(R.id.title_txt);

        // These are the text on the right side
        mSound1Txt = (TextView) findViewById(R.id.sound1_txt);
        mSound2Txt = (TextView) findViewById(R.id.sound2_txt);
        mSound3Txt = (TextView) findViewById(R.id.sound3_txt);
        mSound1Path = (TextView) findViewById(R.id.sound1_path);
        mSound2Path = (TextView) findViewById(R.id.sound2_path);
        mSound3Path = (TextView) findViewById(R.id.sound3_path);

        // This is the play button
        mSound1Button = (ImageButton) findViewById(R.id.sound1_img);
        mSound2Button = (ImageButton) findViewById(R.id.sound2_img);
        mSound3Button = (ImageButton) findViewById(R.id.sound3_img);

        // LOAD FROM PREFS
        // this should load the path names from preferences file and update the local views
        // note that the audio service is also loading from the same preferences file







        // Setting up listeners on buttons so they play
        mSound1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Sound button 1 pressed");
                playRemoteSound(1);
            }
        });
        mSound2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Sound button 2 pressed");
                playRemoteSound(2);
            }
        });
        mSound3Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Sound button 3 pressed");
                playRemoteSound(3);
            }
        });

        // Setting up listeners on path names so we can trigger a picker
        mSound1Path.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Sound path 1 pressed");

                // opens file picker...
                loadFilePicker(1);

                // The following code assumes its only run after the picker has set a value
                // Basically, every time we want an updated value of the soundpaths, we need to pull
                // from sharedpreferences.

                // We should probably assume a zero-init state, where if empty, we'll just load the
                // default value
                Log.d(TAG,"Getting updated picker value for Sound 1");
                //SharedPreferences settings = getPreferences(0);
                //mSound1Path.setText(settings.getString("sound1Path", getString(R.string.sound1_path)));
            }
        });
        mSound2Path.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Sound path 2 pressed");
                loadFilePicker(2);
            }
        });
        mSound3Path.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Sound path 3 pressed");
                loadFilePicker(3);
            }
        });
    }

    // SECTION 4:
    // SUPPORT UTILITY FUNCTIONS
    //

    private void initGoogleApiClient() {
        Log.d(TAG, "Called initGoogleApiClient()");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        setupSoundremoteplayer();
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();
        Log.d(TAG, "Connecting to GoogleApiClient...");
        mGoogleApiClient.connect();
    }

    // This method sets up the target node (the local node)
    public void setupSoundremoteplayer() {
        Log.d(TAG, "setupSoundremotePlayer() called");

        // Bypasses lots of note searching since we already know we only need the local node
        Wearable.NodeApi.getLocalNode(mGoogleApiClient).setResultCallback(
                new ResultCallback<NodeApi.GetLocalNodeResult>() {
                    @Override
                    public void onResult(@NonNull NodeApi.GetLocalNodeResult getLocalNodeResult) {
                        playerNodeId = getLocalNodeResult.getNode().getId();
                        Log.d(TAG, "onResult() called and found local node: " + playerNodeId);
                    }
                }
        );
    }

// SECTION 2:
    // THIS IS ACTIVITY LOGIC
    //

    // This function(will be) is called when a button is pressed for a particular sound.
    private void playRemoteSound(final int soundId) {
        Log.d(TAG, "playRemoteSound() called for sound: " + String.valueOf(soundId));
        if (playerNodeId != null) {
            //Log.d(TAG, "Found: " + playerNodeId);

            // We use a single byte as the payload, which includes the SoundId
            final byte[] message = new byte[1];
            message[0] = ((byte) soundId);
            Wearable.MessageApi.sendMessage(mGoogleApiClient, playerNodeId,
                    "soundremoteplayer", message).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.d(TAG, "message FAILED to send");
                            }
                            else Log.d(TAG, "Message sent: " + message[0]);
                        }
                    }
            );
        }
        else { // no player found
            Toast.makeText(MainActivity.this, "No player found!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "BOO! No SoundRemotePlayer found.");
        }
    }



}
