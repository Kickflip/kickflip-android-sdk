package io.kickflip.sdk.api;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import hugo.weaving.DebugLog;
import io.kickflip.sdk.api.json.HlsStream;
import io.kickflip.sdk.api.json.OAuthToken;
import io.kickflip.sdk.api.json.Stream;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.ConversionException;
import retrofit.converter.GsonConverter;
import retrofit.mime.FormUrlEncodedTypedOutput;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;
import rx.Observable;
import rx.functions.Func1;

/**
 * Created by davidbrodsky on 5/18/15.
 */
public class KickflipApiClientGenerator {

    private static final String BASE_URL = "https://kickflip.io";
    private static final String API_VERSION = "1.2";

    /**
     * Fetch a new TokenResponse from client credentials
     */
    @DebugLog
    public static Observable<OAuthToken> getTokenResponse(String clientId, String clientSecret) {

        RestAdapter oAuthAdapter = new RestAdapter.Builder()
                .setEndpoint(BASE_URL)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        KickflipOAuthService oAuthService = oAuthAdapter.create(KickflipOAuthService.class);
        // TODO : Possible to use const body field for 'client_credentials'?
        return oAuthService.getAccessToken("client_credentials", clientId, clientSecret);
    }

    /**
     * Fetch a new KickflipService from a TokenResponse
     */
    public static KickflipService getService(final OAuthToken tokenResponse) {

        final Gson gson = new Gson();

        RestAdapter apiAdapter = new RestAdapter.Builder()
                .setEndpoint(BASE_URL + "/api/" + API_VERSION)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Authorization", "Bearer " + tokenResponse.getAccessToken());
                    }
                })
                .setConverter(new GsonConverter(gson) {

                    // Url encodes requests from GSON objects
                    // Use: Instead of manually specifying request parameters, we can
                    // modify a response JSON object and re-post it to mutate a server object.
                    // Ideally the Kickflip API would read & write JSON instead of reading
                    // url form encoded data and writing JSON.

                    @Override
                    public Object fromBody(TypedInput body, Type type) throws ConversionException {

                        if (TypeToken.get(type).getRawType().getCanonicalName().equals(Stream.class.getCanonicalName())) {
                            // TODO : Kickflip server should return a definitive stream type
                            // until then, we assume all streams are HLS
                            return super.fromBody(body, HlsStream.class);
                        }
                        return super.fromBody(body, type);
                    }

                    @Override
                    public TypedOutput toBody(Object object) {
                        try {
                            Set<Map.Entry<String, JsonElement>> fieldSet = gson.toJsonTree(object).getAsJsonObject().entrySet();
                            FormUrlEncodedTypedOutput formUrlEncodedTypedOutput = new FormUrlEncodedTypedOutput();
                            for (Map.Entry<String, JsonElement> entry : fieldSet) {
                                if (!entry.getValue().isJsonPrimitive()) {
                                    throw new UnsupportedEncodingException(
                                            "GSON to URL encoding only supports shallow (depth=1) objects. Key: "
                                                    + entry.getKey() + " contains a non-primitive object!");
                                }
                                String valueString = entry.getValue().getAsJsonPrimitive().getAsString();
                                formUrlEncodedTypedOutput.addField(entry.getKey(), valueString);
                            }
                            return formUrlEncodedTypedOutput;
                        } catch (UnsupportedEncodingException | IllegalStateException e) {
                            throw new AssertionError(e);
                        }
                    }
                })
                .build();

        return apiAdapter.create(KickflipService.class);
    }

    /**
     * Fetch a new KickflipService from client credentials
     */
    public static Observable<KickflipService> getService(String clientId, String clientSecret) {
        return getTokenResponse(clientId, clientSecret)
                .map(new Func1<OAuthToken, KickflipService>() {
                    @Override
                    public KickflipService call(OAuthToken tokenResponse) {
                        return getService(tokenResponse);
                    }
                });
    }
}
