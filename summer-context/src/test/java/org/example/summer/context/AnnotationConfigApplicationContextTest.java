package org.example.summer.context;

import org.example.context.AnnotationConfigApplicationContext;
import org.example.context.BeanDefinition;
import org.example.imported.LocalDateConfiguration;
import org.example.imported.ZonedDateConfiguration;
import org.example.io.PropertyResolver;
import org.example.scan.ScanApplication;
import org.example.scan.custom.annotation.CustomAnnotationBean;
import org.example.scan.nested.OuterBean;
import org.example.scan.primary.PersonBean;
import org.example.scan.primary.StudentBean;
import org.example.scan.primary.TeacherBean;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class AnnotationConfigApplicationContextTest {
    @Test
    public void testAnnotationConfigApplicationContext() {
        var ctx=new AnnotationConfigApplicationContext(ScanApplication.class, createPropertyResolver());

        // @CustomAnnotation
        assertNotNull(ctx.findBeanDefinition(CustomAnnotationBean.class));
        assertNotNull(ctx.findBeanDefinition("customAnnotation"));


        // @Import()
        // @Import():
        assertNotNull(ctx.findBeanDefinition(LocalDateConfiguration.class));
        assertNotNull(ctx.findBeanDefinition("startLocalDate"));
        assertNotNull(ctx.findBeanDefinition("startLocalDateTime"));
        assertNotNull(ctx.findBeanDefinition(ZonedDateConfiguration.class));
        assertNotNull(ctx.findBeanDefinition("startZonedDateTime"));
        // nested:
        assertNotNull(ctx.findBeanDefinition(OuterBean.class));
        assertNotNull(ctx.findBeanDefinition(OuterBean.NestedBean.class));

        BeanDefinition studentDef = ctx.findBeanDefinition(StudentBean.class);
        BeanDefinition teacherDef = ctx.findBeanDefinition(TeacherBean.class);
        // 2 PersonBean:
        List<BeanDefinition> defs = ctx.findBeanDefinitions(PersonBean.class);
        assertSame(studentDef, defs.getFirst());
        assertSame(teacherDef, defs.get(1));
        // 1 @Primary PersonBean:
        BeanDefinition personPrimaryDef = ctx.findBeanDefinition(PersonBean.class);
        assertSame(teacherDef, personPrimaryDef);
    }

    PropertyResolver createPropertyResolver() {
        var ps = new Properties();
        ps.put("app.title", "Scan App");
        ps.put("app.version", "v1.0");
        ps.put("jdbc.url", "jdbc:hsqldb:file:testdb.tmp");
        ps.put("jdbc.username", "sa");
        ps.put("jdbc.password", "");
        ps.put("convert.boolean", "true");
        ps.put("convert.byte", "123");
        ps.put("convert.short", "12345");
        ps.put("convert.integer", "1234567");
        ps.put("convert.long", "123456789000");
        ps.put("convert.float", "12345.6789");
        ps.put("convert.double", "123456789.87654321");
        ps.put("convert.localdate", "2023-03-29");
        ps.put("convert.localtime", "20:45:01");
        ps.put("convert.localdatetime", "2023-03-29T20:45:01");
        ps.put("convert.zoneddatetime", "2023-03-29T20:45:01+08:00[Asia/Shanghai]");
        ps.put("convert.duration", "P2DT3H4M");
        ps.put("convert.zoneid", "Asia/Shanghai");
        return new PropertyResolver(ps);
    }
}
