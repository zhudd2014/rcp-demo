package com.test;

import com.alibaba.fastjson.JSONObject;
import org.springframework.context.support.ClassPathXmlApplicationContext;

@SuppressWarnings("resource")
public class Main {


    public static void main(String[] args) {

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                new String[] { "META-INF/spring/dubbo-provider.xml" });
        context.start();
        System.out.println("服务启动成功!");


        EmulatorRequest request = new EmulatorRequest();
        request.setEnv("project");
        request.setService("com.test.work.auth.activate.credit.service.CreditService");
        request.setMethod("getUserRcStepStatus");
//        request.setParams("{\"deptNo\":\"001\",\"goodsNos\":\"13241\",\"pageNo\":\"1\","
//            + "\"pageSize\":\"10\",\"isvGoodsNos\":\"1\"}");
        request.setParams("[3012550]");
        request.setIp("10.1.48.55");
        request.setPort(30160);
        request.setGroup("project_1014855");
        request.setVersion("2.0.0");
        EmulatorService emulatorService = context.getBean(EmulatorService.class);
        JSONObject jsonObject = emulatorService.request(request);
        System.out.println("###################jsonObject = " + jsonObject);
    }

}
