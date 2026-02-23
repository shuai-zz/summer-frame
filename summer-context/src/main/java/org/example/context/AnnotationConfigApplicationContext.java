package org.example.context;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.example.annotation.*;
import org.example.exception.BeanCreationException;
import org.example.exception.BeanDefinitionException;
import org.example.exception.BeanNotOfRequiredTypeException;
import org.example.exception.NoUniqueBeanDefinitionException;
import org.example.io.PropertyResolver;
import org.example.io.ResourceResolver;
import org.example.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author zhaoshuai
 */
public class AnnotationConfigApplicationContext {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final PropertyResolver propertyResolver;
    // 用一个Map<String, BeanDefinition>保存所有的Bean
    protected final Map<String, BeanDefinition> beans;

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
        // 扫描获取所有Bean的Class类型
        final Set<String> beanClassNames = scanForClassNames(configClass);
        // 创建BeanDefinition
        this.beans = createBeanDefinitions(beanClassNames);
    }

    /**
     * 根据扫描的ClassName创建BeanDefinition
     * 查找@Component时，并不是简单地在Class定义查看@Component注解，因为Spring的@Component是可以扩展的，例如，标记为Controller的Class也符合要求
     */
    Map<String, BeanDefinition> createBeanDefinitions(Set<String> classNameSet) {
        Map<String, BeanDefinition> defs = new HashMap<>();
        for (String className : classNameSet) {
            // 获取Class
             Class<?> clazz = null;
            try {
                // className: java.lang.String -> clazz: String.class
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new BeanCreationException(e);
            }
            if (clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface() || clazz.isRecord()) {
                continue;
            }
            // 是否标注@Component
            Component component = ClassUtils.findAnnotation(clazz, Component.class);
            if (component != null) {
                log.atDebug().log("found component: {}", clazz.getName());
                // 获取修饰符(modifier)
                int mod = clazz.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be abstract.");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be private.");
                }

                // 获取Bean的名称:
                String beanName = ClassUtils.getBeanName(clazz);
                var def = new BeanDefinition(beanName, clazz, getSuitableConstructor(clazz), getOrder(clazz), clazz.isAnnotationPresent(Primary.class),
                        // init/destroy方法名称:
                        null, null,
                        // 查找@PostConstruct方法:
                        ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                        // 查找@PreDestroy方法:
                        ClassUtils.findAnnotationMethod(clazz, PreDestroy.class));
                addBeanDefinitions(defs, def);
                log.atDebug().log("define bean: {}", def);

                // 查找是否有@Configuration:
                Configuration configuration = ClassUtils.findAnnotation(clazz, Configuration.class);
                if (configuration != null) {
                    // 查找@Bean方法:
                    scanFactoryMethods(beanName, clazz, defs);
                }
            }
        }
        return defs;
    }

    /**
     * 带有@Configuration注解的Class，视为Bean的工厂，我们需要继续在scanFactoryMethods()中查找@Bean标注的方法：
     * <p>
     * <code>
     * &#064;Configuration
     * <br>
     * public class Hello {
     * <br>
     * &#064;Bean
     * <br>
     * ZoneId createZone() {
     * return ZoneId.of("Z");
     * }
     * }
     * </code>
     */
    void scanFactoryMethods(String factoryBeanName, Class<?> clazz, Map<String, BeanDefinition> defs) {
        for (Method method : clazz.getDeclaredMethods()) {
            // 是否带有@Bean注解
            Bean bean = method.getAnnotation(Bean.class);
            if (bean != null) {
                int mod = method.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be abstract.");
                }
                if (Modifier.isFinal(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be final.");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be private.");
                }
                // Bean的声明类型是方法返回类型
                Class<?> beanClass = method.getReturnType();
                if (beanClass.isPrimitive()) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return primitive type.");
                }
                if (beanClass == void.class || beanClass == Void.class) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return void.");
                }
                var def = new BeanDefinition(
                        ClassUtils.getBeanName(method),
                        beanClass,
                        factoryBeanName,
                        // 创建Bean的工厂方法
                        method,
                        // @Order
                        getOrder(method),
                        // 是否是primary
                        method.isAnnotationPresent(Primary.class),
                        // init方法名称
                        bean.initMethod().isEmpty() ? null : bean.initMethod(),
                        // destroy方法名称
                        bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                        // @PostConstruct/@PreDestroy方法
                        null, null
                );
                addBeanDefinitions(defs, def);
                log.atDebug().log("define bean: {}", def);

            }
        }
    }

    /**
     * Get public constructor or non-public constructor as a fallback
     */
    Constructor<?> getSuitableConstructor(Class<?> clazz) {
        // 获取公共构造函数
        Constructor<?>[] cons = clazz.getConstructors();
        if (cons.length == 0) {
            // 若无，则获取所有声明的构造函数
            cons = clazz.getDeclaredConstructors();
            if (cons.length != 1) {
                throw new BeanDefinitionException("More than one constructor found in class " + clazz.getName() + ".");

            }
        }
        if (cons.length != 1) {
            throw new BeanDefinitionException("More than one constructor found in class " + clazz.getName() + ".");
        }
        return cons[0];
    }

    /**
     * Check and add bean definitions
     */
    void addBeanDefinitions(Map<String, BeanDefinition> defs, BeanDefinition def) {
        if (defs.put(def.getName(), def) != null) {
            throw new BeanDefinitionException("Duplicate bean name: " + def.getName());
        }
    }

    /**
     * Get order by:
     * <p>
     * <code>
     * &#64;Order(100)
     * <br>
     * &#64;Component
     * <br>
     * public class Hello {}
     * </code>
     */
    int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    /**
     * Get order by:
     * <p>
     * <code>
     * &#64;Order(100)
     * <br>
     * &#64;Bean
     * <br>
     * Hello createHello() {
     * return new Hello();
     * }
     * </code>
     */
    int getOrder(Method method) {
        Order order = method.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    /**
     * Do component scan and return class names
     */
    protected Set<String> scanForClassNames(Class<?> configClass) {
        // 获取要扫描的package名称
        ComponentScan scan = ClassUtils.findAnnotation(configClass, ComponentScan.class);
        final String[] scanPackages = scan == null || scan.value().length == 0 ? new String[]{configClass.getPackage().getName()} : scan.value();
        log.atInfo().log("component scan in packages: {}", Arrays.toString(scanPackages));
        Set<String> classNameSet = new HashSet<>();
        for (String pkg : scanPackages) {
            // 扫描package
            log.atDebug().log("scan package: {}", pkg);
            var rr = new ResourceResolver(pkg);
            List<String> classList = rr.scan(res -> {
                String name = res.name();
                if (name.endsWith(".class")) {
                    return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
                }
                return null;
            });
            if (log.isDebugEnabled()) {
                classList.forEach(className -> {
                    log.debug("class found by component scan: {}", className);
                });
            }
            classNameSet.addAll(classList);
        }
        // 查找@Import(xyz.class);
        Import importConfig = configClass.getAnnotation(Import.class);
        if (importConfig != null) {
            for (Class<?> importConfigClass : importConfig.value()) {
                String importClassName = importConfigClass.getName();
                if (classNameSet.contains(importClassName)) {
                    log.warn("ignore import: " + importClassName + " for it is already been scanned");
                } else {
                    log.debug("class found by import: {}", importClassName);
                    classNameSet.add(importClassName);
                }
            }
        }
        return classNameSet;
    }

    boolean isConfigurationDefinition(BeanDefinition def) {
        return ClassUtils.findAnnotation(def.getBeanClass(), Configuration.class) != null;
    }

    /**
     * 根据Name查找BeanDefinition，如果Name不存在，返回null，如果Name存在，但Type不匹配，抛出异常。
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String name) {
        return this.beans.get(name);
    }

    /**
     * 根据Name和Type查找BeanDefinition，如果Name不存在，返回null，如果Name存在，但Type不匹配，抛出异常。
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
        BeanDefinition def = findBeanDefinition(name);
        if (def == null) {
            return null;
        }
        if (!requiredType.isAssignableFrom(def.getBeanClass())) {
            throw new BeanNotOfRequiredTypeException(String.format("Autowire required type '%s' but bean '%s' has actual type '%s'.", requiredType.getName(),
                    name, def.getBeanClass().getName()));
        }
        return def;
    }


    /**
     * 根据Type查找若干个BeanDefinition，返回0个或多个
     */
    public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return this.beans.values().stream()
                // 按类型过滤:
                .filter(def -> type.isAssignableFrom(def.getBeanClass()))
                // 排序:
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 根据Type查找某个Bean Definition， 如果不存在，返回null
     * 如果存在多个返回@Primary标注的一个
     * 若存在多个或没有@Primary标注的，则抛出异常NoUniqueBeanDefinitionException
     */
    @Nullable
    public BeanDefinition findBeanDefinition(Class<?> type) {
        List<BeanDefinition> defs = findBeanDefinitions(type);
        // 没有找到任何BeanDefinition
        if (defs.isEmpty()) {
            return null;
        }
        // 找到唯一一个
        if (defs.size() == 1) {
            return defs.getFirst();
        }

        // 找到@Primary标注的
        List<BeanDefinition> primaryDefs = defs.stream().filter(BeanDefinition::isPrimary).toList();
        // @Primary唯一
        if (primaryDefs.size() == 1) {
            return primaryDefs.getFirst();
        }
        if (primaryDefs.isEmpty()) {
            // 不存在@Primary
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, but no @Primary specified.", type.getName()));
        } else {
            // @Primary不唯一
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, and multiple @Primary specified.", type.getName()));
        }
    }
}
