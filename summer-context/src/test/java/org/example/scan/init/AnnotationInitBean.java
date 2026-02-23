package org.example.scan.init;

import jakarta.annotation.PostConstruct;
import org.example.annotation.Component;
import org.example.annotation.Value;

@Component
public class AnnotationInitBean {
    @Value("${app.title}")
    String appTitle;

    @Value("${app.version}")
    String appVersion;

    public String appName;

    @PostConstruct
    void init(){
        appName = appTitle + " " + appVersion;
    }
}
