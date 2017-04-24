package com.skyrien.soundremote;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.Ringtone;
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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String SETTINGS = "SoundRemote";
    private static final int READ_REQUEST_CODE = 42;
    private GoogleApiClient mGoogleApiClient;
    private String playerNodeId;

    private TextView mSound1Txt;
    private TextView mSound2Txt;
    private TextView mSound3Txt;
    private TextView mSound1Path;
    private TextView mSound2Path;
    private TextView mSound3Path;
    private TextView mConnectionText;
    private ImageButton mSound1Button;
    private ImageButton mSound2Button;
    private ImageButton mSound3Button;
    private AdView mAdView;

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

        // Modifying these -- we want the path to now be the filename, while text is just the #
        mSound1Path.setText(settings.getString("sound1Txt",getString(R.string.sound1_path)));
        mSound2Path.setText(settings.getString("sound2Txt",getString(R.string.sound2_path)));
        mSound3Path.setText(settings.getString("sound3Txt",getString(R.string.sound3_path)));
        //mSound1Txt.setText(settings.getString("sound1Txt","Sound 1"));
        //mSound2Txt.setText(settings.getString("sound2Txt","Sound 2"));
        //mSound3Txt.setText(settings.getString("sound3Txt","Sound 3"));

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


        // Let's load ads last
        MobileAds.initialize(getApplicationContext(), getString(R.string.banner_ad_unit_id));
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    // loadFilePicker() provides a pickable index
    public void loadFilePicker(int requestCode) {
        Log.d(TAG,"loadFilePicker() called");

        // We're going to use the Ringtone manager to give us media URI to work with
        // but later on, this could be a generic file picker.
        //
        // Though... maybe... we should make it easy to use ringtones?

        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);

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
        Log.d(TAG, "onActivityResult() called for: " + String.valueOf(requestCode));

        if(resultCode == Activity.RESULT_OK) {
            String selectedSoundNum = "sound" + String.valueOf(requestCode) + "Path";
            String selectedSoundNumFile = "sound" + String.valueOf(requestCode) + "Txt";

            if (resultData != null)
            {
                Log.d(TAG, "Request Code: " + String.valueOf(requestCode) +
                        " - Result OK: Fetching data, writing to SharedPrefs and toasting!");

                // This saves the URI of the returned ringtone to SharedPreferences
                Uri inputUri = resultData
                        .getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

                // This is to protect against null breaking the app; also prevents silence
                // from being used
                if (inputUri == null) {
                    return;
                }

                // This is where we get the title of the ringtone from the system
                Ringtone ring = RingtoneManager.getRingtone(this,inputUri);
                Log.d(TAG, "Found file: " + ring.getTitle(this));

                // This portion writes the path and ringtone title to SharedPreferences
                Log.d(TAG, "Writing to SharedPreferences: " + selectedSoundNum);
                SharedPreferences settings = getSharedPreferences(SETTINGS,0);
                SharedPreferences.Editor spEditor = settings.edit();
                spEditor.putString(selectedSoundNum,inputUri.toString());
                spEditor.putString(selectedSoundNumFile,ring.getTitle(this));
                spEditor.commit();
                Toast.makeText(MainActivity.this, inputUri.toString(), Toast.LENGTH_SHORT).show();


                // Let's update UI fields from sharedPrefs
                String thePath;

                switch(requestCode) {

                    case 1:
                        thePath = settings.getString("sound1Path","0");
                        //mSound1Path.setText(thePath);
                        mSound1Path.setText(ring.getTitle(this));

                        playRemoteSound(-1);
                        break;

                    case 2:
                        thePath = settings.getString("sound2Path","0");
                        //mSound2Path.setText(thePath);
                        mSound2Path.setText(ring.getTitle(this));

                        playRemoteSound(-2);
                        break;

                    case 3:
                        thePath = settings.getString("sound3Path","0");
                        //mSound3Path.setText(thePath);
                        mSound3Path.setText(ring.getTitle(this));

                        playRemoteSound(-3);
                        break;
                }

                // Let's start the data item sync to the watch
                PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/soundtitles");
                putDataMapReq.getDataMap().putString("sound1Txt",
                        settings.getString("sound1Txt","Sound 1"));
                putDataMapReq.getDataMap().putString("sound2Txt",
                        settings.getString("sound2Txt","Sound 2"));
                putDataMapReq.getDataMap().putString("sound3Txt",
                        settings.getString("sound3Txt","Sound 3"));
                PutDataRequest putDataReq = putDataMapReq.asPutDataRequest().setUrgent();
                Log.d(TAG, "Attempting to write to data layer...");
                PendingResult<DataApi.DataItemResult> pendingResult =
                        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);


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

        // Bottom connection info text
        mConnectionText = (TextView) findViewById(R.id.connectionText);

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

        // Setting up listeners on titles or path names so we can trigger a picker


        mSound1Path.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Sound path 1 pressed");
                loadFilePicker(1);
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
        mSound1Txt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Sound path 1 pressed");
                loadFilePicker(1);
            }
        });
        mSound2Txt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Sound path 2 pressed");
                loadFilePicker(2);
            }
        });
        mSound3Txt.setOnClickListener(new View.OnClickListener() {
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
