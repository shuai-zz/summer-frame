package org.example.context;

import jakarta.annotation.Nullable;
import org.example.exception.BeanCreationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author zhaoshuai
 * 对于@Component注解的Bean
 * 1. 需要获取Class类型
 * 2. 然通过构造方法来创建Bean
 * 3. 收集@PostConstruct和@PreDestory标注的初始化与销毁的方法
 * 4. 其他信息：@Order定义Bean的内部排序顺序，@Primary定义存在多个相同类型时返回哪个Bean
 * 对于@Configuration定义的@Bean方法
 * 1. 看作Bean的工厂方法
 * 2. 获取方法return值作为Class类型
 * 3. 方法本身作为创建Bean的factoryMethod
 * 4. 收集@Bean定义的initMethod和destoryMehtod标识的初始化与销毁的方法名
 * 5. 其他的@Order、@Primary信息
 * 对于@Bean定义的Bean，声明类型beanClass与实际类型instance.getClass()可能不同（如声明类型的某个子类）
 */
public class BeanDefinition implements Comparable<BeanDefinition> {
    // 全局唯一的Bean Name;
    private final String name;
    // Bean的声明Class
    private final Class<?> beanClass;
    // Bean的实例
    private Object instance = null;
    // Bean的构造方法/null
    private final Constructor<?> constructor;
    // Bean的工厂Name/null
    private final String factoryName;
    // Bean的工厂方法/null
    private final Method factoryMethod;
    // Bean的顺序
    private final int order;
    // 是否标识@Primary
    private final boolean primary;

    // init/destroy方法名称:
    private String initMethodName;
    private String destroyMethodName;

    // init/destroy方法:
    private Method initMethod;
    private Method destroyMethod;

    public BeanDefinition(String name, Class<?> beanClass, Constructor<?> constructor, int order, boolean primary, String initMethodName, String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.name = name;
        this.beanClass = beanClass;
        this.constructor = constructor;
        this.factoryName = null;
        this.factoryMethod = null;
        this.order = order;
        this.primary = primary;
        constructor.setAccessible(true);
        setInitAndDestroyMethod(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }

    public BeanDefinition(String name, Class<?> beanClass, String factoryName, Method factoryMethod, int order, boolean primary, String initMethodName,
                          String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.name = name;
        this.beanClass = beanClass;
        this.constructor = null;
        this.factoryName = factoryName;
        this.factoryMethod = factoryMethod;
        this.order = order;
        this.primary = primary;
        factoryMethod.setAccessible(true);
        setInitAndDestroyMethod(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }

    private void setInitAndDestroyMethod(String initMethodName, String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.initMethodName = initMethodName;
        this.destroyMethodName = destroyMethodName;
        if (initMethod != null) {
            initMethod.setAccessible(true);
        }
        if (destroyMethod != null) {
            destroyMethod.setAccessible(true);
        }
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    @Nullable
    public Constructor<?> getConstructor() {
        return this.constructor;
    }

    @Nullable
    public String getFactoryName() {
        return this.factoryName;
    }

    @Nullable
    public Method getFactoryMethod() {
        return this.factoryMethod;
    }

    @Nullable
    public Method getInitMethod() {
        return this.initMethod;
    }

    @Nullable
    public Method getDestroyMethod() {
        return this.destroyMethod;
    }

    @Nullable
    public String getInitMethodName() {
        return this.initMethodName;
    }

    @Nullable
    public String getDestroyMethodName() {
        return this.destroyMethodName;
    }

    public String getName() {
        return this.name;
    }

    public Class<?> getBeanClass() {
        return this.beanClass;
    }

    @Nullable
    public Object getInstance() {
        return this.instance;
    }

    public Object getRequiredInstance() {
        if (this.instance == null) {
            throw new BeanCreationException(String.format("Instance of bean with name '%s' and type '%s' is not instantiated during current stage.",
                    this.getName(), this.getBeanClass().getName()));
        }
        return this.instance;
    }

    public void setInstance(Object instance) {
        Objects.requireNonNull(instance, "Bean instance is null");
        if (!this.beanClass.isAssignableFrom(instance.getClass())) {
            throw new BeanCreationException(String.format("Instance '%s' of Bean '%s' is not the expected type: %s",
                    instance, instance.getClass(), this.beanClass.getName()));
        }
        this.instance = instance;
    }

    public boolean isPrimary() {
        return this.primary;
    }

    @Override
    public String toString() {
        return "BeanDefinition [name=" + name + ", beanClass=" + beanClass.getName() + ", factory=" + getCreateDetail() + ", init-method="
                + (initMethod == null ? "null" : initMethod.getName()) + ", destroy-method=" + (destroyMethod == null ? "null" : destroyMethod.getName())
                + ", primary=" + primary + ", instance=" + instance + "]";
    }

    String getCreateDetail(){
        if(this.factoryMethod!=null){
            String params=String.join(", ", Arrays.stream(this.factoryMethod.getParameterTypes()).map(Class::getSimpleName).toArray(String[]::new));
            return this.factoryMethod.getDeclaringClass().getSimpleName()+"."+this.factoryMethod.getName()+"("+params+")";
        }
        return null;
    }

    @Override
    public int compareTo(BeanDefinition def) {
        int cmp = Integer.compare(this.order, def.order);
        if(cmp!=0){
            return cmp;
        }
        return this.name.compareTo(def.name);
    }
}
