package org.example.annotation;

import com.itranswarp.summer.annotation.Component;

import java.lang.annotation.*;

/**
 * @author zhaoshuai
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RestController {
    /**
     * Bean name. Default to simple class name with first-letter-lowercase
     */
    String value() default "";
}
