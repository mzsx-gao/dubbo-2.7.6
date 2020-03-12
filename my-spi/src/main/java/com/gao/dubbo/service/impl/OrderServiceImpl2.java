package com.gao.dubbo.service.impl;


import com.gao.dubbo.service.OrderService;
import com.gao.dubbo.service.PayService;
import org.apache.dubbo.common.URL;

public class OrderServiceImpl2 implements OrderService {

    private PayService payService;

    public void setPayService(PayService payService) {
        this.payService = payService;
    }

    @Override
    public String getDetail(String name, URL url) {
        payService.pay(100);
        System.out.println(name+",James订单处理成功！");
        return name+",你好，James订单处理成功！";
    }
}
