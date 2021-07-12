package com.gao.dubbo.spi;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.remoting.Transporter;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.junit.Test;

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
        ProxyFactory PROXY_FACTORY = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        System.out.println(PROXY_FACTORY);
    }

    @Test
    public void getTransporter() {
        Transporter transporter = ExtensionLoader.getExtensionLoader(Transporter .class).getAdaptiveExtension();
        System.out.println(transporter);
    }

}
