package io.kickflip.sample;

import io.kickflip.sample.fragment.MainFragment;

/**
 * All activities used with MainActivity implement this interface
 * for communication.
 */
public interface MainFragmentInteractionListener {
    public void onFragmentEvent(MainFragment.EVENT event);
}
