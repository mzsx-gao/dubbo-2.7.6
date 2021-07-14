package org.apache.dubbo.demo;

/**
 * 名称: DemoServiceMock
 * 描述: 服务降级
 *
 * @author gaoshudian
 * @date 2021/7/14 22:50
 */
public class DemoServiceMock implements DemoService {

    @Override
    public String sayHello(String name) {
        return "容错数据";
    }
}
