package com.fenqile.rpc.demo;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.alibaba.dubbo.common.io.Bytes;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.net.InetAddresses;
import com.fenqile.rpc.demo.vo.EmulatorRequest;
import com.fenqile.rpc.demo.vo.ProtocolData;

@Service
public class EmulatorService {

    /**
     * 日志
     */
    private Logger logger = LoggerFactory.getLogger(EmulatorService.class);
    /**
     * APPLICATION_NAME
     */
    private static final String APPLICATION_NAME = "service_emulator_server";
    /**
     * APPLICATION_OWNER
     */
    private static final String APPLICATION_OWNER = "tomfan";
    /**
     * APPLICATION_ORGANIZATION
     */
    private static final String APPLICATION_ORGANIZATION = "fql";
    /**
     * DEFAULT_GROUP
     */
    private static final String DEFAULT_GROUP = "default";
    /**
     * DEFAULT_VERSION
     */
    private static final String DEFAULT_VERSION = "1.0.0";
    /**
     * HEADER_LENGTH
     */
    protected static final int HEADER_LENGTH = 32;
    /**
     * MAGIC
     */
    protected static final short MAGIC = (short) 0xdabb;

    /**
     * MAGIC_HIGH
     */
    protected static final byte MAGIC_HIGH = Bytes.short2bytes(MAGIC)[0];
    /**
     * MAGIC_LOW
     */
    protected static final byte MAGIC_LOW = Bytes.short2bytes(MAGIC)[1];

    /**
     * cmd高位字节 request
     */
    protected static final byte FSOF_REQUEST = (byte) 0x01;

    /**
     * cmd高位字节 response
     */
    protected static final byte FSOF_RESPONSE = (byte) 0x02;

    /**
     * cmd高位字节 request_one_way
     */
    protected static final byte FSOF_REQUEST_ONEWAY = (byte) 0x03;

    /**
     * cmd高位字节 EVENT
     */
    protected static final byte FSOF_EVENT_HIGH = (byte) 0x04;

    /**
     * EVENT低位字节 心跳EVENT
     */
    protected static final byte FSOF_EVENT_LOW_HEARTBEAT = (byte) 0x01;

    /**
     * FSOF_EVENT_LOW_READONLY
     */
    protected static final byte FSOF_EVENT_LOW_READONLY = (byte) 0x02;

    /**
     * DEFAULT_CRC
     */
    protected static final int DEFAULT_CRC = 0;

    /**
     * frame protocol version
     */
    protected static final byte PROTOCOL_VERSION = 0x01;

    /**
     * frame version
     */
    protected static final byte FRAME_VERSION = 0x01;

    /**
     * frame预留16位
     */
    protected static final short FRAME_LOW_DEFAULT = 0x0000;

    /**
     * APPLICATION_NAME
     */
    private ApplicationConfig applicationConfig;

    /**
     * applicationConfig
     */
    @PostConstruct
    private void applicationConfig() {
        // 当前应用配置
        applicationConfig = new ApplicationConfig();
        applicationConfig.setName(APPLICATION_NAME);
        applicationConfig.setOwner(APPLICATION_OWNER);
        applicationConfig.setOrganization(APPLICATION_ORGANIZATION);
    }

    public JSONObject request(EmulatorRequest request) {
        logger.debug("EmulatorServiceImpl request begin.....");
        checkRequest(request);
        JSONObject jsonObject;

        String resultString = socketRequest(request);
        try {
            jsonObject = JSON.parseObject(resultString);
        }
        catch (ClassCastException e) {
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("data", resultString);
            jsonObject = new JSONObject(resultMap);
        }

        return jsonObject;
    }

