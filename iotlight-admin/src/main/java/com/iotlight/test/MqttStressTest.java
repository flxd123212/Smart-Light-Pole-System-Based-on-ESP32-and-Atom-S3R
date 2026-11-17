package com.iotlight.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * 智能灯杆系统 MQTT 压测工具
 * 运行方式: 直接运行 main 方法
 */
public class MqttStressTest {

  // ========== 配置区域 ==========
  // 修改为你的服务器IP
  private static final String BROKER = "tcp://localhost:1883";
  private static final int QOS = 1;
  private static final boolean RETAINED = false;

  // 灯杆列表（必须与数据库 iot_pole.pole_code 匹配）
  private static final String[] POLE_CODES = {
    "LP001", "LP002", "LP003", "LP004", "LP005",
    "LP006", "LP007", "LP008", "LP009", "LP010",
    "LP011", "LP012", "LP013", "LP014", "LP015"
  };
  // ==============================

  private static final Random random = new Random();
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  public static void main(String[] args) {
    printBanner();

    // 解析参数: 轮数 每轮灯杆数 间隔(ms)
    int totalRounds = args.length > 0 ? Integer.parseInt(args[0]) : 50;
    int polesPerRound = args.length > 1 ? Integer.parseInt(args[1]) : 10;
    long intervalMs = args.length > 2 ? Long.parseLong(args[2]) : 5000;
    String broker = args.length > 3 ? args[3] : BROKER;

    int actualPoles = Math.min(polesPerRound, POLE_CODES.length);

    printConfig(broker, actualPoles, totalRounds, intervalMs);

    try {
      runStressTest(broker, totalRounds, actualPoles, intervalMs);
    } catch (Exception e) {
      System.err.println("❌ 压测失败: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void runStressTest(String broker, int totalRounds, int polesCount, long intervalMs)
    throws Exception {

    MqttClient[] clients = new MqttClient[polesCount];
    String[] usedPoles = new String[polesCount];
    System.arraycopy(POLE_CODES, 0, usedPoles, 0, polesCount);

    try {
      // 1. 连接
      System.out.println("\n🔌 正在连接 MQTT Broker...");
      for (int i = 0; i < polesCount; i++) {
        String clientId = "stress-" + usedPoles[i] + "-" + System.currentTimeMillis();
        clients[i] = new MqttClient(broker, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(60);
        // 如果有认证，取消注释
        // options.setUserName("admin");
        // options.setPassword("public".toCharArray());

        clients[i].connect(options);
        System.out.println("   ✅ " + usedPoles[i] + " 连接成功");
      }

      // 2. 发送数据
      System.out.println("\n📤 开始发送模拟数据...\n");
      long startTime = System.currentTimeMillis();
      int totalSent = 0;

      for (int round = 1; round <= totalRounds; round++) {
        long roundStart = System.currentTimeMillis();

        for (int i = 0; i < polesCount; i++) {
          String topic = "lamp/" + usedPoles[i];
          String payload = buildSensorData(usedPoles[i]);

          MqttMessage message = new MqttMessage(payload.getBytes("UTF-8"));
          message.setQos(QOS);
          message.setRetained(RETAINED);

          clients[i].publish(topic, message);
          totalSent++;
        }

        long elapsed = System.currentTimeMillis() - roundStart;
        System.out.printf("   [轮 %3d/%d] 发送 %d 条, 耗时 %d ms, 累计 %d 条%n",
          round, totalRounds, polesCount, elapsed, totalSent);

        if (round < totalRounds) {
          Thread.sleep(intervalMs);
        }
      }

      // 3. 统计
      long totalTime = System.currentTimeMillis() - startTime;
      System.out.println("\n════════════════════════════════════════════════════════════");
      System.out.println("📊 压测统计:");
      System.out.printf("   │  总耗时: %d ms%n", totalTime);
      System.out.printf("   │  总发送: %d 条%n", totalSent);
      System.out.printf("   │  TPS:    %.2f%n", totalTime > 0 ? (double) totalSent / totalTime * 1000 : 0);
      System.out.println("════════════════════════════════════════════════════════════");

    } finally {
      // 4. 断开
      System.out.println("\n🔌 断开所有连接...");
      for (int i = 0; i < polesCount; i++) {
        if (clients[i] != null && clients[i].isConnected()) {
          try {
            clients[i].disconnect();
            clients[i].close();
          } catch (Exception e) {
            // ignore
          }
        }
      }
      System.out.println("✅ 压测完成!");
    }
  }

  /**
   * 构造符合文档 3.2 节的 JSON 数据
   */
  private static String buildSensorData(String poleCode) {
    ObjectNode json = mapper.createObjectNode();

    json.put("lampId", poleCode);
    json.put("temperature", round(20.0 + random.nextDouble() * 18.0, 1));   // 20-38°C
    json.put("humidity", round(30.0 + random.nextDouble() * 55.0, 1));     // 30-85%
    json.put("illumination", round(5.0 + random.nextDouble() * 33.0, 2));  // 5-38
    json.put("voltage", round(4.8 + random.nextDouble() * 0.4, 2));        // 4.8-5.2V
    json.put("current", round(0.008 + random.nextDouble() * 0.017, 3));    // 8-25mA
    json.put("status", random.nextBoolean() ? "1" : "0");
    json.put("_timestamp", sdf.format(new Date()));

    return json.toString();
  }

  private static double round(double value, int decimals) {
    double factor = Math.pow(10, decimals);
    return Math.round(value * factor) / factor;
  }

  private static void printBanner() {
    System.out.println("╔════════════════════════════════════════════════════════════╗");
    System.out.println("║           智能灯杆系统 MQTT 压测工具                       ║");
    System.out.println("║           模拟多灯杆上报传感器数据                         ║");
    System.out.println("╚════════════════════════════════════════════════════════════╝");
  }

  private static void printConfig(String broker, int poles, int rounds, long interval) {
    System.out.println("\n📋 压测配置:");
    System.out.println("   ┌─────────────────────────────────────────────┐");
    System.out.printf("   │  MQTT Broker:  %s%n", broker);
    System.out.printf("   │  灯杆数量:     %d 个%n", poles);
    System.out.printf("   │  总轮数:       %d 轮%n", rounds);
    System.out.printf("   │  轮间隔:       %d ms%n", interval);
    System.out.printf("   │  总消息数:     %d 条%n", rounds * poles);
    System.out.println("   └─────────────────────────────────────────────┘");
  }

  // ========== 辅助方法 ==========

  /**
   * 发送单条控制指令 (开/关灯)
   */
  public static void sendControl(String broker, String poleCode, boolean turnOn) throws Exception {
    String topic = "lamp/" + poleCode + "/control";
    String payload = turnOn ? "1" : "0";

    MqttClient client = new MqttClient(broker, "ctrl-" + System.currentTimeMillis(),
      new MemoryPersistence());
    client.connect();

    MqttMessage msg = new MqttMessage(payload.getBytes());
    msg.setQos(1);
    client.publish(topic, msg);

    System.out.println("📨 " + poleCode + " -> " + (turnOn ? "🟢 开灯" : "🔴 关灯"));
    client.disconnect();
    client.close();
  }

  /**
   * 模拟异常数据 (高温度)
   */
  public static void sendAbnormalData(String broker, String poleCode) throws Exception {
    ObjectNode json = mapper.createObjectNode();
    json.put("lampId", poleCode);
    json.put("temperature", 48.0 + random.nextDouble() * 5.0);
    json.put("humidity", 45.0);
    json.put("illumination", 20.0);
    json.put("voltage", 5.0);
    json.put("current", 0.018);
    json.put("status", "1");
    json.put("_timestamp", sdf.format(new Date()));

    MqttClient client = new MqttClient(broker, "abnormal-" + System.currentTimeMillis(),
      new MemoryPersistence());
    client.connect();

    MqttMessage msg = new MqttMessage(json.toString().getBytes("UTF-8"));
    msg.setQos(1);
    client.publish("lamp/" + poleCode, msg);

    System.out.println("🔥 发送异常数据: " + poleCode + " 温度 " + json.get("temperature").asDouble() + "°C");
    client.disconnect();
    client.close();
  }
}
