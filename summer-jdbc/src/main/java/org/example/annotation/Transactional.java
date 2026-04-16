package org.example.annotation;

import java.lang.annotation.*;

/**
 * @author zhaoshuai
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Transactional {
    String value() default "platformTransactionManager";
}
