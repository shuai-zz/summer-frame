package org.example.exception;


/**
 * @author zhaoshuai
 */
public class DataAccessException extends NestedRuntimeException {
    public DataAccessException() {
    }

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(Throwable cause) {
        super(cause);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
