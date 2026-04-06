package org.example.context;

import java.util.List;

/**
 * @author zhaoshuai
 * define the ApplicationContext interface for user use
 */
public interface ApplicationContext extends AutoCloseable{
    /**
     * does the bean with the specified name exist
     */
    boolean containsBean(String name);

    /**
     * Returns a unique bean based on its name;
     * if not found, throws NoSuchBeanDefinitionException
     */
    <T> T getBean(String name);

    /**
     * Returns a unique bean based on its name
     * if not found, throw NoSuchBeanDefinitionException
     * if found but the type does not match, throw BeanNotOfRequiredTypeException
     */
    <T> T getBean(String name, Class<T> requiredType);

    /**
     * Return a unique bean based on type
     * if not found, throw NoSuchBeanDefinitionException
     */
    <T> T getBean(Class<T> requiredType);

    /**
     * Return a list of beans based on type
     * if not found, return an empty set
     */
    <T> List<T> getBeans(Class<T> type);

    /**
     * Close and execute the destroy method of all beans
     */
    @Override
    void close();
}
