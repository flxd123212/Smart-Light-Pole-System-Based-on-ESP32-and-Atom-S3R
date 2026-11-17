package com.iot.service.impl;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.iot.constant.MqttTopics;
import com.iot.domain.IotControlLog;
import com.iot.mapper.IotControlLogMapper;
import com.iot.mapper.IotPoleMapper;
import com.iot.mapper.IotDeviceMapper;
import com.iot.domain.IotPole;
import com.iot.domain.IotDevice;
import com.iotlight.common.utils.DateUtils;
import com.iot.service.IIotMqttPublishService;

/**
 * MQTT 消息发布服务实现
 *
 * @author iot
 * @date 2026-08-04
 */
@Service
public class IotMqttPublishServiceImpl implements IIotMqttPublishService
{
  private static final Logger log = LoggerFactory.getLogger(IotMqttPublishServiceImpl.class);

  @Autowired
  private MqttClient mqttClient;

  @Autowired
  private IotControlLogMapper iotControlLogMapper;

  @Autowired
  private IotPoleMapper iotPoleMapper;

  @Autowired
  private IotDeviceMapper iotDeviceMapper;

  @Value("${spring.mqtt.qos:1}")
  private int qos;

  /**
   * 检查 MQTT 连接状态
   */
  @Override
  public boolean isConnected()
  {
    try
    {
      return mqttClient != null && mqttClient.isConnected();
    }
    catch (Exception e)
    {
      log.error("检查MQTT连接状态异常", e);
      return false;
    }
  }

  /**
   * 发送灯杆控制命令
   * 消息格式: {"cmd":"on"} 或 {"cmd":"off"} 或 {"cmd":"dim","brightness":80}
   * 兼容格式: "1" 或 "0"
   */
  @Override
  public boolean sendPoleCommand(String poleCode, String command, Integer value)
  {
    // 1. 检查连接状态
    if (!isConnected())
    {
      log.warn("MQTT 服务未连接，无法发送命令: poleCode={}", poleCode);
      recordControlLog(poleCode, null, command, "1", "MQTT服务未连接");
      return false;
    }

    try
    {
      // 2. 构建消息体（兼容ESP32固件格式）
      String payload;
      if ("on".equals(command))
      {
        payload = "1";  // ESP32 识别 "1" 为开灯
      }
      else if ("off".equals(command))
      {
        payload = "0";  // ESP32 识别 "0" 为关灯
      }
      else if ("dim".equals(command) && value != null)
      {
        // 调光（需固件支持PWM）
        JSONObject json = new JSONObject();
        json.put("cmd", "dim");
        json.put("brightness", value);
        payload = json.toJSONString();
      }
      else
      {
        // 通用格式
        JSONObject json = new JSONObject();
        json.put("cmd", command);
        if (value != null)
        {
          json.put("value", value);
        }
        payload = json.toJSONString();
      }

      // 3. 构建Topic并发送
      String topic = MqttTopics.buildPoleCommandTopic(poleCode);
      boolean success = publish(topic, payload);

      // 4. 记录控制日志
      recordControlLog(poleCode, null, command, success ? "0" : "1",
        success ? null : "MQTT消息发送失败");

      return success;
    }
    catch (Exception e)
    {
      log.error("发送灯杆控制命令失败: poleCode={}, command={}", poleCode, command, e);
      recordControlLog(poleCode, null, command, "1", e.getMessage());
      return false;
    }
  }

  /**
   * 发送设备控制命令
   */
  @Override
  public boolean sendDeviceCommand(String deviceCode, String command)
  {
    if (!isConnected())
    {
      log.warn("MQTT 服务未连接，无法发送命令: deviceCode={}", deviceCode);
      recordControlLog(null, deviceCode, command, "1", "MQTT服务未连接");
      return false;
    }

    try
    {
      String topic = MqttTopics.buildDeviceCommandTopic(deviceCode);
      boolean success = publish(topic, command);

      recordControlLog(null, deviceCode, command, success ? "0" : "1",
        success ? null : "MQTT消息发送失败");

      return success;
    }
    catch (Exception e)
    {
      log.error("发送设备控制命令失败: deviceCode={}", deviceCode, e);
      recordControlLog(null, deviceCode, command, "1", e.getMessage());
      return false;
    }
  }

