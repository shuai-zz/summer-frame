package com.example.summer.around;

import org.example.annotation.Autowired;
import org.example.annotation.Component;
import org.example.annotation.Order;

@Order(0)
@Component
public class OtherBean {
    public OriginBean originBean;
    public OtherBean(@Autowired OriginBean originBean){
        this.originBean = originBean;
    }
}
