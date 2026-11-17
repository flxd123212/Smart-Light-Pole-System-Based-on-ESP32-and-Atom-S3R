package com.iot.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.iot.domain.IotAlarmRecord;
import com.iot.domain.IotAlarmRule;
import com.iot.domain.IotCameraCapture;
import com.iot.domain.IotControlLog;
import com.iot.domain.IotDevice;
import com.iot.domain.IotPole;
import com.iot.domain.IotSensorData;
import com.iot.domain.IotSensorMessage;
import com.iot.mapper.IotAlarmRecordMapper;
import com.iot.mapper.IotAlarmRuleMapper;
import com.iot.mapper.IotCameraCaptureMapper;
import com.iot.mapper.IotControlLogMapper;
import com.iot.mapper.IotDeviceMapper;
import com.iot.mapper.IotPoleMapper;
import com.iot.mapper.IotSensorDataMapper;
import com.iot.service.IIotMqttMessageService;
import com.iotlight.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * MQTT 消息处理服务实现
 * 处理设备通过 MQTT 上报的各种消息
 *
 * @author iot
 * @date 2026-08-13
 */
@Service
public class IotMqttMessageServiceImpl implements IIotMqttMessageService
{
  private static final Logger log = LoggerFactory.getLogger(IotMqttMessageServiceImpl.class);

  @Autowired
  private IotPoleMapper iotPoleMapper;

  @Autowired
  private IotDeviceMapper iotDeviceMapper;

  @Autowired
  private IotSensorDataMapper iotSensorDataMapper;

  @Autowired
  private IotAlarmRuleMapper iotAlarmRuleMapper;

  @Autowired
  private IotAlarmRecordMapper iotAlarmRecordMapper;

  @Autowired
  private IotCameraCaptureMapper iotCameraCaptureMapper;

  @Autowired
  private IotControlLogMapper iotControlLogMapper;

  /**
   * 处理设备上报消息
   * 由 MQTT 回调触发
   *
   * @param topic 主题
   * @param payload 消息体 JSON
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public void handleDeviceMessage(String topic, String payload)
  {
    log.info("收到MQTT消息 topic={}, payload={}", topic, payload);

    try
    {
      // 旧版兼容: lamp/xxx 主题
      if (topic.startsWith("lamp/"))
      {
        String poleCode = topic.substring("lamp/".length());
        if (!poleCode.isEmpty() && !poleCode.contains("/"))
        {
          processSensorData(poleCode, payload);
        }
        return;
      }

      // 新版主题路由: light/xxx/yyy/zzz
      if (topic.startsWith("light/"))
      {
        routeLightTopic(topic, payload);
        return;
      }

      log.debug("忽略的主题: {}", topic);
    }
    catch (Exception e)
    {
      log.error("处理MQTT消息异常, topic={}, payload={}", topic, payload, e);
    }
  }

  /**
   * 路由 light/ 开头的主题
   *
   * 支持的主题格式:
   * - light/sensor/{lampId}/data        传感器数据
   * - light/pole/{poleCode}/status      灯杆状态
   * - light/pole/{poleCode}/heartbeat   灯杆心跳
   * - light/device/{deviceCode}/status  设备状态
   * - light/device/{deviceCode}/fault   设备故障
   * - light/device/{deviceCode}/result  设备执行结果
   * - light/camera/{code}/status        摄像头状态
   * - light/camera/{code}/capture      摄像头抓拍
   * - light/control/{code}/log         控制日志
   * - light/alarm/rule/sync            报警规则同步
   */
  private void routeLightTopic(String topic, String payload)
  {
    String[] parts = topic.split("/");
    if (parts.length < 3)
    {
      log.warn("light主题格式不正确: {}", topic);
      return;
    }

    String module = parts[1];

    switch (module)
    {
      case "sensor":
        handleSensorTopic(parts, topic, payload);
        break;
      case "pole":
        handlePoleTopic(parts, topic, payload);
        break;
      case "device":
        handleDeviceTopic(parts, topic, payload);
        break;
      case "camera":
        handleCameraTopic(parts, topic, payload);
        break;
      case "control":
        handleControlTopic(parts, topic, payload);
        break;
      case "alarm":
        handleAlarmTopic(parts, topic, payload);
        break;
      default:
        log.debug("未知的light模块主题: {}", topic);
    }
  }

  // ==================== 传感器数据 ====================

