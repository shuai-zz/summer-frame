package org.example.context;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.example.annotation.*;
import org.example.exception.*;
import org.example.io.PropertyResolver;
import org.example.io.ResourceResolver;
import org.example.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;


/**
 * @author zhaoshuai
 * 依赖注入模式：
 * <br>
 * 1. 构造函数注入
 * {@code
 * class A{
 * private final B b;
 * public A(B b){
 * this.b = b;
 * }
 * }}
 * <br>
 * 2. Setter注入
 * {@code
 * class A{
 * private B b;
 * @Autowired public void setB(B b){
 * this.b = b;
 * }
 * }}
 * <br>
 * 3. AutoWired注入
 * {@code
 * @Autowired private B b;
 * }
 * <br>
 * 4. 工厂方法注入
 * {@code
 * @Configuration public class AppConfig {
 * @Bean Hello hello(@Autowired JdbcTemplate jdbcTemplate) {
 * return new Hello(jdbcTemplate);
 * }
 * }
 * }
 *
 * <br>
 * NOTE: 构造方法注入与工厂方法注入，Bean的创建和注入是一体的，无法分成两个阶段
 * 字段注入与Setter方法注入，Bean的创建和注入是分开的，先创建Bean，再利用反射调用方法或字段，完成注入
 * <br>
 * NOTE: 前两种称为强依赖，若循环依赖只能报错
 * 后两种是弱依赖，能分两步，先实例化Bean，再注入依赖
 */
