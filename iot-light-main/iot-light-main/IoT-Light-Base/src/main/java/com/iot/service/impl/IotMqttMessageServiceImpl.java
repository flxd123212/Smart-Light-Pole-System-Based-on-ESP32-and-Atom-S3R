package com.iot.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.iotlight.common.utils.DateUtils;
import com.iot.domain.IotPole;
import com.iot.domain.IotSensorData;
import com.iot.domain.IotAlarmRule;
import com.iot.domain.IotAlarmRecord;
import com.iot.mapper.IotPoleMapper;
import com.iot.mapper.IotSensorDataMapper;
import com.iot.mapper.IotAlarmRuleMapper;
import com.iot.mapper.IotAlarmRecordMapper;
import com.iot.service.IIotMqttMessageService;

/**
 * MQTT消息处理Service业务层处理
 *
 * @author extrao
 * @date 2026-07-24
 */
@Service
public class IotMqttMessageServiceImpl implements IIotMqttMessageService
{
  private static final Logger log = LoggerFactory.getLogger(IotMqttMessageServiceImpl.class);

  @Autowired
  private IotPoleMapper iotPoleMapper;

  @Autowired
  private IotSensorDataMapper iotSensorDataMapper;

  @Autowired
  private IotAlarmRuleMapper iotAlarmRuleMapper;

  @Autowired
  private IotAlarmRecordMapper iotAlarmRecordMapper;

  /**
   * 处理设备上报消息
   *
   * @param topic 主题
   * @param payload 消息体
   */
  @Override
  public void handleDeviceMessage(String topic, String payload)
  {
    try
    {
      log.info("收到MQTT消息 topic={}, payload={}", topic, payload);

      JSONObject json = JSON.parseObject(payload);
      String lampId = json.getString("lampId");

      if (lampId == null || lampId.isEmpty())
      {
        log.warn("消息缺少lampId，忽略");
        return;
      }

      // 1. 根据lampId(pole_code)查询灯杆
      IotPole pole = iotPoleMapper.selectIotPoleByPoleCode(lampId);
      if (pole == null)
      {
        log.warn("未找到灯杆 poleCode={}", lampId);
        return;
      }

      Long poleId = pole.getPoleId();

      // 2. 保存传感器数据
      IotSensorData sensorData = new IotSensorData();
      sensorData.setPoleId(poleId);
      sensorData.setTemperature(json.getBigDecimal("temperature"));
      sensorData.setHumidity(json.getBigDecimal("humidity"));
      sensorData.setIllumination(json.getBigDecimal("illumination"));
      sensorData.setVoltage(json.getBigDecimal("voltage"));
      sensorData.setCurrent(json.getBigDecimal("current"));
      sensorData.setCollectTime(new Date());
      sensorData.setCreateBy("mqtt");
      sensorData.setCreateTime(DateUtils.getNowDate());
      iotSensorDataMapper.insertIotSensorData(sensorData);

      // 3. 更新灯杆LED状态
      Integer status = json.getInteger("status");
      if (status != null)
      {
        IotPole updatePole = new IotPole();
        updatePole.setPoleId(poleId);
        updatePole.setLedStatus(String.valueOf(status));
        updatePole.setStatus("0");
        iotPoleMapper.updateIotPoleLedStatus(updatePole);
      }

      // 4. 检查报警规则
      checkAlarmRules(poleId, json);

      log.info("灯杆[{}]数据处理完成", lampId);
    }
    catch (Exception e)
    {
      log.error("处理MQTT消息异常", e);
    }
  }

  /**
   * 检查报警规则
   *
   * @param poleId 灯杆ID
   * @param json 消息体
   */
  private void checkAlarmRules(Long poleId, JSONObject json)
  {
    List<IotAlarmRule> rules = iotAlarmRuleMapper.selectEnabledRulesByPoleId(poleId);
    if (rules == null || rules.isEmpty())
    {
      return;
    }

    for (IotAlarmRule rule : rules)
    {
      BigDecimal value = getParamValue(json, rule.getParamType());
      if (value == null)
      {
        continue;
      }

      boolean triggered = false;
      StringBuilder msg = new StringBuilder();

      if (rule.getMinValue() != null && value.compareTo(rule.getMinValue()) < 0)
      {
        triggered = true;
        msg.append("低于下限").append(rule.getMinValue());
      }
      if (rule.getMaxValue() != null && value.compareTo(rule.getMaxValue()) > 0)
      {
        triggered = true;
        msg.append("超过上限").append(rule.getMaxValue());
      }

      if (triggered)
      {
        IotAlarmRecord record = new IotAlarmRecord();
        record.setPoleId(poleId);
        record.setRuleId(rule.getRuleId());
        record.setParamType(rule.getParamType());
        record.setAlarmValue(value.toPlainString());
        record.setAlarmMessage(getParamTypeName(rule.getParamType()) + msg.toString() + "，当前值：" + value);
        record.setStatus("0");
        record.setCreateBy("system");
        record.setCreateTime(DateUtils.getNowDate());
        iotAlarmRecordMapper.insertIotAlarmRecord(record);

        log.warn("触发报警：poleId={}, paramType={}, value={}", poleId, rule.getParamType(), value);
      }
    }
  }

  /**
   * 根据参数类型获取对应值
   *
   * @param json 消息体
   * @param paramType 参数类型
   * @return 值
   */
  private BigDecimal getParamValue(JSONObject json, String paramType)
  {
    switch (paramType)
    {
      case "0": return json.getBigDecimal("temperature");
      case "1": return json.getBigDecimal("humidity");
      case "2": return json.getBigDecimal("illumination");
      case "3": return json.getBigDecimal("voltage");
      case "4": return json.getBigDecimal("current");
      default: return null;
    }
  }

  /**
   * 获取参数类型名称
   *
   * @param paramType 参数类型
   * @return 名称
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
      default: return "未知";
    }
  }
}
