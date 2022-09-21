/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.demo.DemoService;
import org.apache.dubbo.demo.consumer.comp.DemoServiceComponent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Random;

public class Application {

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(ConsumerConfiguration.class);
        context.start();
        System.out.println("客户端启动成功");
        while (true) {
            String req = String.valueOf(new Random().nextInt(2));
            DemoService service = context.getBean("demoServiceComponent", DemoServiceComponent.class);
            try{
                String response = service.sayHello(req);
                System.out.println("返回结果 :" + response);
            }catch (Exception e){
                e.printStackTrace();
            }
            Thread.sleep(1000 * 60);
        }
    }

    @Configuration
    @EnableDubbo(scanBasePackages = "org.apache.dubbo.demo.consumer.comp")
    @PropertySource("classpath:/spring/dubbo-consumer.properties")
    @ComponentScan(value = {"org.apache.dubbo.demo.consumer.comp"})
    static class ConsumerConfiguration {

//        @Bean
//        public RegistryConfig registryConfig() {
//            RegistryConfig registryConfig = new RegistryConfig();
//            registryConfig.setAddress("zookeeper://127.0.0.1:2181");
//            registryConfig.setSimplified(true);
//            return registryConfig;
//        }
//
//        @Bean
//        public MetadataReportConfig metadataReportConfig() {
//            MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
//            metadataReportConfig.setAddress("zookeeper://127.0.0.1:2181");
//            return metadataReportConfig;
//        }

    }
}
