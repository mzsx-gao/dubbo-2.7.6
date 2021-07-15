package com.gao.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.remoting.Transporter;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.RouterFactory;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 名称: Z_MyTest
 * 描述: Dubbo源码中涉及到的SPI
 *
 * @author gaoshudian
 * @date 7/12/21 3:10 PM
 */
public class Z_MyTest {

    @Test
    public void getProtocol() {
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        System.out.println(protocol);
    }

    @Test
    public void getProxyFactory() {
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        System.out.println(proxyFactory);
    }

    @Test
    public void getTransporter() {
        Transporter transporter = ExtensionLoader.getExtensionLoader(Transporter .class).getAdaptiveExtension();
        System.out.println(transporter);
    }

    @Test
    public void cluster() {
        Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();
        System.out.println(cluster);
    }

    @Test
    public void overrideUrl() {
        RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
        Registry registry = registryFactory.getRegistry(URL.valueOf("zookeeper://47.103.97.241:2181"));
        registry.register(URL.valueOf("override://0.0.0.0/org.apache.dubbo.demo.DemoService?category=configurators&compatible_config=true&dynamic=false&enabled=true&timeout=6600"));
    }

    @Test
    public void registryRoutes() {
        RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
        Registry registry = registryFactory.getRegistry(URL.valueOf("zookeeper://47.103.97.241:2181"));
        URL routeUrl = URL.valueOf("route://0.0.0.0/org.apache.dubbo.demo.DemoService?category=routers&dynamic=false&enabled=true&force=true&name=null&priority=0&router=condition&rule=+%3D%3E+host+%21%3D+192.168.15.1&runtime=false");
        registry.register(routeUrl);
    }

    @Test
    public void getRoutes() {
        URL url = URL.valueOf("consumer://172.19.7.245/org.apache.dubbo.demo.DemoService?" +
            "application=dubbo-demo-annotation-consumer&dubbo=2.0.2&init=false&" +
            "interface=org.apache.dubbo.demo.DemoService&metadata-type=remote&" +
            "methods=sayHello,sayHelloAsync&pid=74970&side=consumer&sticky=false&timestamp=1626339835181");
        List<RouterFactory> extensionFactories = ExtensionLoader.getExtensionLoader(RouterFactory.class)
            .getActivateExtension(url, "router");
        List<Router> routers = extensionFactories.stream()
            .map(factory -> factory.getRouter(url))
            .collect(Collectors.toList());
        System.out.println(routers);
    }
}