    /**
     * 请求
     *
     * @param request
     *            request
     * @return String
     * @see
     */
    private String socketRequest(EmulatorRequest request) {
        String resultStr = "";
        InputStream in = null;
        Socket socket = null;
        try {
            socket = new Socket(InetAddresses.forString(request.getIp()), request.getPort());
            // 超时时间
            socket.setSoTimeout(8000);

            ProtocolData data = getProtocolData(request);
            String jsonStr = JSON.toJSONString(data);
            logger.info("ProtocolData = {}" + jsonStr);

            // 替换掉params 有转义符 会导致问题
            JSONObject json = JSON.parseObject(jsonStr);

            if (StringUtils.isNotBlank(request.getParams())) {
                JSONArray array = JSONArray.parseArray(request.getParams());
                json.put("params", array);
            }
            jsonStr = StringEscapeUtils.unescapeJson(JSON.toJSONString(json));

            byte[] body = jsonStr.getBytes("utf-8");

            // create header
            byte[] header = getHeader(body.length);
            byte[] requestByte = new byte[header.length + body.length];
            System.arraycopy(header, 0, requestByte, 0, header.length);
            System.arraycopy(body, 0, requestByte, header.length, body.length);

            ByteBuffer byteBuffer = ByteBuffer.wrap(requestByte);
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            socket.getOutputStream().write(byteBuffer.array());

            socket.getOutputStream().flush();

            in = socket.getInputStream();

            // TODO 头也不一定一次能读完，但是几率很小， 先不更改, 有问题再说
            byte[] resultHeader = new byte[32];

            if (in.read(resultHeader) > 0) {

                // 内容长度
                // TODO 可能有的数据包错误或者攻击导致读到的数据内容非常长，系统会直接挂掉，应该有预防机制
                int dataLength = getDataLength(resultHeader);

                // TODO 每次都创建新的byte数组， 如果对象很大，会直接进入老年代，引发老年代GC,
                // 后续优化，能复用就复用，或者动态扩容和缩容
                byte[] resultData = new byte[dataLength];

                int totalReaded = 0;
                int readed = 0;
                int readLength = dataLength;

                // 一次可能读不完所有的内容，读不到总长度的话就等下一次，直到读到指定长度
                while ((readed = in.read(resultData, totalReaded, readLength)) != -1) {
                    totalReaded += readed;
                    readLength = dataLength - totalReaded;

                    if (totalReaded == dataLength) {
                        resultStr = new String(resultData, "utf-8");

                        logger.info("resultStr:" + resultStr);

                        return resultStr;

                    }
                }
            }

        }
        catch (Exception e) {
            logger.error("请求异常", e);
            throw new RuntimeException("socketRequest error," + e.getMessage(), e);
        }
        finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                    logger.debug("关闭socket...");
                }
                catch (IOException e) {
                    logger.error("关闭socket异常", e);
                }
            }
        }

        return resultStr;
    }

    /**
     * getDataLength
     *
     * @param resultHeader resultHeader
     * @return dataLength
     * @see
     */
    private int getDataLength(byte[] resultHeader) {
        // 根据协议从第四位开始读取一个整形，就是数据的长度
        return Bytes.bytes2int(resultHeader, 4);
    }

    /**
     * 获取协议头
     *
     * @param length
     *            包体长度
     * @return byte[]
     * @see
     */
    private byte[] getHeader(int length) {
        byte[] header = new byte[HEADER_LENGTH];
        // set magic , header offset 0-1
        Bytes.short2bytes(MAGIC, header);
        // set cmd , header offset 2-3
        // set request type
        header[2] = FSOF_REQUEST;
        // set len , header offset 4-7
        Bytes.int2bytes(length, header, 4);
        // set crc , header offset 8-11
        Bytes.int2bytes(DEFAULT_CRC, header, 8);
        // set frame , header offset 12-15
        header[12] = PROTOCOL_VERSION;
        header[13] = FRAME_VERSION;
        Bytes.short2bytes(FRAME_LOW_DEFAULT, header, 14);

        // set sn , header offset 16-19
        int sn = RandomUtils.nextInt(10000, 60000);
        Bytes.int2bytes(sn, header, 16);
        // set serialization , header offset 20
        header[20] = 1;
        return header;
    }

    /**
     * 获取协议包体
     *
     * @param request
     *            request
     * @return 包体数据
     * @see
     */
    private ProtocolData getProtocolData(EmulatorRequest request) {
        ProtocolData data = new ProtocolData();

        data.setEnvironment(request.getEnv());
        data.setGroup(request.getGroup());
        data.setMethod(request.getMethod());
        if (StringUtils.isNotBlank(request.getVersion())) {
            data.setVersion(request.getVersion());
        }
        else {
            data.setVersion(DEFAULT_VERSION);
        }
        if (StringUtils.isNotBlank(request.getGroup())) {
            data.setGroup(request.getGroup());
        }
        else {
            data.setGroup(DEFAULT_GROUP);
        }
        data.setService(request.getService());
        data.setParams(request.getParams());

        return data;
    }

    /**
     * 参数验证
     *
     * @param request
     *            request
     * @see
     */
    private void checkRequest(EmulatorRequest request) {
        Assert.notNull(request, "EmulatorRequest can not be null");
        Assert.hasText(request.getEnv(), "env can not be null");
        Assert.hasText(request.getIp(), "ip can not be null");
        Assert.notNull(request.getPort(), "port can not be null");
        Assert.notNull(request.getService(), "service can not be null");
        Assert.notNull(request.getMethod(), "method can not be null");
    }
}