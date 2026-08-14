package com.iotlight.test;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttStressTest {
  public static void main(String[] args) {
    String broker = "tcp://localhost:1883";
    String clientId = "stress-test-client";

    try {
      MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());
      client.connect();
      System.out.println("====== 压测客户端连接成功 ======");

      // 模拟 10 轮，每轮 15 个灯杆
      for (int round = 1; round <= 10; round++) {
        for (int i = 1; i <= 15; i++) {
          String poleCode = String.format("LP%03d", i);
          String topic = "lamp/" + poleCode + "/data";

          // 构造 JSON 消息
          String payload = String.format(
            "{\"lampId\":\"%s\",\"temperature\":35.5,\"voltage\":220.5,\"current\":1.2,\"status\":1}",
            poleCode
          );

          MqttMessage message = new MqttMessage(payload.getBytes());
          message.setQos(1);
          client.publish(topic, message);
        }
        System.out.println("第 " + round + " 轮发送完成 (15条消息)");
        Thread.sleep(1000); // 每轮间隔 1 秒
      }

      client.disconnect();
      System.out.println("====== 压测结束 ======");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
