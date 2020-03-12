package com.gao.dubbo.service;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;

@SPI("peter")
public interface OrderService {

    String getDetail(String id, URL url);
}
