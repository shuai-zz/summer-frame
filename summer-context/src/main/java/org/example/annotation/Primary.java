package org.example.annotation;

import java.lang.annotation.*;

/**
 * @author zhaoshuai
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Primary {
}
