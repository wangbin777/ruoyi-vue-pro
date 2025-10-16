package cn.iocoder.yudao.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ProtocolConverterUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 协议常量
    private static final byte MAGIC_NUMBER = (byte) 0x7E;
    private static final byte PROTOCOL_VERSION = (byte) 0x01;
    private static final byte REQUEST = (byte) 0x01;
    private static final byte RESPONSE = (byte) 0x02;
    private static final int HEADER_FIXED_LENGTH = 7;
    private static final int MIN_MESSAGE_LENGTH = HEADER_FIXED_LENGTH + 4;

    /**
     * 将JSON对象转换为二进制协议格式的字节数组
     * @param jsonObject 任意Java对象（会被序列化为JSON）
     * @param messageId 消息ID
     * @param method 方法名
     * @param messageType 消息类型（REQUEST=0x01, RESPONSE=0x02）
     * @return 符合二进制协议的字节数组
     */
    public static byte[] convertToBinaryMessage(Object jsonObject, String messageId, String method, byte messageType) {
        return convertToProtocolMessage(messageType, messageId, method, jsonObject);
    }

    /**
     * 生成心跳消息的字节数组
     * @return 心跳协议数据
     */
    public static byte[] createHeartbeatMessage() {
        Map<String, Object> heartbeatData = new HashMap<>();
        heartbeatData.put("type", "heartbeat");
        heartbeatData.put("timestamp", System.currentTimeMillis());
        return convertToBinaryMessage(heartbeatData, "heartbeat_" + System.currentTimeMillis(), "thing.heartbeat", REQUEST);
    }

    /**
     * 核心转换方法 - 编码为二进制协议
     * @param messageType 消息类型（REQUEST=0x01, RESPONSE=0x02）
     * @param messageId 消息ID
     * @param method 方法名
     * @param data 要转换的数据对象
     * @return 协议字节数组
     */
    private static byte[] convertToProtocolMessage(byte messageType, String messageId, String method, Object data) {
        try {
            // 1. 将对象转为JSON字符串
            String jsonStr = objectMapper.writeValueAsString(data);
            byte[] contentBytes = jsonStr.getBytes(StandardCharsets.UTF_8);

            // 2. 获取消息ID和方法名的字节数组
            byte[] messageIdBytes = messageId.getBytes(StandardCharsets.UTF_8);
            byte[] methodBytes = method.getBytes(StandardCharsets.UTF_8);

            // 3. 计算总长度
            int totalLength = HEADER_FIXED_LENGTH + 2 + messageIdBytes.length + 2 + methodBytes.length + contentBytes.length;

            // 4. 构造协议字节数组
            ByteBuffer buffer = ByteBuffer.allocate(totalLength);

            // 协议头
            buffer.put(MAGIC_NUMBER);                    // 1字节 魔术字
            buffer.put(PROTOCOL_VERSION);               // 1字节 版本号
            buffer.put(messageType);                    // 1字节 消息类型
            buffer.putInt(totalLength);                 // 4字节 消息长度

            // 消息ID
            buffer.putShort((short) messageIdBytes.length); // 2字节 消息ID长度
            buffer.put(messageIdBytes);                     // n字节 消息ID内容

            // 方法名
            buffer.putShort((short) methodBytes.length);    // 2字节 方法名长度
            buffer.put(methodBytes);                        // n字节 方法名内容

            // 消息体
            buffer.put(contentBytes);                       // n字节 JSON数据

            return buffer.array();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    /**
     * 解码二进制协议消息
     * @param bytes 协议字节数组
     * @return 解码后的消息对象，包含消息类型、消息ID、方法名和数据
     */
    public static ProtocolMessage decodeBinaryMessage(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("待解码数据不能为空");
        }
        if (bytes.length < MIN_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("数据包长度不足");
        }

        try {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            int index = 0;

            // 1. 验证魔术字
            byte magic = buffer.get();
            index++;
            if (magic != MAGIC_NUMBER) {
                throw new IllegalArgumentException("无效的协议魔术字: " + magic);
            }

            // 2. 验证版本号
            byte version = buffer.get();
            index++;
            if (version != PROTOCOL_VERSION) {
                throw new IllegalArgumentException("不支持的协议版本: " + version);
            }

            // 3. 读取消息类型
            byte messageType = buffer.get();
            index++;
            if (messageType != REQUEST && messageType != RESPONSE) {
                throw new IllegalArgumentException("无效的消息类型: " + messageType);
            }

            // 4. 读取消息长度
            int messageLength = buffer.getInt();
            index += 4;
            if (messageLength != bytes.length) {
                throw new IllegalArgumentException("消息长度不匹配，期望: " + messageLength + ", 实际: " + bytes.length);
            }

            // 5. 读取消息 ID
            short messageIdLength = buffer.getShort();
            index += 2;
            byte[] messageIdBytes = new byte[messageIdLength];
            buffer.get(messageIdBytes);
            index += messageIdLength;
            String messageId = new String(messageIdBytes, StandardCharsets.UTF_8);

            // 6. 读取方法名
            short methodLength = buffer.getShort();
            index += 2;
            byte[] methodBytes = new byte[methodLength];
            buffer.get(methodBytes);
            index += methodLength;
            String method = new String(methodBytes, StandardCharsets.UTF_8);

            // 7. 解析消息体
            byte[] contentBytes = new byte[bytes.length - index];
            System.arraycopy(bytes, index, contentBytes, 0, contentBytes.length);
            String jsonStr = new String(contentBytes, StandardCharsets.UTF_8);
            Object data = objectMapper.readValue(jsonStr, Object.class);

            return new ProtocolMessage(messageType, messageId, method, data);

        } catch (Exception e) {
            throw new RuntimeException("协议解码失败: " + e.getMessage(), e);
        }
    }

    /**
     * 简化的编码方法 - 用于常见业务场景
     * @param messageId 消息ID
     * @param method 方法名
     * @param data 数据对象
     * @return 协议字节数组
     */
    public static byte[] encodeForRequest(String messageId, String method, Object data) {
        return convertToBinaryMessage(data, messageId, method, REQUEST);
    }

    /**
     * 简化的编码方法 - 用于响应场景
     * @param messageId 消息ID
     * @param method 方法名
     * @param data 数据对象
     * @return 协议字节数组
     */
    public static byte[] encodeForResponse(String messageId, String method, Object data) {
        return convertToBinaryMessage(data, messageId, method, RESPONSE);
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * 数据转HEX
     * @param messageId 消息ID
     * @param method 方法名
     * @param object 数据对象
     * @return HEX字符串
     */
    public static String objToHex(String messageId, String method, Object object) {
        byte[] bytes = ProtocolConverterUtils.encodeForRequest(messageId, method, object);
        return bytesToHex(bytes);
    }

    /**
     * 协议消息封装类
     */
    public static class ProtocolMessage {
        private final byte messageType;
        private final String messageId;
        private final String method;
        private final Object data;

        public ProtocolMessage(byte messageType, String messageId, String method, Object data) {
            this.messageType = messageType;
            this.messageId = messageId;
            this.method = method;
            this.data = data;
        }

        public byte getMessageType() { return messageType; }
        public String getMessageId() { return messageId; }
        public String getMethod() { return method; }
        public Object getData() { return data; }

        public boolean isRequest() { return messageType == REQUEST; }
        public boolean isResponse() { return messageType == RESPONSE; }

        @Override
        public String toString() {
            return String.format("ProtocolMessage{type=%s, id='%s', method='%s', data=%s}",
                    isRequest() ? "REQUEST" : "RESPONSE", messageId, method, data);
        }
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        try {
            // 测试数据
            Map<String, Object> testData = new HashMap<>();
            testData.put("id", "1234567890");
            testData.put("version", "1.0");
            testData.put("method", "thing.property.post");
            testData.put("params", Map.of("temperature", 25.5, "humidity", 60));

            // 编码
            byte[] encoded = encodeForRequest("msg_001", "thing.property.post", testData);
            System.out.println("编码后HEX: " + bytesToHex(encoded));
            System.out.println("编码后长度: " + encoded.length + " 字节");

            // 解码
            ProtocolMessage decoded = decodeBinaryMessage(encoded);
            System.out.println("解码后: " + decoded);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}