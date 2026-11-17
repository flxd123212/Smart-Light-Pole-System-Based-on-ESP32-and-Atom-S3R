package com.iot.service;

/**
 * MQTT消息处理Service接口
 *
 * @author extrao
 * @date 2026-07-24
 */
public interface IIotMqttMessageService
{
  /**
   * 处理设备上报消息
   *
   * @param topic 主题
   * @param payload 消息体
   */
  public void handleDeviceMessage(String topic, String payload);
}
