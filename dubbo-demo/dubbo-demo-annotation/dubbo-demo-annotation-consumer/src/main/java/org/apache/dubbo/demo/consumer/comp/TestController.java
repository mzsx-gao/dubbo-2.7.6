package org.apache.dubbo.demo.consumer.comp;

import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.demo.DemoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 名称: TestController
 * 描述: 测试
 *
 * @author gaoshudian
 * @date 2021/7/15 20:09
 */
@RestController
public class TestController {

    @Reference
    private DemoService demoService;

    @RequestMapping("/test")
    public String test(String name){
        return demoService.sayHello(name);
    }
}
