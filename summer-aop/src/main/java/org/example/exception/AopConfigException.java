package org.example.exception;

/**
 * @author zhaoshuai
 */
public class AopConfigException extends NestedRuntimeException{
    public AopConfigException() {
        super();
    }
    public AopConfigException(String message) {
        super(message);
    }
    public AopConfigException(String message, Throwable cause) {
        super(message, cause);
    }
    public AopConfigException(Throwable cause) {
        super(cause);
    }
}
