package com.test;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RequestStatisticService {

    @Autowired
    private EmulatorService emulatorService;

    public static final String serviceName = "com.fenqile.statistic.request.service.StatisticRequestService";
    public static final String method = "queryUserTrace";
    public static final String ip = "192.168.64.209";
    public static final int port = 30337;
    public static final String version = "1.0.0";
    public static final String group = "*";

    public void query(){
        EmulatorRequest request = new EmulatorRequest();
        request.setEnv("pre");
        request.setService(serviceName);
        request.setMethod(method);
//        request.setParams("{\"deptNo\":\"001\",\"goodsNos\":\"13241\",\"pageNo\":\"1\","
//            + "\"pageSize\":\"10\",\"isvGoodsNos\":\"1\"}");
        request.setParams("[\"O20171130800706102831\"]");
        request.setIp(ip);
        request.setPort(port);
        request.setGroup(group);
        request.setVersion(version);
        JSONObject jsonObject = emulatorService.request(request);
        System.out.println("###################jsonObject = " + jsonObject);
    }
}
