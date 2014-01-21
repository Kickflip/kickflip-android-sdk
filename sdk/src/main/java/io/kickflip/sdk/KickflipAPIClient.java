package io.kickflip.sdk;

import android.content.Context;
import android.util.Log;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;

import io.kickflip.sdk.json.KFUserDetailResponse;

/**
 * Kickflip OAuth API Client
 * Configured for "Client Credentials" OAuth Flow
 * <p/>
 * After construction, requests can be immediately performed
 * The client will handle acquiring and refreshing the OAuth
 * Access tokens as needed
 */
public class KickflipAPIClient extends OAuthClient {
    private static final String TAG = "KickflipAPIClient";

    public static final String BASE_URL = "http://funkcity.ngrok.com/";

    public static enum METHOD { GET, POST };

    public KickflipAPIClient(Context appContext, String key, String secret) {
        super(appContext, new OAuthConfig()
                .setCredentialStoreName("KF")
                .setClientId(key)
                .setClientSecret(secret)
                .setAccessTokenRequestUrl(BASE_URL + "o/token/")
                .setAccessTokenAuthorizeUrl(BASE_URL + "o/authorize/"));
        initialize();
    }

    public void initialize(){
        if(!awsCredentialsAcquired())
            createNewUser();
    }

    /**
     * Create a new Kickflip user and
     * store video storage credentials
     */
    public void createNewUser() {
        post(BASE_URL + "api/new/user", KFUserDetailResponse.class, new APICallback() {
            @Override
            public void onSuccess(Object response) {
                Log.i(TAG, "createNewUser response: " + ((KFUserDetailResponse) response).toString());
                storeUserDetails((KFUserDetailResponse) response);
            }

            @Override
            public void onError(Object response) {
                Log.w(TAG, "createNewUser Error: " + ((KFUserDetailResponse) response));
            }
        });
    }

    /**
     * Do a GET Request
     * @param url String url to GET
     * @param responseClass Class of the expected response
     * @param cb Callback that will receive an instance of responseClass
     */
    private void get(final String url, final Class responseClass, final APICallback cb) {
        acquireAccessToken(new OAuthCallback() {
            @Override
            public void ready(HttpRequestFactory requestFactory) {
                    request(requestFactory, METHOD.GET, url, null, responseClass, cb);
            }
        });
    }

    /**
     * Do a POST Request
     * @param url String url to POST
     * @param responseClass Class of the expected response
     * @param cb Callback that will receive an instance of responseClass
     */
    private void post(final String url, final Class responseClass, final APICallback cb){
        acquireAccessToken(new OAuthCallback() {
            @Override
            public void ready(HttpRequestFactory requestFactory) {
                request(requestFactory, METHOD.POST, url, null, responseClass, cb);
            }
        });
    }

    private void request(HttpRequestFactory requestFactory, METHOD method, final String url, HttpContent content, final Class responseClass, final APICallback cb){
        Log.i(TAG, String.format("Attempting %S request to %s", method, url));
        try{
            HttpRequest request = null;
            switch(method){
                case GET:
                    request = requestFactory.buildGetRequest(
                            new GenericUrl(url)).setParser(new JsonObjectParser(new JacksonFactory()));
                    break;
                case POST:
                    request = requestFactory.buildPostRequest(
                            new GenericUrl(url), content).setParser(new JsonObjectParser(new JacksonFactory()));
                    break;
            }
            handleHttpResponse(request.execute(), responseClass, cb);
        }catch (IOException exception){
            // First try to handle as HttpResponseException
            try{
                HttpResponseException httpException = (HttpResponseException) exception;
                Log.i(TAG, "HttpException: " + httpException.getStatusCode());
                switch(httpException.getStatusCode()){
                    case 403:
                        // OAuth Access Token invalid
                        Log.i(TAG, "Error 403: OAuth Token appears invalid. Clearing");
                        clearAccessToken();
                        request(requestFactory, method, url, content, responseClass, cb);
                        break;
                    case 400:
                        // Bad Client Credentials
                        Log.e(TAG, "Error 400: Check your Client key and secret");
                        break;
                    default:
                        httpException.printStackTrace();
                }
            } catch (ClassCastException e){
                exception.printStackTrace();
            }
        }
    }

    private void handleHttpResponse(HttpResponse response, Class responseClass, APICallback cb) throws IOException {
        if (cb != null) {
            Object parsedResponse = response.parseAs(responseClass);
            if (isSuccessResponse(response))
                cb.onSuccess(parsedResponse);
            else
                cb.onError(parsedResponse);
        }
    }

    private void storeUserDetails(KFUserDetailResponse response){
       getStorage().edit().putString("aws_access_key", response.getAwsAccessKey())
                          .putString("aws_secret_key", response.getAwsSecretKey())
                          .putString("app_name", response.getAppName())
                          .putString("name", response.getName())
                          .apply();
    }

    private boolean awsCredentialsAcquired(){
        return getStorage().contains("aws_secret_key");
    }

    private AWSCredentials getAWSCredentials(){
       return new AWSCredentials(
               getStorage().getString("aws_access_key",""),
               getStorage().getString("aws_secret_key",""),
               getStorage().getString("app_name",""));

    }


}
