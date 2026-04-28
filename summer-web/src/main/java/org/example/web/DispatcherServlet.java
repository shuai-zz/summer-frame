package org.example.web;


import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.annotation.PathVariable;
import org.example.annotation.RequestBody;
import org.example.annotation.RequestParam;
import org.example.annotation.ResponseBody;
import org.example.context.ApplicationContext;
import org.example.exception.ServerErrorException;
import org.example.exception.ServerWebInputException;
import org.example.io.PropertyResolver;
import org.example.utils.ClassUtils;
import org.example.web.utils.JsonUtils;
import org.example.web.utils.PathUtils;
import org.example.web.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zhaoshuai
 */
public class DispatcherServlet extends HttpServlet {
    final Logger logger = LoggerFactory.getLogger(getClass());
    ApplicationContext applicationContext;
    ViewResolver viewResolver;

    String resourcePath;
    String faviconPath;

    List<Dispatcher> getDispatchers = new ArrayList<>();
    List<Dispatcher> postDispatchers = new ArrayList<>();

    public DispatcherServlet(ApplicationContext applicationContext, PropertyResolver propertyResolver) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void destroy() {
        this.applicationContext.close();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.info("{} {}", req.getMethod(), req.getRequestURI());
        PrintWriter pw = resp.getWriter();
        pw.write("<h1>Hello, world!</h1>");
        pw.flush();
    }

    static class Dispatcher {
        final static Result NOT_PROCESSED = new Result(false, null);
        final Logger logger = LoggerFactory.getLogger(getClass());

        boolean isRest;
        boolean isResponseBody;
        boolean isVoid;
        Pattern urlPattern;
        Object controller;
        Method handlerMethod;
        Param[] methodParameters;

        public Dispatcher(String httpMethod, boolean isRest, Object controller, Method method, String urlPattern) throws ServletException {
            this.isRest = isRest;
            this.isResponseBody = method.getAnnotation(ResponseBody.class) != null;
            this.isVoid = method.getReturnType() == void.class;
            this.urlPattern = PathUtils.compile(urlPattern);
            this.controller = controller;
            this.handlerMethod = method;
            Parameter[] params = method.getParameters();
            Annotation[][] paramsAnnos = method.getParameterAnnotations();
            this.methodParameters = new Param[params.length];
            for (int i = 0; i < params.length; i++) {
                this.methodParameters[i] = new Param(httpMethod, method, params[i], paramsAnnos[i]);
            }
            logger.atDebug().log("mapping {} to handler {}.{}", urlPattern, controller.getClass().getSimpleName(), method.getName());
            if (logger.isDebugEnabled()) {
                for (Param p : this.methodParameters) {
                    logger.debug("> parameter: {}", p);
                }
            }
        }

        Result process(String url, HttpServletRequest request, HttpServletResponse response) throws Exception {
            Matcher matcher = urlPattern.matcher(url);
            if (matcher.matches()) {
                Object[] arguments=new Object[this.methodParameters.length];
                for (int i = 0; i < arguments.length; i++) {
                    Param param = methodParameters[i];
                    arguments[i]=switch(param.paramType){
                        case PATH_VARIABLE -> {
                            try{
                                String s = matcher.group(param.name);
                                yield convertToType(param.classType, s);
                            }catch (IllegalArgumentException e){
                                throw new ServerWebInputException("Path variable '"+param.name+"' not found.");
                            }
                        }
                        case REQUEST_BODY -> {
                            BufferedReader reader = request.getReader();
                            yield JsonUtils.readJson(reader, param.classType);
                        }
                        case REQUEST_PARAM -> {
                            String s = getOrDefault(request, param.name, param.defaultValue);
                            yield convertToType(param.classType, s);
                        }
                        case SERVLET_VARIABLE -> {
                            Class<?> classType = param.classType;
                            if(classType==HttpServletRequest.class){
                                yield request;
                            }else if(classType==HttpServletResponse.class){
                                yield response;
                            }else if(classType==HttpSession.class){
                                yield request.getSession();
                            }else if(classType==ServletContext.class){
                                yield request.getServletContext();
                            }else{
                                throw new ServerErrorException("Could not determine argument type: "+param.name);
                            }
                        }
                    };
                }
                Object result=null;
                try{
                     result = this.handlerMethod.invoke(this.controller, arguments);
                }catch (InvocationTargetException e){
                    Throwable t = e.getCause();
                    if(t instanceof Exception ex){
                        throw ex;
                    }
                    throw e;
                }catch (ReflectiveOperationException e){
                    throw new ServerErrorException(e);
                }
                return new Result(true, result);
            }
            return NOT_PROCESSED;
        }

