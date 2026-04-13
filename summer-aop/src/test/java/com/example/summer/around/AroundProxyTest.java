package com.example.summer.around;

import org.example.context.AnnotationConfigApplicationContext;
import org.example.io.PropertyResolver;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.introspector.Property;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class AroundProxyTest {

    @Test
    public void testAroundProxy(){
        try(var ctx=new AnnotationConfigApplicationContext(AroundApplication.class, createPropertyResolver())){
            OriginBean proxy = ctx.getBean(OriginBean.class);
            // OriginBean$ByteBuddy$8NoD1FcQ
            System.out.println(proxy.getClass().getName());

            // proxy class, not origin class
            assertNotSame(OriginBean.class, proxy.getClass());
            // proxy.name not injected
            assertNull(proxy.name);

            assertEquals("Hello, Bob!", proxy.hello());
            assertEquals("Morning, Bob.", proxy.morning());

            // test injected proxy
            OtherBean other=ctx.getBean(OtherBean.class);
            assertSame(proxy, other.originBean);
            assertEquals("Hello, Bob!", other.originBean.hello());
        }
    }

    PropertyResolver createPropertyResolver(){
        Properties ps = new Properties();
        ps.put("customer.name", "Bob");
        return new PropertyResolver(ps);
    }
}
