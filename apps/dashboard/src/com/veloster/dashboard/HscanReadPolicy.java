package com.veloster.dashboard;

import java.io.IOException;
import java.io.InterruptedIOException;

/** API17 LocalSocket can report SO_RCVTIMEO as a plain POSIX IOException. */
final class HscanReadPolicy {
    private HscanReadPolicy(){}
    static boolean isIdle(IOException failure){
        if(failure instanceof InterruptedIOException)return true;
        if(failure==null)return false;
        String message=failure.getMessage();
        return "Try again".equals(message)||"Resource temporarily unavailable".equals(message);
    }
}
