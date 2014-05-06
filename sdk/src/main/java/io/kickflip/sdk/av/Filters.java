package io.kickflip.sdk.av;

import android.util.Log;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * This class matches descriptive final int
 * variables to Texture2dProgram.ProgramType
 * @hide
 */
public class Filters {
    private static final String TAG = "Filters";
    private static final boolean VERBOSE = false;

    // Camera filters; must match up with camera_filter_names in strings.xml
    static final int FILTER_NONE = 0;
    static final int FILTER_BLACK_WHITE = 1;
    static final int FILTER_NIGHT = 2;
    static final int FILTER_CHROMA_KEY = 3;
    static final int FILTER_BLUR = 4;
    static final int FILTER_SHARPEN = 5;
    static final int FILTER_EDGE_DETECT = 6;
    static final int FILTER_EMBOSS = 7;
    static final int FILTER_SQUEEZE = 8;
    static final int FILTER_TWIRL = 9;
    static final int FILTER_TUNNEL = 10;
    static final int FILTER_BULGE = 11;
    static final int FILTER_DENT = 12;
    static final int FILTER_FISHEYE = 13;
    static final int FILTER_STRETCH = 14;
    static final int FILTER_MIRROR = 15;

    /**
     * Ensure a filter int code is valid. Update this function as
     * more filters are defined
     * @param filter
     */
    public static void checkFilterArgument(int filter){
        checkArgument(filter >= 0 && filter <= 15);
    }

    /**
     * Updates the filter on the provided FullFrameRect
     * @return the int code of the new filter
     */
    public static void updateFilter(FullFrameRect rect, int newFilter) {
        Texture2dProgram.ProgramType programType;
        float[] kernel = null;
        float colorAdj = 0.0f;

        if (VERBOSE) Log.d(TAG, "Updating filter to " + newFilter);
        switch (newFilter) {
            case FILTER_NONE:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT;
                break;
            case FILTER_BLACK_WHITE:
                // (In a previous version the TEXTURE_EXT_BW variant was enabled by a flag called
                // ROSE_COLORED_GLASSES, because the shader set the red channel to the B&W color
                // and green/blue to zero.)
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_BW;
                break;
            case FILTER_NIGHT:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_NIGHT;
                break;
            case FILTER_CHROMA_KEY:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_CHROMA_KEY;
                break;
            case FILTER_SQUEEZE:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_SQUEEZE;
                break;
            case FILTER_TWIRL:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_TWIRL;
                break;
            case FILTER_TUNNEL:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_TUNNEL;
                break;
            case FILTER_BULGE:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_BULGE;
                break;
            case FILTER_DENT:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_DENT;
                break;
            case FILTER_FISHEYE:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FISHEYE;
                break;
            case FILTER_STRETCH:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_STRETCH;
                break;
            case FILTER_MIRROR:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_MIRROR;
                break;
            case FILTER_BLUR:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT;
                kernel = new float[] {
                        1f/16f, 2f/16f, 1f/16f,
                        2f/16f, 4f/16f, 2f/16f,
                        1f/16f, 2f/16f, 1f/16f };
                break;
            case FILTER_SHARPEN:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT;
                kernel = new float[] {
                        0f, -1f, 0f,
                        -1f, 5f, -1f,
                        0f, -1f, 0f };
                break;
            case FILTER_EDGE_DETECT:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT;
                kernel = new float[] {
                        -1f, -1f, -1f,
                        -1f, 8f, -1f,
                        -1f, -1f, -1f };
                break;
            case FILTER_EMBOSS:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT;
                kernel = new float[] {
                        2f, 0f, 0f,
                        0f, -1f, 0f,
                        0f, 0f, -1f };
                colorAdj = 0.5f;
                break;
            default:
                throw new RuntimeException("Unknown filter mode " + newFilter);
        }

        // Do we need a whole new program?  (We want to avoid doing this if we don't have
        // too -- compiling a program could be expensive.)
        if (programType != rect.getProgram().getProgramType()) {
            rect.changeProgram(new Texture2dProgram(programType));
        }

        // Update the filter kernel (if any).
        if (kernel != null) {
            rect.getProgram().setKernel(kernel, colorAdj);
        }
    }
}
