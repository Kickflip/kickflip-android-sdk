package io.kickflip.sdk.api.json;

import com.google.api.client.util.Key;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Serializable;
import java.util.Map;

/**
 * Kickflip base Stream response
 */
public class Stream extends Response implements Comparable<Stream>, Serializable {

    @Key("stream_id")
    private String mStreamId;

    @Key("stream_type")
    private String mStreamType;

    @Key("chat_url")
    private String mChatUrl;

    @Key("upload_url")
    private String mUploadUrl;

    @Key("stream_url")
    private String mStreamUrl;

    @Key("kickflip_url")
    private String mKickflipUrl;

    @Key("lat")
    private double mLatitude;

    @Key("lon")
    private double mLongitude;

    @Key("city")
    private String mCity;

    @Key("state")
    private String mState;

    @Key("country")
    private String mCountry;

    @Key("private")
    private boolean mPrivate;

    @Key("title")
    private String mTitle;

    @Key("description")
    private String mDescription;

    @Key("extra_info")
    private String mExtraInfo;

    @Key("thumbnail_url")
    private String mThumbnailUrl;

    @Key("time_started")
    private String mTimeStarted;

    @Key("length")
    private int mLength;

    @Key("user_username")
    private String mOwnerName;

    @Key("user_avatar")
    private String mOwnerAvatar;

    @Key("deleted")
    private boolean mDeleted;

    public boolean isDeleted() {
        return mDeleted;
    }

    public void setDeleted(boolean deleted) {
        mDeleted = deleted;
    }

    public String getOwnerName() {
        return mOwnerName;
    }

    public String getOwnerAvatar() {
        return mOwnerAvatar;
    }

    public String getThumbnailUrl() {
        return mThumbnailUrl;
    }

    public void setThumbnailUrl(String url) {
        mThumbnailUrl = url;
    }

    public String getStreamId() {
        return mStreamId;
    }

    public String getStreamType() {
        return mStreamType;
    }

    public String getChatUrl() {
        return mChatUrl;
    }

    public String getUploadUrl() {
        return mUploadUrl;
    }

    public String getStreamUrl() {
        return mStreamUrl;
    }

    public String getKickflipUrl() {
        return mKickflipUrl;
    }

    public String getTimeStarted() {
        return mTimeStarted;
    }

    public int getLengthInSeconds() {
        return mLength;
    }

    public Map getExtraInfo() {
        if (mExtraInfo != null && !mExtraInfo.equals("")) {
            return new Gson().fromJson(mExtraInfo, Map.class);
        }
        return null;
    }

    public void setExtraInfo(Map mExtraInfo) {
        this.mExtraInfo = new Gson().toJson(mExtraInfo);
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double mLatitude) {
        this.mLatitude = mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double mLongitude) {
        this.mLongitude = mLongitude;
    }

    public String getCity() {
        return mCity;
    }

    public void setCity(String mCity) {
        this.mCity = mCity;
    }

    public String getState() {
        return mState;
    }

    public void setState(String mState) {
        this.mState = mState;
    }

    public String getCountry() {
        return mCountry;
    }

    public void setCountry(String mCountry) {
        this.mCountry = mCountry;
    }

    public boolean isPrivate() {
        return mPrivate;
    }

    public void setIsPrivate(boolean mPrivate) {
        this.mPrivate = mPrivate;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String mDescription) {
        this.mDescription = mDescription;
    }

    @Override
    public int compareTo(Stream another) {
        return another.getTimeStarted().compareTo(getTimeStarted());
    }

    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }

}
