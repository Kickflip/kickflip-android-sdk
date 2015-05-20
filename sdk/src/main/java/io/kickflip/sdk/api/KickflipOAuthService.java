package io.kickflip.sdk.api;

import io.kickflip.sdk.api.json.OAuthToken;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;
import rx.Observable;

/**
 * Created by davidbrodsky on 5/19/15.
 */
public interface KickflipOAuthService {

    @FormUrlEncoded
    @POST("/o/token/")
    Observable<OAuthToken> getAccessToken(@Field("grant_type") String grantType,
                                          @Field("client_id") String clientId,
                                          @Field("client_secret") String clientSecret);
}