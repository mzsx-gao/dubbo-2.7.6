package com.gao.dubbo.service.impl;


import com.gao.dubbo.service.InfoService;
import com.gao.dubbo.service.OrderService;
import org.apache.dubbo.common.URL;

public class OrderServiceImpl implements OrderService {

    private InfoService infoService;//是dubbo的扩展点，是spring的bean接口

    public void setInfoService(InfoService infoService) {
        this.infoService = infoService;
    }

    @Override
    public String getDetail(String name, URL url) {
        infoService.passInfo(name,url);
        System.out.println(name+",Peter订单处理成功！");
        return name+",你好，Peter订单处理成功！";
    }
}
