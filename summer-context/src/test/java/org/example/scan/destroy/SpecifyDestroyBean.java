package org.example.scan.destroy;

public class SpecifyDestroyBean {
    public String appTitle;

    public SpecifyDestroyBean(String appTitle) {
        this.appTitle = appTitle;
    }

    void destroy(){
        this.appTitle = null;
    }
}
