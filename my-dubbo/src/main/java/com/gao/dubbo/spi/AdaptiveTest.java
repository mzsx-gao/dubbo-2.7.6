package com.gao.dubbo.spi;

import com.gao.dubbo.service.InfoService;
import com.gao.dubbo.service.OrderService;
import javassist.ClassPool;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Protocol;
import org.junit.Test;

/**
 * SPI解决了实例的加载问题，并对实现 配置了健值对的形式进行管理
 * 但是,实际工作中，我们希望实例能懒加载，或者运行期再抉择
 * 如：InfoService的实现有三种，具体要用哪一个，在编译期未知(延迟加载考虑，只有在运行期实际需要时再加载)
 * 自适应注解Adaptive解决这个选择问题
 *
 * 注解在类上时，直接选此类为适配类（一个接口只允许一个类标记）
 * 注解在方法上时，只为此方法生成代理逻辑（方法必须有参数为url或者参数有返回url的方法）
 */
public class AdaptiveTest {

    /**
     * C实现类上有@Adaptive,选择C实现
     */
    @Test
    public void test1(){
        ExtensionLoader<InfoService> loader = ExtensionLoader.getExtensionLoader(InfoService.class);
        InfoService adaptiveExtension = loader.getAdaptiveExtension();
        URL url = URL.valueOf("dubbo://192.168.0.101:20880/XxxService");
        System.out.println(adaptiveExtension.passInfo("james", url));
    }

    /**
     * 各个实现类上面没有@Adaptive,接口某个方法上有@Adaptive ,SPI上有注解@SPI("b")
     * 1.URL中有具体的值info.service=a,则以URL为准，选择A实现
     * 2.接口方法中注解@Adaptive({"InfoService"}),URL中有具体的值InfoService=c,则以URL中的InfoService参数为准，选择C实现
     * 3.url无参数,则以@SPI("b")为准，选择b实现
     *
     * 原理:
     * 因为生成的代理类中的寻找具体的拓展类的代码如下:
     * String extName = url.getParameter("info.service", "b");
     * 其中info.service是默认根据类名转换而来，如果接口方法的@Adaptive注解有value值，则info.service就是该值
     */
    @Test
    public void test2(){
        ExtensionLoader<InfoService> loader = ExtensionLoader.getExtensionLoader(InfoService.class);
        InfoService adaptiveExtension = loader.getAdaptiveExtension();
        URL url = URL.valueOf("dubbo://192.168.0.101:20880/XxxService?info.service=a");
//        URL url = URL.valueOf("dubbo://192.168.0.101:20880/XxxService?InfoService=c");
//        URL url = URL.valueOf("dubbo://192.168.0.101:20880/XxxService");
        System.out.println(adaptiveExtension.passInfo("james", url));
    }

    /**
     * 测试dubbo的依赖注入
     */
    @Test
    public void iocSPI() {
        //获取OrderService的 Loader 实例
        ExtensionLoader<OrderService> loader = ExtensionLoader.getExtensionLoader(OrderService.class);
        //取得默认拓展类
        OrderService orderService = loader.getDefaultExtension();
        URL url = URL.valueOf("test://localhost/test?info.service=b");
        orderService.getDetail("peter",url);
    }

}
