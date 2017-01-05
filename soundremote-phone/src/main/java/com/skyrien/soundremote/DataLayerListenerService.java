package com.skyrien.soundremote;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Set;

/**
 * Created by skyri on 1/4/2017.
 */

public class DataLayerListenerService extends WearableListenerService
        implements MessageApi.MessageListener {

    private static final String TAG = "DATALAYERLISTENER";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";

    private GoogleApiClient mGoogleApiClient;

    // Setup App References here
    private String remoteNodeId;
    private SoundPool soundPool;
    private int soundId1, soundId2, soundId3;
    boolean plays = false, loaded = false;
    float actVolume, maxVolume, volume;
    AudioManager audioManager;
    int counter;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called");
        super.onCreate();

        initGoogleApiClient();
        initAudioPool();
        Wearable.MessageApi.addListener(mGoogleApiClient, this);

    }

    @Override
    public void onDestroy() {
        stopGoogleApiClient();
        super.onDestroy();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "Message Received: " + messageEvent.getData()[0]);
        playSoundId(messageEvent.getData()[0]);
        super.onMessageReceived(messageEvent);

    }

    private void initGoogleApiClient() {
        Log.d(TAG, "Called initGoogleApiClient()");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        // so this is the callback for a successful connection
                        // I should probably add an event here to detect capabilities

                        setupSoundremote();

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


    private void stopGoogleApiClient() {
        Log.d(TAG, "stopGoogleApiClient() called");
        if (null != mGoogleApiClient) {
            if(mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
                Log.d(TAG, "GoogleApiClient disconnected!");

            }
        }
    }

    // These are the motions to set up the items to display on the remote (watch)
    private void setupSoundremote() {
        // first, find nearby capabiliites
        Wearable.CapabilityApi.getCapability(mGoogleApiClient, "soundremote",
                CapabilityApi.FILTER_REACHABLE)
                .setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                    @Override
                    public void onResult(CapabilityApi.GetCapabilityResult result) {
                        Log.d(TAG, "onResult() called -- found: " + result.getCapability().hashCode());
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
        remoteNodeId = pickBestNodeId(connectedNodes);
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


    private void initAudioPool() {

        // AudioManager stuff
        audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        actVolume = (float)audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        maxVolume = (float)audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume = actVolume / maxVolume;
        Log.d(TAG, "AudioManager created with vol: " + volume);

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
                Log.d(TAG, "SoundPool Created for sampleId: " + sampleId);

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

        // Loading hard coded sound assets
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
