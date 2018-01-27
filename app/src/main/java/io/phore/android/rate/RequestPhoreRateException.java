package io.phore.android.rate;

/**
 * Created by furszy on 7/5/17.
 */
public class RequestPhoreRateException extends Exception {
    public RequestPhoreRateException(String message) {
        super(message);
    }

    public RequestPhoreRateException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestPhoreRateException(Exception e) {
        super(e);
    }
}
