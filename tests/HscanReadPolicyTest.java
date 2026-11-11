package com.veloster.dashboard;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/** Genuine JDK exception types, including the observed API17 native message. */
public final class HscanReadPolicyTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        check(HscanReadPolicy.isIdle(new IOException("Try again")),"observed readba_native EAGAIN is idle");
        check(HscanReadPolicy.isIdle(new IOException("Resource temporarily unavailable")),"alternate POSIX EAGAIN wording is idle");
        check(HscanReadPolicy.isIdle(new SocketTimeoutException("Read timed out")),"typed socket timeout remains idle");
        check(HscanReadPolicy.isIdle(new SocketTimeoutException()),"typed socket timeout need not have a message");
        check(HscanReadPolicy.isIdle(new InterruptedIOException("interrupted")),"InterruptedIOException remains idle");
        check(HscanReadPolicy.isIdle(new InterruptedIOException()),"typed interruption need not have a message");
        check(!HscanReadPolicy.isIdle(null),"null exception is not idle");
        check(!HscanReadPolicy.isIdle(new IOException()),"plain null message is an error");
        for(String message:new String[]{"","Connection reset by peer","Socket closed","Permission denied","Broken pipe","End of stream",
                "Operation not permitted","Connection refused","read failed","Interrupted system call","Try again ","try again",
                "read failed: EAGAIN (Try again)","Resource temporarily unavailable: permission denied"}){
            check(!HscanReadPolicy.isIdle(new IOException(message)),"other IOException remains an error: "+message);
        }
        check(!HscanReadPolicy.isIdle(new EOFException("EOF")),"EOF remains a disconnect");
        check(!HscanReadPolicy.isIdle(new SocketException("Connection reset")),"socket reset remains a disconnect");
        check(!HscanReadPolicy.isIdle(new SocketException("Socket is closed")),"closed socket remains a disconnect");
        System.out.println("PASS "+checks+" HSCAN idle/error classification checks");
    }
}
