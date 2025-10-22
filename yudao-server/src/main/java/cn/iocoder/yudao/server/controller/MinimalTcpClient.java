package cn.iocoder.yudao.server.controller;

import java.io.*;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;

/**
 * 最简TCP客户端 - 手动发送消息
 */
public class MinimalTcpClient {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8091;

        try (Socket socket = new Socket(host, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("连接TCP服务器成功!");
            System.out.println("输入'1' 发送认证信息, '2' 发送属性消息, '3' 发送状态消息, '4' 发送事件消息, 'quit' 退出");

            Random random = new Random();
            String input;
            String[] eventTypes = {"error", "alert", "info", "warning"};
            String eventType = eventTypes[random.nextInt(eventTypes.length)];

            while (!(input = scanner.nextLine()).equalsIgnoreCase("quit")) {
                String message = "";

                switch (input) {
                    case "1":
                        message = String.format(
                                "{\n" +
                                        "    \"version\": \"1.0\",\n" +
                                        "    \"method\": \"auth\",\n" +
                                        "    \"params\": {\n" +
                                        "        \"productKey\": \"o5ed5z0O1R8GuBhw\",\n" +
                                        "        \"deviceName\": \"cddevice\",\n" +
                                        "        \"clientId\": \"o5ed5z0O1R8GuBhw.cddevice\",\n" +
                                        "        \"username\": \"cddevice&o5ed5z0O1R8GuBhw\",\n" +
                                        "        \"password\": \"7432af9cb1b2acc3cc6beb1e9727247869cc63d2037f4fe24796f892120ef51b\"\n" +
                                        "    }\n" +
                                        "}",
                                random.nextInt(200000) + 2000000,
                                random.nextInt(10000) + 50000,
                                System.currentTimeMillis() / 1000
                        );
                        break;

                    case "2":
                        message = String.format(
                                "{\"version\":\"1.0\",\"method\":\"thing.property.post\",\"params\":{\"name\":%d,\"val\":%d,\"tss\":%d,\"qs\":0}}",
                                random.nextInt(200000) + 2000000,
                                random.nextInt(10000) + 50000,
                                System.currentTimeMillis() / 1000
                        );
                        break;

                    case "3":
                        message = String.format(
                                "{\"version\":\"1.0\",\"method\":\"thing.state.update\",\"params\":{\"state\":%d,\"timestamp\":%d}}",
                                random.nextInt(2),
                                System.currentTimeMillis()
                        );
                        break;

                    case "4":
                        message = String.format(
                                "{\"version\":\"1.0\",\"method\":\"thing.event.post\",\"id\":%d,\"params\":{" +
                                        "\"identifier\":\"%s\"," +
                                        "\"value\":{" +
                                        "\"ErrorCode\":%d," +
                                        "\"ErrorMessage\":\"%s\"," +
                                        "\"Timestamp\":%d}}}",
                                random.nextInt(1000),
                                eventType + "_event", // 事件标识符
                                random.nextInt(1000), // 错误码
                                "设备发生严重错误", // 错误信息
                                System.currentTimeMillis()
                        );
                        break;

                    default:
                        System.out.println("无效输入，请输入 1, 2, 3 或 quit");
                        continue;
                }

                writer.println(message);
                System.out.println("发送: " + message);
            }

            System.out.println("客户端退出");

        } catch (Exception e) {
            System.out.println("客户端异常: " + e.getMessage());
        }

    }

}
