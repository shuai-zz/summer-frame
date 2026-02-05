package org.example.exception;

/**
 * @author zhaoshuai
 */
public class NestedRuntimeException extends RuntimeException{
    public NestedRuntimeException() {
    }
    public NestedRuntimeException(String message) {
        super(message);
    }

    public NestedRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
    public NestedRuntimeException(Throwable cause) {
        super(cause);
    }
}
