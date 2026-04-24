package org.example.web;

import freemarker.cache.TemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.*;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.exception.ServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhaoshuai
 */
public class FreeMarkerViewResolver implements ViewResolver{

    final Logger logger= LoggerFactory.getLogger(getClass());
    final String templatePath;
    final String templateEncoding;
    final ServletContext servletContext;

    Configuration config;

    public FreeMarkerViewResolver(String templatePath, String templateEncoding, ServletContext servletContext) {
        this.templatePath = templatePath;
        this.templateEncoding = templateEncoding;
        this.servletContext = servletContext;
    }

    @Override
    public void init() {
        logger.info("init {}, set template path: {}", getClass().getSimpleName(), this.templatePath);
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        cfg.setDefaultEncoding(this.templateEncoding);
        cfg.setTemplateLoader(new ServletTemplateLoader(this.servletContext, this.templatePath));
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        cfg.setLocalizedLookup(false);

        var ow =new DefaultObjectWrapper(Configuration.VERSION_2_3_32);
        ow.setExposeFields(true);
        cfg.setObjectWrapper(ow);
        this.config=cfg;
    }

    @Override
    public void render(String viewName, Map<String, Object> model, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Template template=null;
        try{
            template=this.config.getTemplate(viewName);
        }catch (Exception e){
            throw new ServerErrorException("View not found: "+viewName);
        }
        PrintWriter pw = resp.getWriter();
        try{
            template.process(model, pw);
        }catch (TemplateException e){
            throw new ServerErrorException(e);
        }
        pw.flush();
    }
}

/**
 * copied from freemarker.cache.WebappTemplateLoader and modified to use
 * jakarta.servlet.ServletContext
 * Because it is used old javax.servlet.ServletContext
 */
class ServletTemplateLoader implements TemplateLoader {
    final Logger logger= LoggerFactory.getLogger(getClass());
    private final ServletContext servletContext;
    private final String subDirPath;

    public ServletTemplateLoader(ServletContext servletContext, String subDirPath) {
        Objects.requireNonNull(servletContext);
        Objects.requireNonNull(subDirPath);

        subDirPath=subDirPath.replace('\\', '/');
        if(!subDirPath.endsWith("/")){
            subDirPath+="/";
        }
        if(!subDirPath.startsWith("/")){
            subDirPath="/"+subDirPath;
        }
        this.servletContext = servletContext;
        this.subDirPath = subDirPath;
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
        String fullPath=subDirPath+name;
        try{
            String realPath=servletContext.getRealPath(fullPath);
            logger.atDebug().log("load template {}: real path: {}", name, realPath);
            if (realPath != null){
                File file = new File(realPath);
                if(file.canRead()&&file.isFile()){
                    return file;
                }
            }
        }catch (SecurityException e){
            // ignore
        }
        return null;
    }

    @Override
    public long getLastModified(Object templateSource) {
        if(templateSource instanceof File){
            return ((File) templateSource).lastModified();
        }
        return 0;
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        if(templateSource instanceof File){
            return new InputStreamReader(new FileInputStream((File) templateSource), encoding);
        }
        throw new IOException("File not found.");
    }

    @Override
    public void closeTemplateSource(Object o) throws IOException {
    }

    public Boolean getUrlConnectionUsesCaches(){
        return Boolean.FALSE;
    }
}
