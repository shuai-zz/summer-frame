package org.example.context;


import jakarta.annotation.Nullable;
import org.example.annotation.Bean;

import java.util.List;

/**
 * @author zhaoshuai
 * ConfigurableApplicationContext interface for use by Framwork-level code
 */
public interface ConfigurableApplicationContext extends ApplicationContext{
    List<BeanDefinition> findBeanDefinitions(Class<?> type);

    @Nullable
    BeanDefinition findBeanDefinition(Class<?> type);

    @Nullable
    BeanDefinition findBeanDefinition(String name);

    @Nullable
    BeanDefinition findBeanDefinition(String name, Class<?> type);

    Object createBeanAsEarlySingleton(BeanDefinition def);
}
