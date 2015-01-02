package com.bitwiseops.rsteg;

@SuppressWarnings("serial")
public class CodecException extends Exception {

    public CodecException() {
        super();
    }

    public CodecException(String message, Throwable cause) {
        super(message, cause);
    }

    public CodecException(String message) {
        super(message);
    }

    public CodecException(Throwable cause) {
        super(cause);
    }
    
}