  private void handleSensorTopic(String[] parts, String topic, String payload)
  {
    if (parts.length < 4)
    {
      log.warn("传感器主题格式不正确: {}", topic);
      return;
    }
    String lampId = parts[2];
    String action = parts[3];

    if ("data".equals(action))
    {
      // 确保灯杆存在
      getOrCreatePole(lampId, null);
      processSensorData(lampId, payload);
    }
    else
    {
      log.debug("未知的传感器操作: {}", action);
    }
  }

  /**
   * 处理传感器数据
   */
  private void processSensorData(String poleCode, String payload)
  {
    try
    {
      IotSensorMessage sensorMsg = JSON.parseObject(payload, IotSensorMessage.class);
      if (sensorMsg == null)
      {
        log.warn("解析JSON失败: payload={}", payload);
        return;
      }

      // 查询或创建灯杆
      IotPole pole = getOrCreatePole(poleCode, null);

      // 保存传感器数据
      IotSensorData sensorData = new IotSensorData();
      sensorData.setPoleId(pole.getPoleId());
      sensorData.setTemperature(toBigDecimal(sensorMsg.getTemperature(), -1));
      sensorData.setHumidity(toBigDecimal(sensorMsg.getHumidity(), -1));
      sensorData.setIllumination(toBigDecimal(sensorMsg.getIllumination()));
      sensorData.setVoltage(toBigDecimal(sensorMsg.getVoltage()));
      sensorData.setCurrent(toBigDecimal(sensorMsg.getCurrent()));
      sensorData.setCollectTime(DateUtils.getNowDate());
      iotSensorDataMapper.insertIotSensorData(sensorData);

      // 更新灯杆 LED 状态
      String status = sensorMsg.getStatus();
      if (status != null && !status.isEmpty())
      {
        IotPole updatePole = new IotPole();
        updatePole.setPoleId(pole.getPoleId());
        updatePole.setLedStatus(status);
        updatePole.setStatus("0");
        iotPoleMapper.updateIotPoleLedStatus(updatePole);
      }

      // 检查报警规则
      checkAlarmRules(pole.getPoleId(), sensorMsg);

      log.info("传感器数据处理完成: poleCode={}, temp={}, voltage={}",
          poleCode, sensorMsg.getTemperature(), sensorMsg.getVoltage());
    }
    catch (Exception e)
    {
      log.error("处理传感器数据失败: poleCode={}, payload={}", poleCode, payload, e);
    }
  }

  // ==================== 灯杆主题 ====================

  private void handlePoleTopic(String[] parts, String topic, String payload)
  {
    if (parts.length < 4)
    {
      log.warn("灯杆主题格式不正确: {}", topic);
      return;
    }
    String poleCode = parts[2];
    String action = parts[3];

    switch (action)
    {
      case "status":
        handlePoleStatus(poleCode, payload);
        break;
      case "heartbeat":
        handlePoleHeartbeat(poleCode);
        break;
      case "offline":
        handlePoleOffline(poleCode, payload);
        break;
      case "command":
        log.debug("灯杆命令(设备执行结果): poleCode={}", poleCode);
        break;
      default:
        log.debug("未知的灯杆操作: {}", action);
    }
  }

  private void handlePoleStatus(String poleCode, String payload)
  {
    try
    {
      JSONObject json = JSON.parseObject(payload);
      IotPole pole = getOrCreatePole(poleCode, json.getString("poleName"));

      IotPole updatePole = new IotPole();
      updatePole.setPoleId(pole.getPoleId());
      updatePole.setStatus("0");

      if (json.containsKey("ledStatus"))
      {
        updatePole.setLedStatus(json.getString("ledStatus"));
      }
      iotPoleMapper.updateIotPole(updatePole);
      log.info("灯杆状态已更新: poleCode={}", poleCode);
    }
    catch (Exception e)
    {
      log.error("处理灯杆状态失败: poleCode={}", poleCode, e);
    }
  }

  private void handlePoleHeartbeat(String poleCode)
  {
    try
    {
      IotPole pole = getOrCreatePole(poleCode, null);

      IotPole updatePole = new IotPole();
      updatePole.setPoleId(pole.getPoleId());
      updatePole.setStatus("0");
      iotPoleMapper.updateIotPole(updatePole);
      log.debug("灯杆心跳更新: poleCode={}", poleCode);
    }
    catch (Exception e)
    {
      log.error("处理灯杆心跳失败: poleCode={}", poleCode, e);
    }
  }

