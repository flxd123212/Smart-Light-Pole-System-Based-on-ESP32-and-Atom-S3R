package com.iot.service;

/**
 * MQTT 消息发布服务接口
 *
 * @author iot
 * @date 2026-08-04
 */
public interface IIotMqttPublishService
{
  /**
   * 发送灯杆控制命令
   *
   * @param poleCode 灯杆编码（对应 iot_pole.pole_code）
   * @param command 命令类型：on-开灯, off-关灯, dim-调光
   * @param value 命令值（调光时为亮度值 0-100）
   * @return 是否发送成功
   */
  boolean sendPoleCommand(String poleCode, String command, Integer value);

  /**
   * 发送设备控制命令
   *
   * @param deviceCode 设备编码
   * @param command 命令内容 JSON 字符串
   * @return 是否发送成功
   */
  boolean sendDeviceCommand(String deviceCode, String command);

  /**
   * 触发摄像头抓拍
   *
   * @param cameraCode 摄像头编码
   * @return 是否发送成功
   */
  boolean triggerCameraCapture(String cameraCode);

  /**
   * 发送自定义主题消息
   *
   * @param topic 主题
   * @param payload 消息内容
   * @return 是否发送成功
   */
  boolean publish(String topic, String payload);

  /**
   * 检查 MQTT 连接状态
   *
   * @return true-已连接, false-未连接
   */
  boolean isConnected();
}
