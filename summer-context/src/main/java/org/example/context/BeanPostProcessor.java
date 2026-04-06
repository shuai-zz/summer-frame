package org.example.context;

/**
 * @author zhaoshuai
 */
public interface BeanPostProcessor {
    /**
     * invoked after new Bean().
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName)  {
        return bean;
    }

    /**
     * invoked before Bean.init() called.
     */
    default Object postProcessAfterInitialization(Object bean, String beanName)  {
        return bean;
    }

    /**
     * invoked before bean.setXyz() called.
     */
    default Object postProcessOnSetProperty(Object bean, String beanName)  {
        return bean;
    }
}
