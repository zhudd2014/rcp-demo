package com.test;

import lombok.Data;

import java.io.Serializable;

/**
 * 模拟器参数
 *
 * @author fanchunlin
 * @version 2016年3月22日
 * @see EmulatorRequest
 * @since 1.0
 */
@Data
public class EmulatorRequest implements Serializable{

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -7915158363901196233L;
    /**
     * env
     */
    private String env;
    /**
     * service
     */
    private String service;
    /**
     * ip
     */
    private String ip;
    /**
     * port
     */
    private Integer port;
    /**
     * group
     */
    private String group;
    /**
     * version
     */
    private String version;
    /**
     * method
     */
    private String method;
    /**
     * params
     */
    private String params;


}
