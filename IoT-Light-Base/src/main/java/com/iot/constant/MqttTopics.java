package com.iot.constant;

/**
 * MQTT 主题常量与构建工具
 *
 * 主题规则与 IotMqttMessageServiceImpl 的路由保持一致:
 *   - lamp/{poleCode}            旧版传感器上报 (兼容)
 *   - lamp/{poleCode}/control    灯杆控制指令 (ESP32 订阅)
 *   - light/sensor/{lampId}/data 传感器数据 (新版)
 *   - light/pole/{poleCode}/status|heartbeat|offline
 *   - light/device/{deviceCode}/status|fault|result|command
 *   - light/camera/{code}/status|capture
 *   - light/control/{code}/log
 *   - light/alarm/rule/sync
 *
 * @author iot
 * @date 2026-08-16
 */
public class MqttTopics
{
  /** 业务主题前缀 (新版) */
  public static final String TOPIC_PREFIX = "light/";

  /** 旧版灯杆上报前缀: lamp/{poleCode} */
  public static final String TOPIC_LEGACY_PREFIX = "lamp/";

  /** 控制指令后缀 */
  public static final String TOPIC_CONTROL_SUFFIX = "/control";

  private MqttTopics()
  {
  }

  /**
   * 灯杆控制指令主题: lamp/{poleCode}/control
   * ESP32 固件订阅该主题, 消息 "1"=开灯, "0"=关灯
   */
  public static String buildPoleCommandTopic(String poleCode)
  {
    return "lamp/" + poleCode + "/control";
  }

  /**
   * 设备控制指令主题: light/device/{deviceCode}/command
   */
  public static String buildDeviceCommandTopic(String deviceCode)
  {
    return "light/device/" + deviceCode + "/command";
  }

  /**
   * 摄像头抓拍上报主题: light/camera/{cameraCode}/capture
   * Flask/AI 服务拍照后上报抓拍结果, 后端写入 iot_camera_capture
   */
  public static String buildCameraCaptureTopic(String cameraCode)
  {
    return "light/camera/" + cameraCode + "/capture";
  }

  /**
   * 摄像头抓拍触发命令主题: light/camera/{cameraCode}/command
   * 后端下发 {"action":"capture"} 通知 Flask/AI 服务触发拍照
   * (与上报主题分离, 避免抓拍触发被误当成抓拍记录)
   */
  public static String buildCameraCommandTopic(String cameraCode)
  {
    return "light/camera/" + cameraCode + "/command";
  }

  /**
   * 摄像头状态主题: light/camera/{cameraCode}/status
   */
  public static String buildCameraStatusTopic(String cameraCode)
  {
    return "light/camera/" + cameraCode + "/status";
  }

  /**
   * 灯杆状态主题: light/pole/{poleCode}/status
   */
  public static String buildPoleStatusTopic(String poleCode)
  {
    return "light/pole/" + poleCode + "/status";
  }

  /**
   * 灯杆心跳主题: light/pole/{poleCode}/heartbeat
   */
  public static String buildPoleHeartbeatTopic(String poleCode)
  {
    return "light/pole/" + poleCode + "/heartbeat";
  }
}
