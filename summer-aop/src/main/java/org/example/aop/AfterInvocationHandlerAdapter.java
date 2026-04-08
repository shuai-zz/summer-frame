package org.example.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author zhaoshuai
 */
public abstract class AfterInvocationHandlerAdapter implements InvocationHandler {
    public abstract Object after(Object proxy, Object returnValue, Method method, Object[] args);

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object ret=method.invoke(proxy, args);
        return after(proxy, ret, method, args);
    }
}
