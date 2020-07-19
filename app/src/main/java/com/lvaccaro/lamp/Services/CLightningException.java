package com.lvaccaro.lamp.Services;

public class CLightningException extends Exception{

    public CLightningException() {
        super();
    }

    public CLightningException(String message) {
        super(message);
    }

    public CLightningException(String message, Throwable cause) {
        super(message, cause);
    }

    public CLightningException(Throwable cause) {
        super(cause);
    }

    protected CLightningException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
