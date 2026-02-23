package org.example.exception;

/**
 * @author zhaoshuai
 */
public class NoSuchBeanDefinitionException extends BeanDefinitionException{
    public NoSuchBeanDefinitionException() {
    }

    public NoSuchBeanDefinitionException(String message) {
        super(message);
    }
}
