package com.skyrien.soundremote;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.ConfirmationOverlay;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.Set;

public class MainActivity extends WearableActivity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // SECTION 1:
    // THIS SECTION DEALS WITH ACTIVITY SETUP

    private static final String TAG = "MainActivity";
    private GoogleApiClient mGoogleApiClient;
    private String playerNodeId;

    private BoxInsetLayout mContainerView;
    private ImageButton mImageButton1;
    private ImageButton mImageButton2;
    private ImageButton mImageButton3;
    private TextView mText1;
    private TextView mText2;
    private TextView mText3;
    private TextClock mTextClock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();
        Log.d(TAG, "onCreate() called");

        // Set up the API client - also connects to nodes
        initGoogleApiClient(); // this sets the player node id in the background

        // Setting up listeners and links for all UI items
        initUiFields();


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
            Toast.makeText(MainActivity.this, "No player found!\nTry relaunching SoundRemote on your phone", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "BOO! No SoundRemotePlayer found.");
        }
    }

    // SECTION 3:
    // BOILERPLATE FOR WEAR
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected() called");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended() called");

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "onConnectionFailed() called");

    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
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
            mTextClock.setTextColor(getResources().getColor(android.R.color.white));

        } else {
            mContainerView.setBackground(null);
            mText1.setTextColor(getResources().getColor(android.R.color.black));
            mText2.setTextColor(getResources().getColor(android.R.color.black));
            mText3.setTextColor(getResources().getColor(android.R.color.black));
            mTextClock.setTextColor(getResources().getColor(android.R.color.black));
        }
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
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        playerNodeId = pickBestNodeId(connectedNodes);
    }

    private String pickBestNodeId(Set<Node> nodes) {
        Log.d(TAG, "pickBestNodeId() called -- Other nodes: " + nodes.size());
        String bestNodeId = null;
        for (Node node : nodes) {
            if (node.isNearby()) {
                Log.d(TAG, "Found nearby node: " + node.getDisplayName());
                return node.getId();
            }
            bestNodeId = node.getId();
            Log.d(TAG, "Found best node: " + node.getDisplayName());
        }
        Log.d(TAG, "Found best node: " + bestNodeId);
        return bestNodeId;
    }


    private void initUiFields() {

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mImageButton1 = (ImageButton) findViewById(R.id.sound1_img);
        mImageButton2 = (ImageButton) findViewById(R.id.sound2_img);
        mImageButton3 = (ImageButton) findViewById(R.id.sound3_img);
        mText1 = (TextView) findViewById(R.id.sound1_txt);
        mText2 = (TextView) findViewById(R.id.sound2_txt);
        mText3 = (TextView) findViewById(R.id.sound3_txt);
        mTextClock = (TextClock) findViewById(R.id.textClock1);

        // Set up listeners for buttons
        mImageButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(MainActivity.this, "Button 1 pressed!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Button 1 pressed!");
                playRemoteSound(1);
            }
        });

        mImageButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(MainActivity.this, "Button 2 pressed!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Button 2 pressed!");
                playRemoteSound(2);
            }
        });

        mImageButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(MainActivity.this, "Button 3 pressed!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Button 3 pressed!");
                playRemoteSound(3);
            }
        });
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged() called");
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/soundtitles") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    String sound1Txt, sound2Txt, sound3Txt;
                    sound1Txt = dataMap.getString("sound1Txt");
                    sound2Txt = dataMap.getString("sound2Txt");
                    sound3Txt = dataMap.getString("sound3Txt");

                    Log.d(TAG, "DataItem strings: 1: " + sound1Txt + "; 2: " + sound2Txt + "; 3: " + sound3Txt);

                    // setting strings
                    mText1.setText(sound1Txt);
                    mText2.setText(sound2Txt);
                    mText3.setText(sound3Txt);
                }
            }
        }
    }




}
