package io.kickflip.sdk;


public interface BroadcastListener {
    public void onBroadcastStart();
    public void onBroadcastLive();
    public void onBroadcastStop();
    public void onBroadcastError();
}
