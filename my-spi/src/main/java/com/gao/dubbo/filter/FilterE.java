package com.gao.dubbo.filter;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

/**
 * 使用方传递了group = peter或james，并且url中包含test参数，则该Filter激活
 */
@Activate(group = {"peter","james"},order = 1,value = "test")
public class FilterE implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        System.out.println("你好，调通了Filer E实现！");
        return null;
    }
}
