package org.example.jdbc.tx;

import com.itranswarp.summer.aop.AnnotationProxyBeanPostProcessor;
import org.example.annotation.Transactional;

/**
 * @author zhaoshuai
 */
public class TransactionalBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Transactional> {

}