  /**
   * 触发摄像头抓拍
   * 通过MQTT通知摄像头抓拍（替代HTTP轮询方式）
   */
  @Override
  public boolean triggerCameraCapture(String cameraCode)
  {
    if (!isConnected())
    {
      log.warn("MQTT 服务未连接，无法触发抓拍: cameraCode={}", cameraCode);
      recordControlLog(null, cameraCode, "capture", "1", "MQTT服务未连接");
      return false;
    }

    try
    {
      JSONObject json = new JSONObject();
      json.put("action", "capture");
      json.put("timestamp", System.currentTimeMillis());

      // 触发命令走 command 主题, 与抓拍上报主题 (light/camera/{code}/capture) 分离
      String topic = MqttTopics.buildCameraCommandTopic(cameraCode);
      boolean success = publish(topic, json.toJSONString());

      recordControlLog(null, cameraCode, "capture", success ? "0" : "1",
        success ? null : "MQTT消息发送失败");

      return success;
    }
    catch (Exception e)
    {
      log.error("触发摄像头抓拍失败: cameraCode={}", cameraCode, e);
      recordControlLog(null, cameraCode, "capture", "1", e.getMessage());
      return false;
    }
  }

  /**
   * 发送自定义主题消息
   */
  @Override
  public boolean publish(String topic, String payload)
  {
    try
    {
      if (!isConnected())
      {
        log.warn("MQTT 客户端未连接，无法发送消息: topic={}", topic);
        return false;
      }

      MqttMessage message = new MqttMessage(payload.getBytes());
      message.setQos(qos);
      message.setRetained(false);

      mqttClient.publish(topic, message);
      log.info("MQTT 消息发送成功: topic={}, payload={}", topic, payload);
      return true;
    }
    catch (Exception e)
    {
      log.error("MQTT 消息发送失败: topic={}, payload={}", topic, payload, e);
      return false;
    }
  }

  // ==================== 私有辅助方法 ====================

  /**
   * 记录控制日志
   *
   * @param poleCode 灯杆编码（可选）
   * @param deviceCode 设备编码（可选）
   * @param command 控制命令
   * @param result 执行结果：0成功 / 1失败
   * @param errorMessage 错误信息（可选）
   */
  private void recordControlLog(String poleCode, String deviceCode, String command,
                                String result, String errorMessage)
  {
    try
    {
      IotControlLog controlLog = new IotControlLog();

      // 查询灯杆ID
      if (poleCode != null && !poleCode.isEmpty())
      {
        IotPole pole = iotPoleMapper.selectIotPoleByPoleCode(poleCode);
        if (pole != null)
        {
          controlLog.setPoleId(pole.getPoleId());
        }
      }

      // 查询设备ID
      if (deviceCode != null && !deviceCode.isEmpty())
      {
        IotDevice device = iotDeviceMapper.selectIotDeviceByDeviceName(deviceCode);
        if (device != null)
        {
          controlLog.setDeviceId(device.getDeviceId());
          if (controlLog.getPoleId() == null && device.getPoleId() != null)
          {
            controlLog.setPoleId(device.getPoleId());
          }
        }
      }

      controlLog.setControlType(getControlType(command));
      controlLog.setResult(result);
      controlLog.setErrorMessage(errorMessage);
      controlLog.setCreateBy("system");
      controlLog.setCreateTime(DateUtils.getNowDate());

      iotControlLogMapper.insertIotControlLog(controlLog);
      log.debug("控制日志记录完成: poleCode={}, deviceCode={}, command={}, result={}",
        poleCode, deviceCode, command, result);
    }
    catch (Exception e)
    {
      log.error("记录控制日志失败: {}", e.getMessage(), e);
    }
  }

  /**
   * 获取控制类型编码（与数据库 iot_control_log.control_type 对应）
   * 0-开灯 1-关灯 2-重启 3-抓拍 4-调光 9-其他
   */
  private String getControlType(String command)
  {
    if (command == null) return "9";

    switch (command)
    {
      case "on":
      case "开灯":
        return "0";
      case "off":
      case "关灯":
        return "1";
      case "restart":
      case "重启":
        return "2";
      case "capture":
      case "抓拍":
        return "3";
      case "dim":
      case "调光":
        return "4";
      default:
        return "9";
    }
  }
}
