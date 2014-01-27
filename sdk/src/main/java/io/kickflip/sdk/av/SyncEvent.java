package io.kickflip.sdk.av;

/**
 * Generic sync event used to
 * synchronize audio / video recording
 */
public class SyncEvent {

    private long mSyncTime;
    private boolean mStarted;

    public SyncEvent(){
        mStarted = false;
    }

    private void markSyncTime(){
        mSyncTime = System.nanoTime();
        mStarted = true;
    }

    /**
     * Returns the SyncEvent time,
     * representing calling time
     * if it hasn't yet been set
     * @return start time in nanoseconds
     */
    public long getSyncTimeNs(){
        if(!mStarted)
            markSyncTime();
        return mSyncTime;
    }

    public boolean isStarted(){
        return mStarted;
    }
}
