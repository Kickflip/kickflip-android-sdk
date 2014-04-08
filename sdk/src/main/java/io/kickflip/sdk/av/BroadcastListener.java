package io.kickflip.sdk.av;


public interface BroadcastListener {
    public void onBroadcastStart();
    public void onBroadcastLive(String watchUrl);
    public void onBroadcastStop();
    public void onBroadcastError();
}
