package com.gao.dubbo.filter;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

/**
 * 使用方传递了group = peter 则该Filter激活
 */
@Activate(group = "peter",order = 6)
public class FilterC implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        System.out.println("你好，调通了Filer C实现！");
        return null;
    }
}
