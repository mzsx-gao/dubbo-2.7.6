package com.gao.dubbo.service.impl;


import com.gao.dubbo.service.InfoService;
import com.gao.dubbo.service.OrderService;
import org.apache.dubbo.common.URL;

public class OrderServiceImpl implements OrderService {

    private InfoService infoService;//是dubbo的扩展点，是spring的bean接口

    /**
     * 这里会进行dubbo的依赖注入，注入的是InfoService的自适应扩展类实现，例如：
     * package com.gao.dubbo.service;
     * import org.apache.dubbo.common.extension.ExtensionLoader;
     * public class InfoService$Adaptive implements com.gao.dubbo.service.InfoService {
     *      public java.lang.Object sayHello(java.lang.String arg0)  {
     *          throw new UnsupportedOperationException("The method public abstract java.lang.Object com.gao.dubbo.service
     *          .InfoService.sayHello(java.lang.String) of interface com.gao.dubbo.service.InfoService is not adaptive method!");
     *      }
     *      public java.lang.Object passInfo(java.lang.String arg0, org.apache.dubbo.common.URL arg1)  {
     *          if (arg1 == null) throw new IllegalArgumentException("url == null");
     *          org.apache.dubbo.common.URL url = arg1;
     *          String extName = url.getParameter("info.service", "b");
     *          if(extName == null) throw new IllegalStateException("Failed to get extension (com.gao.dubbo.service
     *          .InfoService) name from url (" + url.toString() + ") use keys([info.service])");
     *          com.gao.dubbo.service.InfoService extension = (com.gao.dubbo.service.InfoService)ExtensionLoader
     *          .getExtensionLoader(com.gao.dubbo.service.InfoService.class).getExtension(extName);
     *          return extension.passInfo(arg0, arg1);
     *      }
     * }
     * 最终去调用infoService的方法时，在根据url的参数选择具体用哪个实现类
     */
    public void setInfoService(InfoService infoService) {
        this.infoService = infoService;
    }

    @Override
    public String getDetail(String name, URL url) {
        infoService.passInfo(name,url);
        System.out.println(name+",Peter订单处理成功！");
        return name+",你好，Peter订单处理成功！";
    }
}
