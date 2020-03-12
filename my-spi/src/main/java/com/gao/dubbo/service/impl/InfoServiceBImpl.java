package com.gao.dubbo.service.impl;


import com.gao.dubbo.service.InfoService;
import org.apache.dubbo.common.URL;

public class InfoServiceBImpl implements InfoService {

    @Override
    public Object sayHello(String name) {
        System.out.println(name+",你好，调通了B实现！");
        return name+",你好，调通了B实现！";
    }

    @Override
    public Object passInfo(String msg, URL url) {
        System.out.println("恭喜你，调通了B实现");
        return msg;
    }

}
