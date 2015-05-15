/*
 * Copyright (c) 2013, David Brodsky. All rights reserved.
 *
 *	This program is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kickflip.sdk.av;

import android.os.FileObserver;
import android.util.Log;

import com.google.common.eventbus.EventBus;

import java.io.File;

import io.kickflip.sdk.event.HlsManifestWrittenEvent;
import io.kickflip.sdk.event.HlsSegmentWrittenEvent;
import io.kickflip.sdk.event.ThumbnailWrittenEvent;

/**
 * A FileObserver that listens for actions
 * specific to the creation of an HLS stream
 * e.g: A .ts segment is written
 * or a .m3u8 manifest is modified
 *
 * @author davidbrodsky
 * @hide
 */
public class HlsFileObserver extends FileObserver {
    private static final String TAG = "HlsFileObserver";
    private static final boolean VERBOSE = false;

    private static final String M3U8_EXT = "m3u8";
    private static final String TS_EXT = "ts";
    private static final String JPG_EXT = "jpg";
    private String mObservedPath;
    private EventBus mEventBus;

    /**
     * Begin observing the given path for changes
     * to .ts, .m3u8 and .jpg files
     *
     * @param path     the absolute path to observe.
     * @param eventBus an EventBus to post events to
     */
    public HlsFileObserver(String path, EventBus eventBus) {
        super(path, CLOSE_WRITE | MOVED_TO);
        mEventBus = eventBus;
        mObservedPath = path;
    }

    @Override
    public void onEvent(int event, String path) {
        if (path == null) return; // If the directory was deleted.
        String ext = path.substring(path.lastIndexOf('.') + 1);
        String absolutePath = mObservedPath + File.separator + path;
        Log.d(TAG, String.format("Event %d at %s ext %s", event, path, ext));

        if (event == MOVED_TO && ext.equals(M3U8_EXT)) {
            if (VERBOSE) Log.i(TAG, "posting manifest written " + absolutePath);
            mEventBus.post(new HlsManifestWrittenEvent(absolutePath));

        } else if (event == CLOSE_WRITE && ext.equals(TS_EXT)) {
            if (VERBOSE) Log.i(TAG, "posting hls segment written " + absolutePath);
            mEventBus.post(new HlsSegmentWrittenEvent(absolutePath));

        } else if (event == CLOSE_WRITE && ext.equals(JPG_EXT)) {
            if (VERBOSE) Log.i(TAG, "posting thumbnail written " + absolutePath);
            mEventBus.post(new ThumbnailWrittenEvent(absolutePath));
        }
    }

}
