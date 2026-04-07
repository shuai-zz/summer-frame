package com.example.summer.aop;

import org.example.aop.ProxyResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProxyResolverTest {
    @Test
    public void testProxyResolver(){
        OriginBean origin = new OriginBean();
        origin.name="Bob";

        Assertions.assertEquals("Hello, Bob.", origin.hello());

        // create proxy:
        OriginBean proxy = new ProxyResolver().createProxy(origin, new PoliteInvocationHandler());

        // Proxy Classname, similar to OriginBean$ByteBuddy$9hQwRy3T
        System.out.println(proxy.getClass().getName());

        // proxy class, not origin class
        Assertions.assertNotSame(OriginBean.class, proxy.getClass());
        // proxy.name is null
        Assertions.assertNull(proxy.name);

        // with @Polite
        Assertions.assertEquals("Hello, Bob!", proxy.hello());
        // without @Polite
        Assertions.assertEquals("Morning, Bob.", proxy.morning());
    }
}
