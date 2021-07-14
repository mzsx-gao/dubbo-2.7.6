package com.gao.dubbo.spi.adaptiveProxyCode;

import org.apache.dubbo.common.extension.ExtensionLoader;

public class InfoService$Adaptive implements com.gao.dubbo.service.InfoService {

    public java.lang.Object passInfo(java.lang.String arg0, org.apache.dubbo.common.URL arg1) {
        if (arg1 == null) throw new IllegalArgumentException("url == null");
        org.apache.dubbo.common.URL url = arg1;
        String extName = url.getParameter("info.service", "b");
        if (extName == null)
            throw new IllegalStateException("Failed to get extension (com.gao.dubbo.service.InfoService) name from " +
                "url (" + url.toString() + ") use keys([info.service])");
        com.gao.dubbo.service.InfoService extension =
            (com.gao.dubbo.service.InfoService) ExtensionLoader.getExtensionLoader(com.gao.dubbo.service.InfoService.class).getExtension(extName);
        return extension.passInfo(arg0, arg1);
    }

    public java.lang.Object sayHello(java.lang.String arg0) {
        throw new UnsupportedOperationException("The method public abstract java.lang.Object com.gao.dubbo.service" +
            ".InfoService.sayHello(java.lang.String) of interface com.gao.dubbo.service.InfoService is not adaptive " +
            "method!");
    }
}
