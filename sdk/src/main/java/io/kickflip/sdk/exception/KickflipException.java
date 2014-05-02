package io.kickflip.sdk.exception;

import java.io.IOException;

/**
 * Kickflip Exception
 */
public class KickflipException extends IOException{
    private String mMessage;
    private int mCode;

    public KickflipException(){
        mMessage = "An unknown error occurred";
        mCode = 0;
    }

    public KickflipException(String message, int code){
        mMessage = message;
        mCode = code;
    }

    public String getMessage() {
        return mMessage;
    }

    public int getCode() {
        return mCode;
    }
}
