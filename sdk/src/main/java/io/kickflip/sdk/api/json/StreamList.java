package io.kickflip.sdk.api.json;

import com.google.api.client.util.Key;

import java.util.List;

/**
 * Created by davidbrodsky on 3/11/14.
 */
public class StreamList extends Response {

    @Key("streams")
    private List<Stream> mStreams;

    @Key("next_page_available")
    private boolean mNextPageAvailable;

    @Key("total_items")
    private int mTotalItems;

    @Key("page_number")
    private int mPageNumber;

    @Key("results_per_page")
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
