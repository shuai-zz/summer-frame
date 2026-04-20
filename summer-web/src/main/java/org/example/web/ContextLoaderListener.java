package org.example.web;

import com.itranswarp.summer.context.AnnotationConfigApplicationContext;
import com.itranswarp.summer.context.ApplicationContext;
import com.itranswarp.summer.exception.NestedRuntimeException;
import com.itranswarp.summer.io.PropertyResolver;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.example.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhaoshuai
 */
public class ContextLoaderListener implements ServletContextListener {
    final Logger logger= LoggerFactory.getLogger(getClass());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("init {}.", getClass().getName());
        ServletContext servletContext = sce.getServletContext();
        PropertyResolver propertyResolver = WebUtils.createPropertyResolver();
        String encoding = propertyResolver.getProperty("${summer.web.character-encoding:UTF-8}");
        servletContext.setRequestCharacterEncoding(encoding);
        servletContext.setResponseCharacterEncoding(encoding);
        ApplicationContext applicationContext = createApplicationContext(servletContext.getInitParameter("configuration"), propertyResolver);
        // register DispatcherServlet
        WebUtils.registerDispatcherServlet(servletContext, propertyResolver);

        servletContext.setAttribute("applicationContext", applicationContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if(sce.getServletContext().getAttribute("applicationContext") instanceof ApplicationContext applicationContext){
            applicationContext.close();
        }
    }

    ApplicationContext createApplicationContext(String configClassName, PropertyResolver propertyResolver){
        logger.info("init ApplicationContext by configuration: {}", configClassName);
        if(configClassName==null||configClassName.isEmpty()){
            throw new NestedRuntimeException("Cannot init ApplicationContext for missing init param name: configuration");
        }
        Class<?> configClass;
        try{
            configClass=Class.forName(configClassName);
        }catch (ClassNotFoundException e){
            throw new NestedRuntimeException("Could not load class from init param 'configuration': "+configClassName);
        }
        return new AnnotationConfigApplicationContext(configClass, propertyResolver);
    }
}
