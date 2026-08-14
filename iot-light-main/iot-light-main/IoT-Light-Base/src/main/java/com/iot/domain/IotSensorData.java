package com.iot.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.iotlight.common.annotation.Excel;
import com.iotlight.common.core.domain.BaseEntity;

/**
 * 传感器数据对象 iot_sensor_data
 * 
 * @author extrao
 * @date 2026-07-24
 */
public class IotSensorData extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 数据主键 */
    private Long dataId;

    /** 所属灯杆ID */
    @Excel(name = "所属灯杆ID")
    private Long poleId;

    /** 温度（℃） */
    @Excel(name = "温度", readConverterExp = "℃=")
    private BigDecimal temperature;

    /** 湿度（%RH） */
    @Excel(name = "湿度", readConverterExp = "%=RH")
    private BigDecimal humidity;

    /** 光照强度（lx） */
    @Excel(name = "光照强度", readConverterExp = "l=x")
    private BigDecimal illumination;

    /** 电压（V） */
    @Excel(name = "电压", readConverterExp = "V=")
    private BigDecimal voltage;

    /** 电流（A） */
    @Excel(name = "电流", readConverterExp = "A=")
    private BigDecimal current;

    /** 数据采集时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "数据采集时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date collectTime;

    public void setDataId(Long dataId) 
    {
        this.dataId = dataId;
    }

    public Long getDataId() 
    {
        return dataId;
    }

    public void setPoleId(Long poleId) 
    {
        this.poleId = poleId;
    }

    public Long getPoleId() 
    {
        return poleId;
    }

    public void setTemperature(BigDecimal temperature) 
    {
        this.temperature = temperature;
    }

    public BigDecimal getTemperature() 
    {
        return temperature;
    }

    public void setHumidity(BigDecimal humidity) 
    {
        this.humidity = humidity;
    }

    public BigDecimal getHumidity() 
    {
        return humidity;
    }

    public void setIllumination(BigDecimal illumination) 
    {
        this.illumination = illumination;
    }

    public BigDecimal getIllumination() 
    {
        return illumination;
    }

    public void setVoltage(BigDecimal voltage) 
    {
        this.voltage = voltage;
    }

    public BigDecimal getVoltage() 
    {
        return voltage;
    }

    public void setCurrent(BigDecimal current) 
    {
        this.current = current;
    }

    public BigDecimal getCurrent() 
    {
        return current;
    }

    public void setCollectTime(Date collectTime) 
    {
        this.collectTime = collectTime;
    }

    public Date getCollectTime() 
    {
        return collectTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("dataId", getDataId())
            .append("poleId", getPoleId())
            .append("temperature", getTemperature())
            .append("humidity", getHumidity())
            .append("illumination", getIllumination())
            .append("voltage", getVoltage())
            .append("current", getCurrent())
            .append("collectTime", getCollectTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
