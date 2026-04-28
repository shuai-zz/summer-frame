package org.example.annotation;

import java.lang.annotation.*;

/**
 * @author zhaoshuai
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestBody {
}
