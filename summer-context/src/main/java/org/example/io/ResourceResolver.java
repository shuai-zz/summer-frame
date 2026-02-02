package org.example.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;



/**
 * A simple classpath scan works both in directory and jar.
 *
 * @author zhaoshuai
 */
public class ResourceResolver {
    Logger log = LoggerFactory.getLogger(getClass());

    String basePackage;

    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    // 获取扫描到的Resource
    public <R>List<R> scan(Function<Resource,R> mapper){
        String basePackagePath=this.basePackage.replace(".","/");
        String path=basePackagePath;
        try {
            List<R> collector=new ArrayList<>();
            scan0(basePackagePath,path,collector,mapper);
            return collector;
        }catch (IOException e){
            throw new UncheckedIOException(e);
        }catch (URISyntaxException e){
            throw new RuntimeException(e);
        }
    }

    <R> void scan0(String basePackagePath, String path, List<R> collector, Function<Resource,R> mapper) throws IOException, URISyntaxException {
        log.atDebug().log("scan path:{}",path);
        // 通过ClassLoader获取URL列表
        Enumeration<URL> en = getContextClassLoader().getResources(path);
        while (en.hasMoreElements()){
            URL url = en.nextElement();
            URI uri = url.toURI();
            String uriStr=removeTrailingSlash(uriToString(uri));
            String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());
            if(uriBaseStr.startsWith("file:")){
                // 在目录中搜索
                uriBaseStr = uriBaseStr.substring(5);
            }
            if(uriStr.startsWith("jar:")){
                // 在jar包中搜索
                scanFile(true,uriBaseStr,jarUriToPath(basePackagePath,uri),collector,mapper);
            }else{
                scanFile(false,uriBaseStr, Paths.get(uri),collector,mapper);
            }
        }
    }

    /**
     * ClassLoader首先从Thread.getContextClassLoader()获取，如果获取不到，再从当前Class获取
     * NOTE: 因为Web应用的ClassLoader不是JVM提供的基于Classpath的ClassLoader，而是Servlet容器提供的ClassLoader，它不在默认的Classpath搜索，而是在/WEB-INF/classes目录和/WEB-INF/lib的所有jar包搜索，从Thread.getContextClassLoader()可以获取到Servlet容器专属的ClassLoader；
     * @return ClassLoader: 一系列文件名
     * */
    ClassLoader getContextClassLoader(){
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if(cl==null){
            cl=getClass().getClassLoader();
        }
        return cl;
    }

    /**
     * 将Jar URI转换为Path
     * @param basePackagePath jakarta/annotation
     * @param jarUri jar:file:/Users/zhaoShuai/.m2/repository/jakarta/annotation/jakarta.annotation-api/2.1.1/jakarta.annotation-api-2.1.1.jar!/jakarta/annotation
     * @return jakarta/annotation
     * @throws IOException 抛出异常，如果无法将URI转换为Path
     */
    Path jarUriToPath(String basePackagePath, URI jarUri) throws IOException{
        return FileSystems.newFileSystem(jarUri, Map.of()).getPath(basePackagePath);
    }

    /**
     *
     * @param isJar if true
     * @param base /Users/zhaoShuai/Documents/workspace/mini-spring/summer-context/target/test-classes/
     * @param root /Users/zhaoShuai/Documents/workspace/mini-spring/summer-context/target/test-classes/jakarta/annotation
     * @param collector
     * @param mapper
     * @param <R>
     * @throws IOException
     */
    <R> void scanFile(boolean isJar, String base, Path root, List<R> collector, Function<Resource,R> mapper) throws IOException{
        String baseDir = removeTrailingSlash(base);
        Files.walk(root)
                .filter(Files::isRegularFile)
                .forEach(file->{
                    Resource res=null;
                    if(isJar){
                        res=new Resource(baseDir,removeLeadingSlash(file.toString()));
                    }else{
                        String path = file.toString();
                        String name = removeLeadingSlash(path.substring(baseDir.length()));
                        res=new Resource("file:"+path,name);
                    }
                    log.atDebug().log("found resource:{}", res);
                    R r=mapper.apply(res);
                    if(r!=null){
                        collector.add(r);
                    }
                });
    }


    String uriToString(URI uri) {
        // 兼容中文路径及符号
        return URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8);
    }

    String removeLeadingSlash(String s) {
        if (s.startsWith("/") || s.startsWith("\\")) {
            return s.substring(1);
        }
        return s;
    }
    String removeTrailingSlash(String s) {
        if (s.endsWith("/")|| s.endsWith("\\")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }
}
