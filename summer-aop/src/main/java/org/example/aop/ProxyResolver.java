package org.example.aop;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;

/**
 * @author zhaoshuai
 * create proxy by subclassing and override methods with interceptor.
 */
public class ProxyResolver {
    final Logger logger= LoggerFactory.getLogger(getClass());
    // ByteBuddy Instance
    final ByteBuddy byteBuddy=new ByteBuddy();

    private static ProxyResolver INSTANCE=null;
    public static ProxyResolver getInstance(){
        if(INSTANCE==null){
            INSTANCE=new ProxyResolver();
        }
        return INSTANCE;
    }

    public ProxyResolver() {
    }

    // Take in the original Bean and the interceptor, returns the proxied instance
    @SuppressWarnings("unchecked")
    public <T> T createProxy(T bean, InvocationHandler handler){
        // class of target bean
        Class<?> targetClass = bean.getClass();
        logger.atDebug().log("create proxy for bean {} @{}", targetClass.getName(), Integer.toHexString(bean.hashCode()));
        // dynamically create the proxy class
        Class<?> proxyClass = this.byteBuddy
                // subclass use the default non-arg constructor
                .subclass(targetClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
                // intercept all public methods
                .method(ElementMatchers.isPublic()).intercept(InvocationHandlerAdapter.of(
                        // proxy method invoke
                        (proxy, method, args) -> {
                            // delegate to origin bean
                            return handler.invoke(bean, method, args);
                        }
                ))
                // generate bytecode
                .make()
                // load bytecode
                .load(targetClass.getClassLoader()).getLoaded();
        // create Proxy Instance
        Object proxy;
        try{
            proxy=proxyClass.getConstructor().newInstance();
        }catch (RuntimeException e){
            throw e;
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        return (T) proxy;
    }
}
