package io.kickflip.sdk.av;

import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.exception.KickflipException;

/**
 * Provides callbacks for the major lifecycle benchmarks of a Broadcast.
 */
public interface BroadcastListener {
    /**
     * The broadcast has started, and is currently buffering.
     */
    public void onBroadcastStart();

    /**
     * The broadcast is fully buffered and available. This is a good time to share the broadcast.
     *
     * @param stream the {@link io.kickflip.sdk.api.json.Stream} representing this broadcast.
     */
    public void onBroadcastLive(Stream stream);

    /**
     * The broadcast has ended.
     */
    public void onBroadcastStop();

    /**
     * An error occurred.
     */
    public void onBroadcastError(KickflipException error);
}
