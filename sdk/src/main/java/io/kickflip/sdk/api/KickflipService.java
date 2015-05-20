package io.kickflip.sdk.api;

import java.util.Map;

import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.api.json.StreamList;
import io.kickflip.sdk.api.json.User;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FieldMap;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;
import rx.Observable;

/**
 * Created by davidbrodsky on 5/18/15.
 */
public interface KickflipService {

    String NEW_USER = "/user/new";
    String GET_USER_PUBLIC = "/user/info";
    String GET_USER_PRIVATE = "/user/uuid";
    String EDIT_USER = "/user/change";
    String START_STREAM = "/stream/start";
    String STOP_STREAM = "/stream/stop";
    String SET_STREAM_META = "/stream/change";
    String GET_STREAM_META = "/stream/info";
    String FLAG_STREAM = "/stream/flag";
    String SEARCH_KEYWORD = "/search";
    String SEARCH_USER = "/search/user";
    String SEARCH_GEO = "/search/location";

    @FormUrlEncoded
    @POST(NEW_USER)
    Observable<User> createNewUser(@Field("password") String password);

    @FormUrlEncoded
    @POST(NEW_USER)
    Observable<User> createNewUser(@Field("username") String username,
                                   @Field("password") String password,
                                   @Field("display_name") String displayName,
                                   @Field("email") String email,
                                   @Field("extra_info") String extraInfo);

    @FormUrlEncoded
    @POST(GET_USER_PRIVATE)
    Observable<User> loginUser(@Field("username") String username,
                               @Field("password") String password);

    @FormUrlEncoded
    @POST(EDIT_USER)
    Observable<User> setUserInfo(@Field("new_password") String newPassword,
                                 @Field("display_name") String displayName,
                                 @Field("email") String email,
                                 @Field("extra_info") String extraInfo);

    @FormUrlEncoded
    @POST(GET_USER_PUBLIC)
    Observable<User> getUserInfo(@Field("username") String username);

    @FormUrlEncoded
    @POST(START_STREAM)
    Observable<Stream> startStream(@Field("uuid") String userUUID,
                                   @Field("private") boolean isPrivate,
                                   @Field("title") String title,
                                   @Field("description") String description,
                                   @Field("extra_info") String extraInfo);

    @FormUrlEncoded
    @POST(STOP_STREAM)
    Observable<Stream> stopStream(@Field("stream_id") String streamId,
                                  @Field("uuid") String userUUID,
                                  @Field("lat") double latitude,
                                  @Field("lon") double longitude);

    @FormUrlEncoded
    @POST(SET_STREAM_META)
    Observable<Stream> setStreamInfo(@FieldMap() Map<String, String> params);

    @FormUrlEncoded
    @POST(SET_STREAM_META)
    Observable<Stream> setStreamInfo(@Field("stream_id") String streamId,
                                     @Field("uuid") String userUUID,
                                     @Field("lat") double latitude,
                                     @Field("lon") double longitude,
                                     @Field("title") String title,
                                     @Field("description") String description,
                                     @Field("extra_info") String extraInfo,
                                     @Field("city") String city,
                                     @Field("state") String state,
                                     @Field("country") String country,
                                     @Field("thumbnail_url") String thumbnailUrl,
                                     @Field("private") boolean isPrivate,
                                     @Field("deleted") boolean isDeleted);

    @FormUrlEncoded
    @POST(GET_STREAM_META)
    Observable<Stream> getStreamInfo(@Field("stream_id") String streamId);

    @FormUrlEncoded
    @POST(FLAG_STREAM)
    Observable<Stream> flagStream(@Field("stream_id") String streamId,
                                  @Field("uuid") String userUUID);

    @FormUrlEncoded
    @POST(SEARCH_USER)
    Observable<StreamList> getStreamsByUser(@Field("uuid") String callingUserUUID,
                                            @Field("username") String targetUsername,
                                            @Field("results_per_page") int itemsPerPage,
                                            @Field("page") int page);

    @FormUrlEncoded
    @POST(SEARCH_KEYWORD)
    Observable<StreamList> getStreamsByKeyword(@Field("uuid") String callingUserUUID,
                                               @Field("keyword") String keyword,
                                               @Field("results_per_page") int itemsPerPage,
                                               @Field("page") int page);

    @FormUrlEncoded
    @POST(SEARCH_GEO)
    Observable<StreamList> getStreamsByLocation(@Field("uuid") String callingUserUUID,
                                                @Field("lat") double latitude,
                                                @Field("lon") double longitude,
                                                @Field("radius") int radiusMeters,
                                                @Field("results_per_page") int itemsPerPage,
                                                @Field("page") int page);

}
