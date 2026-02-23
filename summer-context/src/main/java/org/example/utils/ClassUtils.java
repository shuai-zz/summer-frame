package org.example.utils;


import jakarta.annotation.Nullable;
import org.example.annotation.Bean;
import org.example.annotation.Component;
import org.example.context.BeanDefinition;
import org.example.exception.BeanCreationException;
import org.example.exception.BeanDefinitionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zhaoshuai
 */
public class ClassUtils {
    /**
     * 递归查找Annotation
     * <p>
     * 示例：Annotation A可以直接标注在Class定义:
     * <p>
     * <code>
     * &#064;A <br>
     * public class Hello {}
     * </code>
     * <p>
     * 或者Annotation B标注了A，Class标注了B:
     * <p>
     * <code>
     * &#64;A
     * public @interface B {}
     * <p>
     * &#064;B
     * public class Hello {}
     * </code>
     */
    public static <A extends Annotation> A findAnnotation(Class<?> target, Class<A> annoClass){
        A a=target.getAnnotation(annoClass);
        for(Annotation anno:target.getAnnotations()){
            Class<? extends Annotation> annoType = anno.annotationType();
            if(!"java.lang.annotation".equals(annoType.getPackageName())){
                A found=findAnnotation(annoType, annoClass);
                if(found!=null){
                    if(a!=null){
                        throw new BeanDefinitionException("Duplicate @" + annoClass.getSimpleName()+" found on class "+target.getSimpleName());
                    }
                    a=found;
                }
            }
        }
        return a;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A getAnnotation(Annotation[] annotations, Class<A> annoClass){
        for(Annotation anno:annotations){
            if(annoClass.isInstance(anno)){
                return (A) anno;
            }
        }
        return null;
    }

    /**
     * Get bean name by:
     * <p>
     *     <code>
     *          &#064;Bean
     * Hello createHello(){}
     *     </code>
     */
    public static String getBeanName(Method method){
        Bean bean = method.getAnnotation(Bean.class);
        String name =bean.value();
        if(name.isEmpty()){
            name=method.getName();
        }
        return name;
    }

    /**
     * Get bean name by:
     * <p>
     *     <code>
     *         &#064;Component
     *         public class Hello {}
     *         </code>
     */
    public static String getBeanName(Class<?> clazz){
        String name="";
        // 查找@Component
        Component component = clazz.getAnnotation(Component.class);
        if(component!=null){
            // @Component exist
            name=component.value();
        }else{
            // @Component not exist, 在其他注解中查找@Component
            for(Annotation anno:clazz.getAnnotations()){
                if(findAnnotation(anno.annotationType(), Component.class)!=null){
                    try{
                        name=(String) anno.annotationType().getMethod("value").invoke(anno);
                    }catch (ReflectiveOperationException e){
                        throw new BeanDefinitionException("Cannot get annotation value. ", e);
                    }
                }
            }
        }
        if (name.isEmpty()){
            // default name: "HelloWorld" => "helloWorld
            name=clazz.getSimpleName();
            name=Character.toLowerCase(name.charAt(0))+name.substring(1);
        }
        return name;
    }

    /**
     * Get non-arg method by @PostConstruct or @PreDestroy. Not search in super class.
     * <p >
     *     <code>
     * &#064;PostConstruct
     * void init(){}
     *     </code>
     */
    @Nullable
    public static Method findAnnotationMethod(Class<?> clazz, Class<? extends Annotation> annoClass){
        // try get declared method:
        List<Method> ms= Arrays.stream(clazz.getDeclaredMethods())
                .filter(m->m.isAnnotationPresent(annoClass))
                .peek(m->{
                    if(m.getParameterCount()!=0){
                        throw new BeanDefinitionException(String.format("Method '%s' with @%s must not have argument: %s",m.getName(),annoClass.getSimpleName(),clazz.getName()));
                    }
                }).toList();
        if(ms.isEmpty()){
            return null;
        }
        if(ms.size()==1){
            return ms.getFirst();
        }
        throw new BeanDefinitionException(String.format("Multiple method with @%s found in class: %s", annoClass.getSimpleName(), clazz.getName()));
    }

    /**
     * Get non-arg method by method name. Not search in super class.
     */
    public static Method getNamedMethod(Class<?> clazz, String methodName){
        try {
            return clazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new BeanDefinitionException(String.format("Method '%s' not found in class: %s", methodName, clazz.getName()));
        }
    }
}
