package com.gao.dubbo.service.proxyCodeDemo;

import java.lang.reflect.InvocationHandler;
import org.apache.dubbo.common.bytecode.ClassGenerator.DC;
import org.apache.dubbo.common.bytecode.Proxy;

public class Proxy0_ extends Proxy implements DC {
    public Object newInstance(InvocationHandler var1) {
        return new proxy0(var1);
    }

    public Proxy0_() {
    }
}
