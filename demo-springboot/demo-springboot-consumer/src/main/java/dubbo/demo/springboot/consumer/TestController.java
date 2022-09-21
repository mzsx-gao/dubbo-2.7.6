package dubbo.demo.springboot.consumer;

import dubbo.demo.springboot.DemoService;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 名称: TestController
 * 描述: 测试
 *
 * @author gaoshudian
 * @date 4/26/22 11:33 PM
 */
@RestController
public class TestController {

    @Reference(version = "1.0.0",group = "dubbo")
    private DemoService demoService;

    @RequestMapping("/test")
    public String test(){
        return demoService.sayHello("hello world");
    }
}
