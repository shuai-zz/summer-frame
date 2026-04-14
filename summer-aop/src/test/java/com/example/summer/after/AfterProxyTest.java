package com.example.summer.after;

import org.example.context.AnnotationConfigApplicationContext;
import org.example.io.PropertyResolver;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AfterProxyTest {
    @Test
    public void testAfterProxy(){
        try(var ctx=new AnnotationConfigApplicationContext(AfterApplication.class, createPropertyResolver())){
            GreetingBean proxy = ctx.getBean(GreetingBean.class);
            // should change return value;
            assertEquals("Hello, Bob!", proxy.hello("Bob"));
            assertEquals("Morning, Alice!", proxy.morning("Alice"));
        }
    }
    PropertyResolver createPropertyResolver(){
        return new PropertyResolver(new Properties());
    }
}
