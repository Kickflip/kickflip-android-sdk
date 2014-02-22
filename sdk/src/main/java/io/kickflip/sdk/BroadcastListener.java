package io.kickflip.sdk;


public interface BroadcastListener {
    public void onBroadcastStart();
    public void onBroadcastLive(String watchUrl);
    public void onBroadcastStop();
    public void onBroadcastError();
}
