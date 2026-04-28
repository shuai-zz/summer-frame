package org.example.web;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.example.context.AnnotationConfigApplicationContext;
import org.example.context.ApplicationContext;
import org.example.exception.NestedRuntimeException;
import org.example.io.PropertyResolver;
import org.example.web.utils.WebUtils;
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
        // register filters
        WebUtils.registerFilters(servletContext);

        servletContext.setAttribute("applicationContext", applicationContext);
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