  /**
   * 处理灯杆离线消息 (LWT 遗嘱消息)
   * Flask 异常断开时, MQTT Broker 自动发布此消息
   */
  private void handlePoleOffline(String poleCode, String payload)
  {
    try
    {
      IotPole pole = iotPoleMapper.selectIotPoleByPoleCode(poleCode);
      if (pole == null)
      {
        log.warn("灯杆离线消息, 但灯杆不存在: poleCode={}", poleCode);
        return;
      }

      // 标记灯杆状态为离线
      IotPole updatePole = new IotPole();
      updatePole.setPoleId(pole.getPoleId());
      updatePole.setStatus("1"); // 1=离线
      iotPoleMapper.updateIotPole(updatePole);

      // 标记该灯杆下所有设备为离线
      IotDevice query = new IotDevice();
      query.setPoleId(pole.getPoleId());
      List<IotDevice> devices = iotDeviceMapper.selectIotDeviceList(query);
      for (IotDevice device : devices)
      {
        IotDevice updateDevice = new IotDevice();
        updateDevice.setDeviceId(device.getDeviceId());
        updateDevice.setStatus("1"); // 1=离线
        iotDeviceMapper.updateIotDevice(updateDevice);
      }

      log.warn("灯杆离线: poleCode={}, 关联 {} 个设备已标记为离线", poleCode, devices.size());
    }
    catch (Exception e)
    {
      log.error("处理灯杆离线失败: poleCode={}", poleCode, e);
    }
  }

  // ==================== 设备主题 ====================

  private void handleDeviceTopic(String[] parts, String topic, String payload)
  {
    if (parts.length < 4)
    {
      log.warn("设备主题格式不正确: {}", topic);
      return;
    }
    String deviceCode = parts[2];
    String action = parts[3];

    switch (action)
    {
      case "status":
        handleDeviceStatus(deviceCode, payload);
        break;
      case "fault":
        handleDeviceFault(deviceCode, payload);
        break;
      case "result":
        handleDeviceResult(deviceCode, payload);
        break;
      case "command":
        log.debug("设备命令(设备执行结果): deviceCode={}", deviceCode);
        break;
      default:
        log.debug("未知的设备操作: {}", action);
    }
  }

  private void handleDeviceStatus(String deviceCode, String payload)
  {
    try
    {
      JSONObject json = JSON.parseObject(payload);
      String lampId = json.getString("lampId");
      String deviceType = json.getString("deviceType");

      // 自动创建设备（如果不存在）
      IotDevice device = getOrCreateDevice(deviceCode, lampId, deviceType != null ? deviceType : "0");

      // 更新设备状态
      IotDevice updateDevice = new IotDevice();
      updateDevice.setDeviceId(device.getDeviceId());
      updateDevice.setStatus(json.getString("status") != null ? json.getString("status") : "0");
      iotDeviceMapper.updateIotDevice(updateDevice);

      log.info("设备状态已更新: deviceCode={}, status={}", deviceCode, updateDevice.getStatus());
    }
    catch (Exception e)
    {
      log.error("处理设备状态失败: deviceCode={}", deviceCode, e);
    }
  }

  private void handleDeviceFault(String deviceCode, String payload)
  {
    try
    {
      JSONObject json = JSON.parseObject(payload);
      String lampId = json.getString("lampId");

      // 先尝试通过 lampId 找灯杆
      Long poleId = null;
      if (lampId != null && !lampId.isEmpty())
      {
        IotPole pole = getOrCreatePole(lampId, null);
        poleId = pole.getPoleId();
      }
      else
      {
        // 如果没有 lampId，尝试通过设备查找
        IotDevice device = iotDeviceMapper.selectIotDeviceByDeviceName(deviceCode);
        if (device != null)
        {
          poleId = device.getPoleId();
        }
      }

      if (poleId == null)
      {
        log.warn("设备故障消息无法关联灯杆: deviceCode={}, 缺少lampId", deviceCode);
        return;
      }

      // 确保设备存在
      getOrCreateDevice(deviceCode, lampId, "0");

      // 生成报警记录
      IotAlarmRecord record = new IotAlarmRecord();
      record.setPoleId(poleId);
      record.setParamType("5");
      record.setAlarmValue(json.getString("faultCode"));
      record.setAlarmMessage("设备故障[" + deviceCode + "]: " + json.getString("faultMessage"));
      record.setStatus("0");
      record.setCreateBy("mqtt");
      record.setCreateTime(DateUtils.getNowDate());
      iotAlarmRecordMapper.insertIotAlarmRecord(record);

      log.warn("设备故障告警: deviceCode={}, poleId={}, fault={}",
          deviceCode, poleId, json.getString("faultMessage"));
    }
    catch (Exception e)
    {
      log.error("处理设备故障失败: deviceCode={}", deviceCode, e);
    }
  }

