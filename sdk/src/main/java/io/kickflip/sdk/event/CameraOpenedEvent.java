package io.kickflip.sdk.event;

import android.hardware.Camera.Parameters;

/**
 * Used to pass the parameters of the opened camera to subscribers.
 */
public class CameraOpenedEvent {

    public Parameters params;

    public CameraOpenedEvent(Parameters params) {
        this.params = params;
    }

}
