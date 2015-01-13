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

package net.openwatch.ffmpegwrapper;

import java.nio.ByteBuffer;

/**
 * A wrapper around the FFmpeg C libraries
 * designed for muxing encoded AV packets
 * into various output formats not supported by
 * Android's MediaMuxer, which is currently limited to .mp4
 *
 * As this is designed to complement Android's MediaCodec class,
 * the only supported formats for jData in writeAVPacketFromEncodedData are:
 * H264 (YUV420P pixel format) / AAC (16 bit signed integer samples, one center channel)
 *
 * Methods of this class must be called in the following order:
 * 0. (optional) setAVOptions
 * 1. prepareAVFormatContext
 * 2. (repeat for each packet) writeAVPacketFromEncodedData
 * 3. finalizeAVFormatContext
 * @author davidbrodsky
 *
 */
public class FFmpegWrapper {

    static {
        System.loadLibrary("FFmpegWrapper");
    }

    public native void setAVOptions(AVOptions jOpts);
    public native void prepareAVFormatContext(String jOutputPath);
    public native void writeAVPacketFromEncodedData(ByteBuffer jData, int jIsVideo, int jOffset, int jSize, int jFlags, long jPts);
    public native void finalizeAVFormatContext();

    /**
     * Used to configure the muxer's options.
     * Note the name of this class's fields 
     * have to be hardcoded in the native method
     * for retrieval.
     * @author davidbrodsky
     *
     */
    static public class AVOptions{
        public int videoWidth = 1280;
        public int videoHeight = 720;

        public int audioSampleRate = 44100;
        public int numAudioChannels = 1;

        // Format specific options
        public int hlsSegmentDurationSec = 10;

        public String outputFormatName = "hls";
        // TODO: Provide a Map for format-specific options
    }

}
