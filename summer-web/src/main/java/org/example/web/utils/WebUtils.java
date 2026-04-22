package org.example.web.utils;


import jakarta.servlet.*;
import org.example.context.ApplicationContext;
import org.example.context.ApplicationContextUtils;
import org.example.io.PropertyResolver;
import org.example.utils.ClassPathUtils;
import org.example.utils.YamlUtils;
import org.example.web.DispatcherServlet;
import org.example.web.FilterRegistrationBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * @author zhaoshuai
 */
public class WebUtils {
    public static final String DEFAULT_BASE_PACKAGE="\0\t\0\t\0";
    static final Logger logger= LoggerFactory.getLogger(WebUtils.class);
    static final String CONFIG_APP_YAML="/application.yml";
    static final String CONFIG_APP_PROP="/application.properties";

    public static void registerDispatcherServlet(ServletContext servletContext, PropertyResolver propertyResolver){
        DispatcherServlet dispatcherServlet = new DispatcherServlet(ApplicationContextUtils.getRequiredApplicationContext(), propertyResolver);
        logger.info("register servlet {} for URL '/'", dispatcherServlet.getClass().getName());
        ServletRegistration.Dynamic dispatcherReg = servletContext.addServlet("dispatcherServlet", dispatcherServlet);
        dispatcherReg.addMapping("/");
        dispatcherReg.setLoadOnStartup(0);
    }

    public static void registerFilters(ServletContext servletContext){
        ApplicationContext applicationContext = ApplicationContextUtils.getRequiredApplicationContext();
        for (FilterRegistrationBean filterRegBean : applicationContext.getBeans(FilterRegistrationBean.class)) {
            List<String> urlPatterns = filterRegBean.getUrlPatterns();
            if(urlPatterns==null||urlPatterns.isEmpty()){
                throw new IllegalArgumentException("No url patterns for {}"+filterRegBean.getClass().getName());
            }
            Filter filter = Objects.requireNonNull(filterRegBean.getFilter(), "FilterRegistrationBean.getFilter() must not return null");
            logger.info("register filter '{}' {} for URLs: {}", filterRegBean.getName(), filter.getClass().getName(), String.join(", ",urlPatterns));
            var filterReg = servletContext.addFilter(filterRegBean.getName(), filter);
            filterReg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, urlPatterns.toArray(String[]::new));

        }
    }

    /**
     * Try load property resolver from /application.yml or /application.properties
     */
    public static PropertyResolver createPropertyResolver(){
        final Properties props=new Properties();
        // try load application.yml
        try{
            Map<String, Object> ymlMap = YamlUtils.loadYamlAsPlainMap(CONFIG_APP_YAML);
            logger.info("load config: {}", CONFIG_APP_YAML);
            for (String key : ymlMap.keySet()) {
                Object value = ymlMap.get(key);
                if(value instanceof String strValue){
                    props.put(key, strValue);
                }
            }
        }catch (UncheckedIOException e){
            if(e.getCause() instanceof FileNotFoundException){
                // try load application.properties
                ClassPathUtils.readInputStream(CONFIG_APP_PROP, inputStream -> {
                    logger.info("load config: {}", CONFIG_APP_PROP);
                    props.load(inputStream);
                    return true;
                });
            }
        }
        return new PropertyResolver(props);
    }
}
