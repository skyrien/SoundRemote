package com.skyrien.soundremote;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
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
    private static final String SETTINGS = "SoundRemote";
    String RESOURCE_PATH = ContentResolver.SCHEME_ANDROID_RESOURCE + "://";
    String path = RESOURCE_PATH + getPackageName() + "/";


    // Setup App References here
    private GoogleApiClient mGoogleApiClient;
    private AssetManager assetManager;

    private String remoteNodeId;
    private MediaPlayer[] mediaPlayers;
    private Uri[] soundUri;
    private int soundId1, soundId2, soundId3;
    boolean plays = false, loaded = false;
    float actVolume, maxVolume, volume;
    AudioManager audioManager;
    int counter;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called");
        super.onCreate();

        // Async load of API Client
        initGoogleApiClient();

        // Media and AudioManager stuff
        mediaPlayers = new MediaPlayer[3];
        soundUri = new Uri[3];

        audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        actVolume = (float)audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        maxVolume = (float)audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume = actVolume / maxVolume;
        Log.d(TAG, "AudioManager created with vol: " + volume);





        // Async load of audio files
        initAudioPool();

        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }


    @Override
    public void onDestroy() {
        stopGoogleApiClient();
        mediaPlayers[0].release();
        mediaPlayers[1].release();
        mediaPlayers[2].release();

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


    public void initAudioPool() {
        Log.d(TAG, "initAudioPool() called");

        try {

            /*
            // Don't need this as we've set these as default values in onCreate
            // Load internal assets
            String RESOURCE_PATH = ContentResolver.SCHEME_ANDROID_RESOURCE + "://";
            String path = RESOURCE_PATH + getPackageName() + "/";
            soundUri[0] = Uri.parse(path + R.raw.sample1);
            soundUri[1] = Uri.parse(path + R.raw.sample2);
            soundUri[2] = Uri.parse(path + R.raw.sample3);
            */

            // Load SharedPreferences values into local instance of soundUri
            // should we validate or trust the source?
            SharedPreferences settings = getSharedPreferences(SETTINGS,0);
            Log.d(TAG, "Loading from SharedPreferences...");
            soundUri[0] = Uri.parse(settings.getString("sound1Path", path + R.raw.sample1));
            Log.d(TAG, "Found #1: " + soundUri[0].toString());
            soundUri[1] = Uri.parse(settings.getString("sound2Path", path + R.raw.sample2));
            Log.d(TAG, "Found #2: " + soundUri[1].toString());
            soundUri[2] = Uri.parse(settings.getString("sound3Path", path + R.raw.sample3));
            Log.d(TAG, "Found #3: " + soundUri[2].toString());

            for (int i = 0; i <= 2; i++) {
                mediaPlayers[i] = new MediaPlayer();
                mediaPlayers[i].setDataSource(getApplicationContext(), soundUri[i]);
                mediaPlayers[i].prepareAsync();
            }

        } catch (Throwable e) {
            Log.e(TAG, "Something happened while setting data source.");
            e.printStackTrace();
        }
    }



    public void playSoundId(int soundId) {
        Log.d(TAG, "playSoundId() called for soundId: " + soundId);

        // soundId == 0 is a special message to trigger a reload of the audio files.
        // No other action needs to be taken so we return afterward
        if (soundId == 0) {
            Log.d(TAG, "Reinitializing audio pool");

            for (int i = 0; i <= 2; i++) {
                mediaPlayers[i].release();
            }
            
            initAudioPool();
            return;
        }

        int requestIndex = soundId - 1;
        //mediaPlayers[requestIndex].start();

        for (int i = 0; i <= 2; i++) {
            // The right sound and player
            if (i == (requestIndex))
            {
                if (!mediaPlayers[i].isPlaying()) {
                    mediaPlayers[i].start();
                }
                else { // if it is playing, seek to 0 while keeping in play
                    mediaPlayers[i].seekTo(0);
                    }
            }


            else if (i != (requestIndex) && mediaPlayers[i].isPlaying()) {
                mediaPlayers[i].stop();
                mediaPlayers[i].prepareAsync();
            }
        }


    }


}
