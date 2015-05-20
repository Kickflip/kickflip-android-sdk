package io.kickflip.sdk.api.json;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by davidbrodsky on 3/11/14.
 */
public class StreamList extends Response {

    @SerializedName("streams")
    private List<Stream> mStreams;

    @SerializedName("next_page_available")
    private boolean mNextPageAvailable;

    @SerializedName("total_items")
    private int mTotalItems;

    @SerializedName("page_number")
    private int mPageNumber;

    @SerializedName("results_per_page")
    private int mResultsPerPage;

    public List<Stream> getStreams() {
        return mStreams;
    }

    public boolean isNextPageAvailable() {
        return mNextPageAvailable;
    }

    public int getTotalItems() {
        return mTotalItems;
    }

    public int getPageNumber() {
        return mPageNumber;
    }

    public int getResultsPerPage() {
        return mResultsPerPage;
    }
}
