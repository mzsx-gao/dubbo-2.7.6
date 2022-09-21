package dubbo.demo.springboot.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class ConsumerApplication {

    public static void main(String[] args){
        SpringApplication.run(ConsumerApplication.class,args);
        System.out.println("consumer启动成功");
    }

}
