package com.iot.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.iotlight.common.annotation.Excel;
import com.iotlight.common.core.domain.BaseEntity;

/**
 * 控制日志对象 iot_control_log
 * 
 * @author extrao
 * @date 2026-07-24
 */
public class IotControlLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志主键 */
    private Long logId;

    /** 灯杆ID */
    @Excel(name = "灯杆ID")
    private Long poleId;

    /** 设备ID */
    @Excel(name = "设备ID")
    private Long deviceId;

    /** 控制类型：0开灯 / 1关灯 / 2重启 */
    @Excel(name = "控制类型：0开灯 / 1关灯 / 2重启")
    private String controlType;

    /** 执行结果：0成功 / 1失败 */
    @Excel(name = "执行结果：0成功 / 1失败")
    private String result;

    /** 错误信息 */
    @Excel(name = "错误信息")
    private String errorMessage;

    public void setLogId(Long logId) 
    {
        this.logId = logId;
    }

    public Long getLogId() 
    {
        return logId;
    }

    public void setPoleId(Long poleId) 
    {
        this.poleId = poleId;
    }

    public Long getPoleId() 
    {
        return poleId;
    }

    public void setDeviceId(Long deviceId) 
    {
        this.deviceId = deviceId;
    }

    public Long getDeviceId() 
    {
        return deviceId;
    }

    public void setControlType(String controlType) 
    {
        this.controlType = controlType;
    }

    public String getControlType() 
    {
        return controlType;
    }

    public void setResult(String result) 
    {
        this.result = result;
    }

    public String getResult() 
    {
        return result;
    }

    public void setErrorMessage(String errorMessage) 
    {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() 
    {
        return errorMessage;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("logId", getLogId())
            .append("poleId", getPoleId())
            .append("deviceId", getDeviceId())
            .append("controlType", getControlType())
            .append("result", getResult())
            .append("errorMessage", getErrorMessage())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
