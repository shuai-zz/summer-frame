package org.example.annotation;

import java.lang.annotation.*;

/**
 * @author zhaoshuai
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PostMapping {
    /**
     * URL mapping
     */
    String value();
}
