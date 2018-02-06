package io.phore.android.rate.db;

import java.math.BigDecimal;

/**
 * Created by furszy on 7/5/17.
 */

public class PhoreRate {

    /** Coin letters (USD,EUR,etc..) */
    private final String code;
    /** Value of 1 phr in this rate */
    private final BigDecimal rate;
    /** Last update time */
    private final long timestamp;

    public PhoreRate(String code, BigDecimal rate, long timestamp) {
        this.code = code;
        this.rate = rate;
        this.timestamp = timestamp;

    }

    public String getCode() {
        return code;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Old method..
     */
    public String getLink(){
        return null;
    }

}