        Object convertToType(Class<?> classType, String s) {
            if (classType == String.class) {
                return s;
            }else if (classType == Integer.class || classType == int.class) {
                return Integer.valueOf(s);
            }else if (classType == Long.class || classType == long.class) {
                return Long.valueOf(s);
            }else if (classType == Float.class || classType == float.class) {
                return Float.valueOf(s);
            }else if(classType == Double.class || classType == double.class){
                return Double.valueOf(s);
            }else if(classType == Boolean.class || classType == boolean.class){
                return Boolean.valueOf(s);
            }else if(classType==Byte.class || classType==byte.class){
                return Byte.valueOf(s);
            }else if(classType==Short.class || classType==short.class){
                return Short.valueOf(s);
            }else{
                throw new ServerErrorException("Could not determine argument type: "+classType);
            }
        }

        String getOrDefault(HttpServletRequest request, String name, String defaultValue){
            String s = request.getParameter(name);
            if(s==null){
                if(WebUtils.DEFAULT_PARAM_VALUE.equals(defaultValue)){
                    throw new ServerWebInputException("Request parameter '"+name+"' not found.");
                }
                return defaultValue;
            }
            return s;
        }
    }

    static enum ParamType {
        PATH_VARIABLE,
        REQUEST_PARAM,
        REQUEST_BODY,
        SERVLET_VARIABLE;
    }

    static class Param {

        String name;
        ParamType paramType;
        Class<?> classType;
        String defaultValue;

        public Param(String httpMethod, Method method, Parameter parameter, Annotation[] annotations) throws ServletException {
            PathVariable pv = ClassUtils.getAnnotation(annotations, PathVariable.class);
            RequestParam rp = ClassUtils.getAnnotation(annotations, RequestParam.class);
            RequestBody rb = ClassUtils.getAnnotation(annotations, RequestBody.class);
            //should only have 1 annotation
            int total = (pv == null ? 0 : 1) + (rp == null ? 0 : 1) + (rb == null ? 0 : 1);
            if (total > 1) {
                throw new ServletException("Annotation @PathVariable, @RequestParam and @RequestBody cannot be combined at method: " + method);
            }

            this.classType = parameter.getType();
            if (pv != null) {
                this.name = pv.value();
                this.paramType = ParamType.PATH_VARIABLE;
            } else if (rp != null) {
                this.name = rp.value();
                this.defaultValue = rp.defaultValue();
                this.paramType = ParamType.REQUEST_PARAM;
            } else if (rb != null) {
                this.paramType = ParamType.REQUEST_BODY;
            } else {
                this.paramType = ParamType.SERVLET_VARIABLE;
                // check servlet variable type
                if (this.classType != HttpServletRequest.class && this.classType != HttpServletResponse.class && this.classType != HttpSession.class && this.classType != ServletContext.class) {
                    throw new ServerErrorException("(Missing annotation?) Unsupported argument type: " + classType + " at method: " + method);
                }
            }
        }

        @Override
        public String toString() {
            return "Param [name=" + name + ", paramType=" + paramType + ", classType=" + classType + ", defaultValue=" + defaultValue + "]";
        }
    }

    static record Result(boolean processed, Object returnObject) {
    }
}