  private void handleDeviceResult(String deviceCode, String payload)
  {
    try
    {
      JSONObject json = JSON.parseObject(payload);
      String lampId = json.getString("lampId");

      // 查询灯杆ID
      Long poleId = null;
      if (lampId != null && !lampId.isEmpty())
      {
        IotPole pole = getOrCreatePole(lampId, null);
        poleId = pole.getPoleId();
      }
      else
      {
        IotDevice device = iotDeviceMapper.selectIotDeviceByDeviceName(deviceCode);
        if (device != null)
        {
          poleId = device.getPoleId();
        }
      }

      // 查询设备ID
      Long deviceId = null;
      IotDevice device = iotDeviceMapper.selectIotDeviceByDeviceName(deviceCode);
      if (device != null)
      {
        deviceId = device.getDeviceId();
        if (poleId == null)
        {
          poleId = device.getPoleId();
        }
      }

      // 记录控制日志
      IotControlLog logEntity = new IotControlLog();
      logEntity.setPoleId(poleId);
      logEntity.setDeviceId(deviceId);
      logEntity.setControlType(getControlType(json.getString("controlType")));
      logEntity.setResult(json.getString("result") != null ? json.getString("result") : "0");
      logEntity.setErrorMessage(json.getString("errorMessage"));
      logEntity.setCreateBy("mqtt");
      logEntity.setCreateTime(DateUtils.getNowDate());
      iotControlLogMapper.insertIotControlLog(logEntity);

      log.info("设备执行结果记录: deviceCode={}, result={}",
          deviceCode, logEntity.getResult());
    }
    catch (Exception e)
    {
      log.error("处理设备执行结果失败: deviceCode={}", deviceCode, e);
    }
  }

  // ==================== 摄像头主题 ====================

  private void handleCameraTopic(String[] parts, String topic, String payload)
  {
    if (parts.length < 4)
    {
      log.warn("摄像头主题格式不正确: {}", topic);
      return;
    }
    String cameraCode = parts[2];
    String action = parts[3];

    switch (action)
    {
      case "status":
        handleCameraStatus(cameraCode, payload);
        break;
      case "capture":
        handleCameraCapture(cameraCode, payload);
        break;
      default:
        log.debug("未知的摄像头操作: {}", action);
    }
  }

  private void handleCameraStatus(String cameraCode, String payload)
  {
    try
    {
      JSONObject json = JSON.parseObject(payload);
      String lampId = json.getString("lampId");

      // 自动注册摄像头
      IotDevice camera = getOrCreateDevice(cameraCode, lampId, "4");

      IotDevice updateDevice = new IotDevice();
      updateDevice.setDeviceId(camera.getDeviceId());
      updateDevice.setStatus(json.getString("status") != null ? json.getString("status") : "0");
      iotDeviceMapper.updateIotDevice(updateDevice);

      log.info("摄像头状态已更新: cameraCode={}, status={}", cameraCode, updateDevice.getStatus());
    }
    catch (Exception e)
    {
      log.error("处理摄像头状态失败: cameraCode={}", cameraCode, e);
    }
  }

