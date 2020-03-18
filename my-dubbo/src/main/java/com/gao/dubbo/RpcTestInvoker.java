package com.gao.dubbo;

import com.gao.dubbo.service.DemoService;
import com.gao.dubbo.service.impl.ZkDemoServiceImpl;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.config.annotation.Reference;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.junit.Test;
import java.io.IOException;

/**
 * 注册中心动态服务
 */
public class RpcTestInvoker {
    ExtensionLoader<Protocol> protocolLoader = ExtensionLoader.getExtensionLoader(Protocol.class);
    ExtensionLoader<ProxyFactory> proxyLoader = ExtensionLoader.getExtensionLoader(ProxyFactory.class);

    //注册中心服务--zk
    final URL registryUrl = URL.valueOf("registry://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService?registry=zookeeper");

    //支付的协议：dubbo,http,hessian,rmi
    URL serviceUrl = URL.valueOf("dubbo://127.0.0.1:9001/com.enjoy.service.DemoService");
    @Test
    public void serverRpc() throws IOException {
        DemoService service = new ZkDemoServiceImpl("peter");
        Protocol protocol = protocolLoader.getAdaptiveExtension();
        //动态代理
        ProxyFactory proxy = proxyLoader.getAdaptiveExtension();

        serviceUrl = serviceUrl.setPort(9001);
        serviceUrl = serviceUrl.addParameter("loadbalance","consistenthash");
        serviceUrl = serviceUrl.addParameter("cluster","failfast");
        URL newRegistryUrl = registryUrl.addParameter("export", serviceUrl.toFullString());
        //暴露服务
        Invoker<DemoService> serviceInvoker = proxy.getInvoker(service, DemoService.class, newRegistryUrl);
        Exporter<DemoService> exporter = protocol.export(serviceInvoker);
        System.out.println("server 启动协议："+serviceUrl.getProtocol());
        // 保证服务一直开着
        System.in.read();
        exporter.unexport();
    }

    @Reference(cluster = "failover")
    @Test
    public void serverRpc2() throws IOException {
        DemoService service = new ZkDemoServiceImpl("james");
        Protocol protocol = protocolLoader.getAdaptiveExtension();
        //动态代理
        ProxyFactory proxy = proxyLoader.getAdaptiveExtension();

        serviceUrl = serviceUrl.setPort(9002);
        serviceUrl = serviceUrl.addParameter("loadbalance","consistenthash");
        serviceUrl = serviceUrl.addParameter("cluster","failfast");
        URL newRegistryUrl = registryUrl.addParameter("export", serviceUrl.toFullString());
        //暴露服务
        Invoker<DemoService> serviceInvoker = proxy.getInvoker(service, DemoService.class, newRegistryUrl);
        Exporter<DemoService> exporter = protocol.export(serviceInvoker);
        System.out.println("server 启动协议："+serviceUrl.getProtocol());
        // 保证服务一直开着
        System.in.read();
        exporter.unexport();
    }

    @Test
    public void clientRpc() throws IOException {
        Protocol protocol = protocolLoader.getAdaptiveExtension();
        //动态代理
        ProxyFactory proxy = proxyLoader.getAdaptiveExtension();

        //消费服务
        serviceUrl = serviceUrl.addParameter("loadbalance","first");
        serviceUrl = serviceUrl.addParameter("cluster","failsms");
        Invoker<DemoService> referInvoker = protocol.refer(DemoService.class, registryUrl);
        DemoService service = proxy.getProxy(referInvoker);

        for (int i = 0;i < 5;i++){
            String result = service.sayHello(registryUrl.getProtocol()+"调用");
            System.out.println(result);
        }
        // 保证服务一直开着
//        System.in.read();

    }


}
