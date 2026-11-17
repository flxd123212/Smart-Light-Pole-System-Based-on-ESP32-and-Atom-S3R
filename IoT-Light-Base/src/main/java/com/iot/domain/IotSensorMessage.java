package com.iot.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * ESP32 上报的传感器数据消息模型
 * 对应 Topic: lamp/LP001
 *
 * @author iot
 * @date 2026-08-13
 */
public class IotSensorMessage implements Serializable
{
  private static final long serialVersionUID = 1L;

  /** 灯杆编号（对应 iot_pole.pole_code） */
  @JsonProperty("lampId")
  private String lampId;

  /** 温度 ℃ */
  @JsonProperty("temperature")
  private Double temperature;

  /** 湿度 % */
  @JsonProperty("humidity")
  private Double humidity;

  /** 光照强度（自定义单位 0~40.95） */
  @JsonProperty("illumination")
  private Double illumination;

  /** LED 工作电压 V */
  @JsonProperty("voltage")
  private Double voltage;

  /** LED 工作电流 A */
  @JsonProperty("current")
  private Double current;

  /** LED 状态：0-关 / 1-开 */
  @JsonProperty("status")
  private String status;

  public String getLampId()
  {
    return lampId;
  }

  public void setLampId(String lampId)
  {
    this.lampId = lampId;
  }

  public Double getTemperature()
  {
    return temperature;
  }

  public void setTemperature(Double temperature)
  {
    this.temperature = temperature;
  }

  public Double getHumidity()
  {
    return humidity;
  }

  public void setHumidity(Double humidity)
  {
    this.humidity = humidity;
  }

  public Double getIllumination()
  {
    return illumination;
  }

  public void setIllumination(Double illumination)
  {
    this.illumination = illumination;
  }

  public Double getVoltage()
  {
    return voltage;
  }

  public void setVoltage(Double voltage)
  {
    this.voltage = voltage;
  }

  public Double getCurrent()
  {
    return current;
  }

  public void setCurrent(Double current)
  {
    this.current = current;
  }

  public String getStatus()
  {
    return status;
  }

  public void setStatus(String status)
  {
    this.status = status;
  }

  @Override
  public String toString()
  {
    return "IotSensorMessage{" +
      "lampId='" + lampId + '\'' +
      ", temperature=" + temperature +
      ", humidity=" + humidity +
      ", illumination=" + illumination +
      ", voltage=" + voltage +
      ", current=" + current +
      ", status='" + status + '\'' +
      '}';
  }
}