  private void handleCameraCapture(String cameraCode, String payload)
  {
    try
    {
      JSONObject json = JSON.parseObject(payload);
      String lampId = json.getString("lampId");

      // 自动注册摄像头
      IotDevice camera = getOrCreateDevice(cameraCode, lampId, "4");

      // 获取或创建灯杆
      Long poleId = null;
      if (lampId != null && !lampId.isEmpty())
      {
        IotPole pole = getOrCreatePole(lampId, null);
        poleId = pole.getPoleId();
      }
      else
      {
        poleId = camera.getPoleId();
      }

      // 保存抓拍记录
      IotCameraCapture capture = new IotCameraCapture();
      capture.setPoleId(poleId != null ? poleId : 1L);
      capture.setImageUrl(json.getString("imageUrl"));
      capture.setPersonCount(json.getLong("personCount") != null ? json.getLong("personCount") : 0L);
      capture.setResultJson(json.getString("resultJson"));

      // 解析日期
      String captureTimeStr = json.getString("captureTime");
      if (captureTimeStr != null && !captureTimeStr.isEmpty())
      {
        capture.setCaptureTime(parseDateStr(captureTimeStr));
      }
      else
      {
        capture.setCaptureTime(new Date());
      }

      capture.setCreateBy("mqtt");
      capture.setCreateTime(DateUtils.getNowDate());
      iotCameraCaptureMapper.insertIotCameraCapture(capture);

      log.info("摄像头抓拍记录: cameraCode={}, personCount={}",
          cameraCode, capture.getPersonCount());
    }
    catch (Exception e)
    {
      log.error("处理摄像头抓拍失败: cameraCode={}", cameraCode, e);
    }
  }

  // ==================== 控制日志主题 ====================

  private void handleControlTopic(String[] parts, String topic, String payload)
  {
    if (parts.length < 4)
    {
      log.warn("控制主题格式不正确: {}", topic);
      return;
    }
    String deviceCode = parts[2];
    String action = parts[3];

    if ("log".equals(action))
    {
      handleControlLog(deviceCode, payload);
    }
    else
    {
      log.debug("未知的控制操作: {}", action);
    }
  }

  private void handleControlLog(String deviceCode, String payload)
  {
    try
    {
      JSONObject json = JSON.parseObject(payload);
      String lampId = json.getString("lampId");

      // 查询灯杆ID
      Long poleId = null;
      if (lampId != null && !lampId.isEmpty())
      {
        IotPole pole = getOrCreatePole(lampId, null);
        poleId = pole.getPoleId();
      }
      else
      {
        IotDevice device = iotDeviceMapper.selectIotDeviceByDeviceName(deviceCode);
        if (device != null)
        {
          poleId = device.getPoleId();
        }
      }

      // 查询设备ID
      Long deviceId = null;
      IotDevice device = iotDeviceMapper.selectIotDeviceByDeviceName(deviceCode);
      if (device != null)
      {
        deviceId = device.getDeviceId();
        if (poleId == null)
        {
          poleId = device.getPoleId();
        }
      }

      IotControlLog logEntity = new IotControlLog();
      logEntity.setPoleId(poleId);
      logEntity.setDeviceId(deviceId);
      logEntity.setControlType(getControlType(json.getString("controlType")));
      logEntity.setResult(json.getString("result") != null ? json.getString("result") : "0");
      logEntity.setErrorMessage(json.getString("errorMessage"));
      logEntity.setCreateBy("mqtt");
      logEntity.setCreateTime(DateUtils.getNowDate());
      iotControlLogMapper.insertIotControlLog(logEntity);

      log.info("控制日志记录: deviceCode={}, controlType={}, result={}",
          deviceCode, logEntity.getControlType(), logEntity.getResult());
    }
    catch (Exception e)
    {
      log.error("处理控制日志失败: deviceCode={}", deviceCode, e);
    }
  }

  // ==================== 报警规则主题 ====================

  private void handleAlarmTopic(String[] parts, String topic, String payload)
  {
    if (parts.length < 3)
    {
      log.warn("报警主题格式不正确: {}", topic);
      return;
    }

    String action = parts.length > 2 ? parts[2] : "";

    if ("rule".equals(action))
    {
      String subAction = parts.length > 3 ? parts[3] : "";
      if ("sync".equals(subAction))
      {
        handleAlarmRuleSync(payload);
      }
      else
      {
        log.debug("未知的报警规则操作: {}", subAction);
      }
    }
    else
    {
      log.debug("未知的报警操作: {}", action);
    }
  }

  private void handleAlarmRuleSync(String payload)
  {
    try
    {
      JSONObject json = JSON.parseObject(payload);
      String lampId = json.getString("lampId");
      String action = json.getString("action");

      if (lampId != null && !lampId.isEmpty())
      {
        IotPole pole = getOrCreatePole(lampId, null);

        if ("request".equals(action))
        {
          // 查询该灯杆的报警规则数量
          IotAlarmRule query = new IotAlarmRule();
          query.setPoleId(pole.getPoleId());
          List<IotAlarmRule> rules = iotAlarmRuleMapper.selectIotAlarmRuleList(query);
          log.info("设备请求报警规则同步: lampId={}, 规则数={}", lampId, rules.size());
        }
        else if ("sync".equals(action))
        {
          boolean success = json.getBooleanValue("success");
          log.info("报警规则同步结果: lampId={}, success={}", lampId, success);
        }
      }
    }
    catch (Exception e)
    {
      log.error("处理报警规则同步失败", e);
    }
  }

