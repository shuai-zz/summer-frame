package org.example.scan.destroy;

import jakarta.annotation.PreDestroy;
import org.example.annotation.Component;
import org.example.annotation.Value;

@Component
public class AnnotationDestroyBean {
    @Value("${app.title}")
    public String appTitle;

    @PreDestroy
    void destroy(){
        this.appTitle=null;
    }
}
