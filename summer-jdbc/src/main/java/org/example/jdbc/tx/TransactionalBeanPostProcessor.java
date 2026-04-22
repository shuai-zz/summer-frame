package org.example.jdbc.tx;

import org.example.annotation.Transactional;
import org.example.aop.AnnotationProxyBeanPostProcessor;

/**
 * @author zhaoshuai
 */
public class TransactionalBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Transactional> {

}