  // ==================== 报警规则检查 ====================

  private void checkAlarmRules(Long poleId, IotSensorMessage sensorMsg)
  {
    try
    {
      IotAlarmRule query = new IotAlarmRule();
      query.setPoleId(poleId);
      query.setEnabled("1");
      List<IotAlarmRule> rules = iotAlarmRuleMapper.selectIotAlarmRuleList(query);

      if (rules == null || rules.isEmpty())
      {
        log.debug("该灯杆无报警规则: poleId={}", poleId);
        return;
      }

      for (IotAlarmRule rule : rules)
      {
        Double currentValueDouble = getParamValue(sensorMsg, rule.getParamType());
        if (currentValueDouble == null)
        {
          continue;
        }

        BigDecimal currentValue = BigDecimal.valueOf(currentValueDouble);

        boolean isAlarm = false;
        if (rule.getMinValue() != null && currentValue.compareTo(rule.getMinValue()) < 0)
        {
          isAlarm = true;
        }
        if (rule.getMaxValue() != null && currentValue.compareTo(rule.getMaxValue()) > 0)
        {
          isAlarm = true;
        }

        if (isAlarm)
        {
          IotAlarmRecord record = new IotAlarmRecord();
          record.setPoleId(poleId);
          record.setParamType(rule.getParamType());
          record.setAlarmValue(currentValue.toString());
          record.setAlarmMessage("参数[" + getParamTypeName(rule.getParamType()) +
              "]超限，当前值：" + currentValue);
          record.setRuleId(rule.getRuleId());
          record.setStatus("0");
          record.setCreateTime(DateUtils.getNowDate());
          iotAlarmRecordMapper.insertIotAlarmRecord(record);

          log.warn("报警触发: poleId={}, paramType={}, value={}",
              poleId, rule.getParamType(), currentValue);
        }
      }
    }
    catch (Exception e)
    {
      log.error("检查报警规则失败: poleId={}", poleId, e);
    }
  }

  // ==================== 自动注册方法 ====================

  /**
   * 自动注册灯杆（如果不存在）
   */
  private IotPole getOrCreatePole(String poleCode, String poleName)
  {
    // 先查正常记录
    IotPole pole = iotPoleMapper.selectIotPoleByPoleCode(poleCode);
    if (pole != null)
    {
      return pole;
    }

    // 查是否有软删除的记录，有的话恢复
    pole = iotPoleMapper.selectIotPoleByPoleCodeIgnoreDel(poleCode);
    if (pole != null)
    {
      log.info("恢复已停用灯杆: poleCode={}, poleId={}", poleCode, pole.getPoleId());
      IotPole reactivate = new IotPole();
      reactivate.setPoleId(pole.getPoleId());
      reactivate.setDelFlag("0");
      reactivate.setStatus("0");
      reactivate.setLedStatus("0");
      iotPoleMapper.updateIotPole(reactivate);
      // 恢复该灯杆下已软删除的设备
      iotDeviceMapper.reactivateIotDeviceByPoleId(pole.getPoleId());
      // 重新查询返回
      return iotPoleMapper.selectIotPoleByPoleCode(poleCode);
    }

    log.info("自动注册灯杆: poleCode={}", poleCode);
    pole = new IotPole();
    pole.setPoleCode(poleCode);
    pole.setPoleName(poleName != null ? poleName : "自动注册-" + poleCode);
    pole.setStatus("0");
    pole.setLedStatus("0");
    pole.setLocation("MQTT自动注册");
    pole.setCreateBy("mqtt-auto");
    pole.setCreateTime(DateUtils.getNowDate());

    try
    {
      iotPoleMapper.insertIotPole(pole);
    }
    catch (DuplicateKeyException e)
    {
      // 并发场景下可能重复插入，直接查询返回
      log.warn("灯杆自动注册重复，重新查询: poleCode={}", poleCode);
      return iotPoleMapper.selectIotPoleByPoleCode(poleCode);
    }
    return pole;
  }