public class AnnotationConfigApplicationContext {
    /*
        IoC容器创建Bean过程
        1. 创建Bean实例，此时必须注入强依赖
        2. 对Bean实例进行Setter方法注入和字段注入
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final PropertyResolver propertyResolver;
    protected final Map<String, BeanDefinition> beans;
    // 跟踪当前正在创建的所有Bean的名称，用于解决循环依赖
    private final Set<String> creatingBeanName;


    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
        // 扫描获取所有Bean的Class类型
        Set<String> beanClassNames = scanForClassNames(configClass);
        // 创建Bean定义
        this.beans = createBeanDefinitions(beanClassNames);

        // create BeanName and detect circular dependencies
        this.creatingBeanName = new HashSet<>();
        // create a bean of type @Configuration
        this.beans.values().stream()
                // filter out @Configuration
                .filter(this::isConfigurationDefinition)
                .sorted()
                .map(def -> {
                    // create a Bean instance
                    createBeanAsEarlySingleton(def);
                    return def.getName();
                }).toList();

        // create other normal beans
        createNormalBeans();

        if (logger.isDebugEnabled()) {
            this.beans.values().stream()
                    .sorted()
                    .forEach(def -> logger.debug("bean initialized;{}", def));
        }
    }


    private void createNormalBeans() {
        List<BeanDefinition> defs = this.beans.values().stream()
                // filter out BeanDefinition with instance==null
                .filter(def -> def.getInstance() == null)
                .sorted()
                .toList();
        defs.forEach(def -> {
            // if the bean has not been created(it may have been created before the constructors of other beans were injected)
            if (def.getInstance() == null) {
                createBeanAsEarlySingleton(def);
            }
        });
    }

    // 创建一个Bean，但不进行字段和方法级别的注入。如果创建的Bean不是Configuration，则在构造方法/工厂方法中注入的依赖Bean会自动创建
    public Object createBeanAsEarlySingleton(BeanDefinition def) {
        logger.atDebug().log("Try create bean '{}' as early singleton:{}", def.getName(), def.getBeanClass().getName());

        if (!this.creatingBeanName.add(def.getName())) {
            // 监测到重复创建Bean导致的循环依赖
            throw new UnsatisfiedDependencyException(String.format("Circular dependency detected when create bean '%s'", def.getName()));
        }

        // create method: constructor or factory method
        Executable createFn = def.getFactoryName() == null ? def.getConstructor() : def.getFactoryMethod();


        // create parameters
        final Parameter[] parameters = createFn.getParameters();
        final Annotation[][] parameterAnnotations = createFn.getParameterAnnotations();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            // retrieve @Value and @Autowired from parameters
            final Parameter param = parameters[i];
            final Annotation[] paramAnnos = parameterAnnotations[i];
            final Value value = ClassUtils.getAnnotation(paramAnnos, Value.class);
            final Autowired autowired = ClassUtils.getAnnotation(paramAnnos, Autowired.class);

            // Beans of type @Configuration are factories and cannot be created by @Autowired
            final boolean isConfiguration = isConfigurationDefinition(def);
            if (isConfiguration && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify @Autowired when create @Configuration bean '%s': %s", def.getName(), def.getBeanClass().getName())
                );
            }
            // the parameter needs to be either @Value or @Autowired
            if (value != null && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify both @Value and @Autowired when create bean '%s': %s", def.getName(), def.getBeanClass().getName())
                );
            }
            if (value == null && autowired == null) {
                throw new BeanCreationException(
                        String.format("Must specify @Value or @Autowired when create bean '%s': %s", def.getName(), def.getBeanClass().getName())
                );
            }

            // type of the parameter
            final Class<?> type = param.getType();
            if (value != null) {
                // @Value
                args[i] = this.propertyResolver.getProperty(value.value(), type);
            } else {
                // @Autowired
                String name = autowired.name();
                boolean required = autowired.value();

                // dependency BeanDefinition
                BeanDefinition dependsOnDef = name.isEmpty() ? findBeanDefinition(type) : findBeanDefinition(name, type);
                // required==true?
                if (required && dependsOnDef == null) {
                    throw new BeanCreationException(
                            String.format("Missing autowired bean with type '%s' when create bean '%s': %s", type.getName(), def.getName(), def.getBeanClass().getName())
                    );
                }
                if (dependsOnDef != null) {
                    // get the dependency Bean
                    Object autowiredBeanInstance = dependsOnDef.getInstance();
                    if (autowiredBeanInstance == null && !isConfiguration) {
                        // current dependency Bean has not been initialized yet, recursively call to initialize the dependent bean
                        autowiredBeanInstance = createBeanAsEarlySingleton(dependsOnDef);
                    }
                    args[i] = autowiredBeanInstance;
                } else {
                    args[i] = null;
                }
            }
        }
        // create Bean  instance
        Object instance;
        if (def.getFactoryName() == null) {
            // create by constructor
            try {
                instance = def.getConstructor().newInstance(args);
            } catch (Exception e) {
                throw new BeanCreationException(String.format("Failed to create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
            }
        } else {
            // create by @Bean
            Object configInstance = getBean(def.getFactoryName());
            try {
                instance = def.getFactoryMethod().invoke(configInstance, args);
            } catch (Exception e) {
                throw new BeanCreationException(String.format("Failed to create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
            }
        }
        def.setInstance(instance);
        return def.getInstance();
    }

    /**
     * create BeanDefinition based on the scanned ClassName
     */
    Map<String, BeanDefinition> createBeanDefinitions(Set<String> classNameSet) {
        Map<String, BeanDefinition> defs = new HashMap<>();
        for (String className : classNameSet) {
            // get Class
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new BeanCreationException(e);
            }

            if (clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface() || clazz.isRecord()) {
                continue;
            }


            // @Component?
            Component component = ClassUtils.findAnnotation(clazz, Component.class);
            if (component != null) {
                logger.atDebug().log("found component:{}", clazz.getName());
                int mod = clazz.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be abstract");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be private");
                }
                String beanName = ClassUtils.getBeanName(clazz);
                var def = new BeanDefinition(beanName, clazz, getSuitableConstructor(clazz), getOrder(clazz), clazz.isAnnotationPresent(Primary.class),
                        // name init / destroy method
                        null, null,
                        // init method
                        ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                        // destroy method
                        ClassUtils.findAnnotationMethod(clazz, PreDestroy.class)
                );
                addBeanDefinition(defs, def);
                logger.atDebug().log("define bean:{}", def);

