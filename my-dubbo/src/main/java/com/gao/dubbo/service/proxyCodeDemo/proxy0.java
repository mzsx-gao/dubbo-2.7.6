package com.gao.dubbo.service.proxyCodeDemo;

import com.alibaba.dubbo.rpc.service.EchoService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import com.gao.dubbo.service.DemoService;
import org.apache.dubbo.common.bytecode.ClassGenerator.DC;
import org.apache.dubbo.rpc.service.Destroyable;

public class proxy0 implements DC, Destroyable, EchoService, DemoService {

    public static Method[] methods;
    private InvocationHandler handler;

    public String sayHello(String var1){
        Object[] var2 = new Object[]{var1};
        Object var3 = null;
        try {
            var3 = this.handler.invoke(this, methods[0], var2);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return (String) var3;
    }

    public CompletableFuture sayHelloAsync(String var1) {
        Object[] var2 = new Object[]{var1};
        Object var3 = null;
        try {
            var3 = this.handler.invoke(this, methods[1], var2);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return (CompletableFuture) var3;
    }

    public void $destroy() {
        Object[] var1 = new Object[0];
        try {
            this.handler.invoke(this, methods[2], var1);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public Object $echo(Object var1) {
        Object[] var2 = new Object[]{var1};
        Object var3 = null;
        try {
            var3 = this.handler.invoke(this, methods[3], var2);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return (Object) var3;
    }

    public proxy0() {
    }

    public proxy0(InvocationHandler var1) {
        this.handler = var1;
    }
}