  /**
   * 自动注册设备（如果不存在）
   */
  private IotDevice getOrCreateDevice(String deviceName, String lampId, String deviceType)
  {
    IotDevice device = iotDeviceMapper.selectIotDeviceByDeviceName(deviceName);
    if (device != null)
    {
      // 如果设备已被软删除，恢复它
      if ("2".equals(device.getDelFlag()))
      {
        log.info("恢复已停用设备: deviceName={}, deviceId={}", deviceName, device.getDeviceId());
        IotDevice reactivate = new IotDevice();
        reactivate.setDeviceId(device.getDeviceId());
        reactivate.setDelFlag("0");
        reactivate.setStatus("0");
        iotDeviceMapper.updateIotDevice(reactivate);
        device = iotDeviceMapper.selectIotDeviceByDeviceName(deviceName);
      }
      return device;
    }

    log.info("自动注册设备: deviceName={}", deviceName);
    device = new IotDevice();
    device.setDeviceName(deviceName);
    device.setDeviceType(deviceType != null ? deviceType : "0");
    device.setDeviceModel("MQTT自动注册");
    device.setStatus("0");
    device.setCreateBy("mqtt-auto");
    device.setCreateTime(DateUtils.getNowDate());

    // 关联灯杆
    if (lampId != null && !lampId.isEmpty())
    {
      IotPole pole = getOrCreatePole(lampId, null);
      device.setPoleId(pole.getPoleId());
    }
    else
    {
      log.warn("设备缺少lampId，使用默认poleId=1: deviceName={}", deviceName);
      device.setPoleId(1L);
    }

    try
    {
      iotDeviceMapper.insertIotDevice(device);
    }
    catch (DuplicateKeyException e)
    {
      log.warn("设备自动注册重复，重新查询: deviceName={}", deviceName);
      return iotDeviceMapper.selectIotDeviceByDeviceName(deviceName);
    }
    return device;
  }

  // ==================== 工具方法 ====================

  /**
   * 获取参数值
   */
  private Double getParamValue(IotSensorMessage sensorMsg, String paramType)
  {
    if (paramType == null || sensorMsg == null)
    {
      return null;
    }

    switch (paramType)
    {
      case "0": return sensorMsg.getTemperature();
      case "1": return sensorMsg.getHumidity();
      case "2": return sensorMsg.getIllumination();
      case "3": return sensorMsg.getVoltage();
      case "4": return sensorMsg.getCurrent();
      default: return null;
    }
  }

  /**
   * 获取参数类型名称
   */
  private String getParamTypeName(String paramType)
  {
    switch (paramType)
    {
      case "0": return "温度";
      case "1": return "湿度";
      case "2": return "光照";
      case "3": return "电压";
      case "4": return "电流";
      case "5": return "设备故障";
      default: return "未知";
    }
  }

  /**
   * 解析日期字符串，支持多种格式
   */
  private Date parseDateStr(String dateStr)
  {
    if (dateStr == null || dateStr.isEmpty())
    {
      return new Date();
    }

    String[] patterns = {
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy-MM-dd"
    };

    for (String pattern : patterns)
    {
      try
      {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Shanghai"));
        return sdf.parse(dateStr);
      }
      catch (Exception e)
      {
        // 继续尝试下一个格式
      }
    }

    log.warn("无法解析日期字符串: {}, 使用当前时间", dateStr);
    return new Date();
  }

  /**
   * 获取控制类型编码
   */
  private String getControlType(String controlType)
  {
    if (controlType == null)
    {
      return "9";
    }
    switch (controlType)
    {
      case "on": return "0";
      case "off": return "1";
      case "restart": return "2";
      case "capture": return "3";
      case "dim": return "4";
      case "0": case "1": case "2": case "3": case "4": case "9":
        return controlType;
      default: return "9";
    }
  }

  /**
   * Double 转 BigDecimal，null 时返回 null
   */
  private BigDecimal toBigDecimal(Double value)
  {
    return value != null ? BigDecimal.valueOf(value) : null;
  }

  /**
   * Double 转 BigDecimal，null 时返回默认值
   */
  private BigDecimal toBigDecimal(Double value, double defaultValue)
  {
    return value != null ? BigDecimal.valueOf(value) : BigDecimal.valueOf(defaultValue);
  }
}
