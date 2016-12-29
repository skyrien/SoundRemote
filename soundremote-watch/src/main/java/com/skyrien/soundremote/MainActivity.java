package com.skyrien.soundremote;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
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
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends WearableActivity {

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
    private TextView mTitleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();
        Log.d(TAG, "onCreate() called");

        // Set up the API client
        initGoogleApiClient();
        initUiFields();

        /*
        // Now, figure out if there's a suitable player for this watch
        CapabilityApi.GetCapabilityResult result =
                Wearable.CapabilityApi.getCapability(mGoogleApiClient, "soundremoteplayer",
                        CapabilityApi.FILTER_REACHABLE)
                .setResultCallbac
        */


        // Set up a capability listener
        CapabilityApi.CapabilityListener capabilityListener =
                new CapabilityApi.CapabilityListener() {
                    @Override
                    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
                        updateSoundRemoteCapability(capabilityInfo);
                    }
                };
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient,
                                                        capabilityListener, "soundremoteplayer");



    }

    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected() called");

    }

    private void updateSoundRemoteCapability(CapabilityInfo capabilityInfo) {
        Log.d(TAG, "updateSoundRemoteCapability() called");
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        playerNodeId = pickBestNodeId(connectedNodes);
    }

    private String pickBestNodeId(Set<Node> nodes) {
        Log.d(TAG, "pickBestNodeId() called");
        String bestNodeId = null;
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    private void playSoundId(int soundId) {
        Log.d(TAG, "playSoundId() called");
        if (playerNodeId != null) {
            byte[] message = new byte[1];
            message[0] = ((byte) soundId);
            Wearable.MessageApi.sendMessage(mGoogleApiClient, playerNodeId,
                    "soundremoteplayer", message).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.d(TAG, "message FAILED to send");
                            }
                        }
                    }
            );
        }
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

    private void initGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        // use it
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
        mGoogleApiClient.connect();
    }

    private void initUiFields() {

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mImageButton1 = (ImageButton) findViewById(R.id.sound1_img);
        mImageButton2 = (ImageButton) findViewById(R.id.sound2_img);
        mImageButton3 = (ImageButton) findViewById(R.id.sound3_img);
        mText1 = (TextView) findViewById(R.id.sound1_txt);
        mText2 = (TextView) findViewById(R.id.sound2_txt);
        mText3 = (TextView) findViewById(R.id.sound3_txt);
        mTitleText = (TextView) findViewById(R.id.titleText);

        // Set up listeners for buttons
        mImageButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Button 1 pressed!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Button 1 pressed!");
            }
        });

        mImageButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Button 2 pressed!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Button 2 pressed!");
            }
        });

        mImageButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Button 3 pressed!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Button 3 pressed!");
            }
        });
    }

    private void sendMessage(final String path, final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

}
