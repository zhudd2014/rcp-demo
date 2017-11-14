package com.fenqile.rpc.demo.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 模拟器参数
 *
 * @author fanchunlin
 * @version 2016年3月22日
 * @see ProtocolData
 * @since 1.0
 */
@Data
public class ProtocolData implements Serializable{

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -7915158363901196233L;
    /**
     * environment
     */
    private String environment;
    /**
     * service
     */
    private String service;
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
    

    /**
     * types
     */
    private String[] types = {};
    
}
