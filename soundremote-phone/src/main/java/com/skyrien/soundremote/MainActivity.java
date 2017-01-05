package com.skyrien.soundremote;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
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
        initGoogleApiClient();
        initUiFields();

        // Start listener service -- how do we know it's started?
        // Is there a way to start the whole service in a new thread?
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
                        // so this is the callback for a successful connection
                        // I should probably add an event here to detect capabilities

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

    // This method detects local nodes nearby and if found... should do something
    public void setupSoundremoteplayer() {
        Log.d(TAG, "setupSoundremotePlayer() called");
        // Check for capable sound remote player nodes
        Wearable.CapabilityApi.getCapability(mGoogleApiClient, "soundremoteplayer",
                CapabilityApi.FILTER_REACHABLE)
                .setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                    @Override
                    public void onResult(CapabilityApi.GetCapabilityResult result) {
                        Log.d(TAG, "getCapability returned onResult() -- found: " + result.getCapability().hashCode());
                        updateSoundRemoteCapability(result.getCapability());
                    }
                });


        // Adding a listener to support updating based on changes
        CapabilityApi.CapabilityListener capabilityListener =
                new CapabilityApi.CapabilityListener() {
                    @Override
                    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
                        Log.d(TAG, "onCapabilityChanged() called");
                        updateSoundRemoteCapability(capabilityInfo);
                    }
                };
        Wearable.CapabilityApi.addCapabilityListener(
                mGoogleApiClient,
                capabilityListener,
                "soundremoteplayer");

    }

    // This method actually updates the local representation of nodes based on what it finds
    private void updateSoundRemoteCapability(CapabilityInfo capabilityInfo) {
        Log.d(TAG, "updateSoundRemoteCapability() called");
        Wearable.NodeApi.getLocalNode(mGoogleApiClient).setResultCallback(
                new ResultCallback<NodeApi.GetLocalNodeResult>() {
                    @Override
                    public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
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
