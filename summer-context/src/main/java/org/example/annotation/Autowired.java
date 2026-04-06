package org.example.annotation;

import java.lang.annotation.*;

/**
 * @author zhaoshuai
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    /**
     * Is required.
     */
    boolean value() default true;

    /**
     * Bean name if set.
     */
    String name() default "";
}