                Configuration configuration = ClassUtils.findAnnotation(clazz, Configuration.class);
                if (configuration != null) {
                    scanFactoryMethods(beanName, clazz, defs);
                }
            }
        }
        return defs;
    }

    /**
     * inject properties
     */
    void injectProperties(BeanDefinition def, Class<?> clazz, Object bean) throws ReflectiveOperationException {
        // find and inject Field and Method in current class
        for (Field f : clazz.getDeclaredFields()) {
            tryInjectProperties(def, clazz, bean, f);
        }
        for (Method m : clazz.getDeclaredMethods()) {
            tryInjectProperties(def, clazz, bean, m);
        }
        // find and inject Filed and Method in super class
        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null) {
            injectProperties(def, superClazz, bean);
        }
    }

    /**
     * inject single property
     */
    void tryInjectProperties(BeanDefinition def, Class<?> clazz, Object bean, AccessibleObject acc) throws ReflectiveOperationException {
        Value value = acc.getAnnotation(Value.class);
        Autowired autowired = acc.getAnnotation(Autowired.class);
        if (value == null && autowired == null) {
            return;
        }

        Field field = null;
        Method method = null;
        if (acc instanceof Field f) {
            checkFieldOrMethod(f);
            f.setAccessible(true);
            field = f;
        }
        if (acc instanceof Method m) {
            checkFieldOrMethod(m);
            if (m.getParameters().length != 1) {
                throw new BeanDefinitionException(String.format("Cannot inject a non-setter method %s for bean '%s': %s", m.getName(), def.getName(), def.getBeanClass().getName()));
            }
            m.setAccessible(true);
            method = m;
        }

        String accessibleName = field != null ? field.getName() : method.getName();
        Class<?> accessibleType = field != null ? field.getType() : method.getParameterTypes()[0];

        if (value != null && autowired != null) {
            throw new BeanCreationException(String.format("Cannot specify both @Autowired and @Value when inject %s.%s for bean '%s': %s",
                    clazz.getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
        }

        // inject @Value
        if (value != null) {
            Object propValue = this.propertyResolver.getRequiredProperty(value.value(), accessibleType);
            if (field != null) {
                logger.atDebug().log("Field injection: {}.{}={}", def.getBeanClass().getName(), accessibleName, propValue);
                field.set(bean, propValue);
            }
            if (method != null) {
                logger.atDebug().log("Method injection: {}.{}({})", def.getBeanClass().getName(), accessibleName, propValue);
                method.invoke(bean, propValue);
            }
        }

        // inject @Autowired
        if (autowired != null) {
            String name = autowired.name();
            boolean required = autowired.value();
            Object depends = name.isEmpty() ? findBean(accessibleType) : findBean(name, accessibleType);

            if (required && depends == null) {
                throw new UnsatisfiedDependencyException(String.format("Dependency bean not found when inject %s.%s for bean '%s': %s",
                        clazz.getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
            }
            if (depends != null) {
                if (field != null) {
                    logger.atDebug().log("Field injection: {}.{}={}", def.getBeanClass().getName(), accessibleName, depends);
                    field.set(bean, depends);
                }
                if (method != null) {
                    logger.atDebug().log("Method injection: {}.{}({})", def.getBeanClass().getName(), accessibleName, depends);
                    method.invoke(bean, depends);
                }
            }
        }
    }


    void checkFieldOrMethod(Member m) {
        int mod = m.getModifiers();
        if (Modifier.isStatic(mod)) {
            throw new BeanDefinitionException("Cannot inject static field: " + m);
        }
        if (Modifier.isFinal(mod)) {
            if (m instanceof Field field) {
                throw new BeanDefinitionException("Cannot inject final field: " + field);
            }
            if (m instanceof Method method) {
                logger.warn(
                        "Inject final method should be careful because it is not called on target bean when bean is proxied and may cause NullPointerException."
                );
            }
        }
    }

    /**
     * Get public constructor or non-public constructor as fallback.
     */
    Constructor<?> getSuitableConstructor(Class<?> clazz) {
        Constructor<?>[] cons = clazz.getConstructors();
        if (cons.length == 0) {
            cons = clazz.getDeclaredConstructors();
            if (cons.length != 1) {
                throw new BeanDefinitionException("More than one constructor found in class " + clazz.getName() + ".");
            }
        }
        if (cons.length != 1) {
            throw new BeanDefinitionException("More than one public constructor found in class " + clazz.getName() + ".");
        }
        return cons[0];
    }

    /**
     * Scan factory method that annotated with @Bean
     * <code>
     * &#64;Configuration
     * public class Hello{
     *
     * @Bean ZoneId createZone(){
     * return ZoneId.of("Z");
     * }
     * }
     * </code>
     */
    void scanFactoryMethods(String factoryBeanName, Class<?> clazz, Map<String, BeanDefinition> defs) {
        for (Method method : clazz.getDeclaredMethods()) {
            Bean bean = method.getAnnotation(Bean.class);
            if (bean != null) {
                int mod = method.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be abstract");
                }
                if (Modifier.isFinal(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be final");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be private");
                }

                Class<?> beanClass = method.getReturnType();
                // primitive type: int, long.....
                if (beanClass.isPrimitive()) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return primitive type");
                }
                if (beanClass == void.class || beanClass == Void.class) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return void");
                }
                var def = new BeanDefinition(ClassUtils.getBeanName(method), beanClass, factoryBeanName, method, getOrder(method),
                        method.isAnnotationPresent(Primary.class),
                        // init method
                        bean.initMethod().isEmpty() ? null : bean.initMethod(),
                        // destroy method
                        bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                        // @PostConstruct / @PreDestroy method
                        null, null
                );
                addBeanDefinition(defs, def);
                logger.atDebug().log("define bean:{}", def);
            }
        }
    }

    /**
     * Check and add bean definitions
     */
    void addBeanDefinition(Map<String, BeanDefinition> defs, BeanDefinition def) {
        if (defs.put(def.getName(), def) != null) {
            throw new BeanDefinitionException("Duplicate bean definition: " + def.getName());
        }
    }

    /**
     * Get order by:
     * <Code>
     * &#064;Order(100)
     * &#064;Component
     * public class Hello{}
     * </Code>
     */
    int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    /**
     * Get order by:
     * <Code>
     * &#064;Order(100)
     * &#064;Bean
     * public Hello createHello(){
     * return new Hello();
     * }
     * </Code>
     */
    int getOrder(Method method) {
        Order order = method.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    /**
     * Do component scan and return class names
     */
    protected Set<String> scanForClassNames(Class<?> configClass) {
        // get package name to be scanned
        ComponentScan scan = ClassUtils.findAnnotation(configClass, ComponentScan.class);
        // if no package name specified, scan the package of config class
        final String[] scanPackages = scan == null || scan.value().length == 0 ? new String[]{configClass.getPackage().getName()} : scan.value();
        logger.atInfo().log("component scan in package: {}", Arrays.toString(scanPackages));

        Set<String> classNameSet = new HashSet<>();
        for (String pkg : scanPackages) {
            // scan package
            logger.atDebug().log("scan package: {}", pkg);
            ResourceResolver rr = new ResourceResolver(pkg);
            List<String> classList = rr.scan(res -> {
                String name = res.name();
                if (name.endsWith(".class")) {
                    return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
                }
                return null;
            });
            if (logger.isDebugEnabled()) {
                classList.forEach(className -> {
                    logger.debug("class found by component scan: {}", className);
                });
            }
            classNameSet.addAll(classList);
        }

        // find @Import(xyz.class)
        Import importConfig = configClass.getAnnotation(Import.class);
        if (importConfig != null) {
            for (Class<?> importConfigClass : importConfig.value()) {
                String importClassName = importConfigClass.getName();
                if (classNameSet.contains(importClassName)) {
                    logger.warn("ignore import: " + importClassName + " for it is already been scanned.");
                } else {
                    logger.debug("class found by import: {}", importClassName);
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
     * find BeanDefinition based on Name
     * if Name not exist, return null
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String name) {
        return this.beans.get(name);
    }

    /**
     * find BeanDefinition based on Name and Type
     * if Name not exist, return null
     * if Name exists, but Type not match, throw BeanNotOfRequiredTypeException
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
        BeanDefinition def = findBeanDefinition(name);
        if (def == null) {
            return null;
        }
        if (!requiredType.isAssignableFrom(def.getBeanClass())) {
            throw new BeanNotOfRequiredTypeException(String.format("Autowire required type '%s' but bean '%s' has actual type '%s'.",
                    requiredType.getName(), name, def.getBeanClass().getName()));
        }
        return def;
    }

    /**
     * find BeanDefinition based on Type
     * return 0 or more
     */
    public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return this.beans.values().stream()
                //filter by type and subtype
                .filter(def -> type.isAssignableFrom(def.getBeanClass()))
                .sorted().toList();
    }

    /**
     * find BeanDefinition based on Type
     * if Type not exist, return null
     * if multiple BeanDefinition exist, return the one with annotation @Primary
     * if multiple @Primary exist or no @Primary but multiple BeanDefinition exist, throw NoUniqueBeanDefinitionException
     */
    @Nullable
    public BeanDefinition findBeanDefinition(Class<?> type) {
        List<BeanDefinition> defs = findBeanDefinitions(type);
        if (defs.isEmpty()) {
            return null;
        }
        if (defs.size() == 1) {
            return defs.getFirst();
        }
        // more than 1 beans, require @Primary
        List<BeanDefinition> primaryDefs = defs.stream().filter(BeanDefinition::isPrimary).toList();
        if (primaryDefs.size() == 1) {
            return primaryDefs.getFirst();
        }
        if (primaryDefs.isEmpty()) {
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, but no @Primary specified.",
                    type.getName()));
        } else {
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, but multiple @Primary specified.",
                    type.getName()));
        }
    }

    /**
     * find Bean based on Name
     * if Bean does not exist, throw NoSuchBeanDefinitionException
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        BeanDefinition def = this.beans.get(name);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s'.", name));
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * find Bean based on Name and Type
     * if Bean does not exist, throw NoSuchBeanDefinitionException
     * if Bean exists, but Type not match, throw BeanNotOfRequiredTypeException
     */
    public <T> T getBean(String name, Class<T> requiredType) {
        T t = findBean(name, requiredType);
        if (t == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s' and type '%s'.", name, requiredType));
        }
        return t;
    }

    /**
     * find Beans based on Type
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getBeans(Class<?> requiredType) {
        List<BeanDefinition> defs = findBeanDefinitions(requiredType);
        if (defs.isEmpty()) {
            return List.of();
        }
        List<T> list = new ArrayList<>(defs.size());
        for (BeanDefinition def : defs) {
            list.add((T) def.getRequiredInstance());
        }
        return list;
    }

    /**
     * find Bean based on Type
     * if not exist, throw NoSuchBeanDefinitionException
     * if multiple Beans exist but lack of a unique @Primary annotation, throw NoUniqueBeanDefinitionException
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with type '%s'.", requiredType));
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * check if Bean with the specified name exists
     */
    public boolean containsBean(String name) {
        return this.beans.containsKey(name);
    }

    // findXXX is similar to getXXX, but does not throw exception, instead return null
    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(String name, Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(name, requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> List<T> findBeans(Class<T> requiredType) {
        return findBeanDefinitions(requiredType).stream().map(def->(T)def.getRequiredInstance()).toList();
    }
}
