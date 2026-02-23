package org.example.annotation;

import java.lang.annotation.*;

/**
 * @author zhaoshuai
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ComponentScan {
    /**
     * Package names to scan.
     * Default to current package
     */
    String[] value() default {};
}
