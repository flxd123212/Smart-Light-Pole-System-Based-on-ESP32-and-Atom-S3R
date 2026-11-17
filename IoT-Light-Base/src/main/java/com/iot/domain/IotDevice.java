package com.iot.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.iotlight.common.annotation.Excel;
import com.iotlight.common.core.domain.BaseEntity;

/**
 * 设备对象 iot_device
 * 
 * @author iot
 * @date 2026-07-24
 */
public class IotDevice extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 设备主键 */
    private Long deviceId;

    /** 所属灯杆ID */
    @Excel(name = "所属灯杆ID")
    private Long poleId;

    /** 设备名称 */
    @Excel(name = "设备名称")
    private String deviceName;

    /** 设备类型：0温湿度 / 1光照 / 2电压电流 / 3LED灯 / 4摄像头 */
    @Excel(name = "设备类型：0温湿度 / 1光照 / 2电压电流 / 3LED灯 / 4摄像头")
    private String deviceType;

    /** 设备型号 */
    @Excel(name = "设备型号")
    private String deviceModel;

    /** 状态：0正常 / 1离线 / 2故障 */
    @Excel(name = "状态：0正常 / 1离线 / 2故障")
    private String status;

    /** 删除标志：0存在 / 2删除 */
    private String delFlag;

    public void setDeviceId(Long deviceId) 
    {
        this.deviceId = deviceId;
    }

    public Long getDeviceId() 
    {
        return deviceId;
    }

    public void setPoleId(Long poleId) 
    {
        this.poleId = poleId;
    }

    public Long getPoleId() 
    {
        return poleId;
    }

    public void setDeviceName(String deviceName) 
    {
        this.deviceName = deviceName;
    }

    public String getDeviceName() 
    {
        return deviceName;
    }

    public void setDeviceType(String deviceType) 
    {
        this.deviceType = deviceType;
    }

    public String getDeviceType() 
    {
        return deviceType;
    }

    public void setDeviceModel(String deviceModel) 
    {
        this.deviceModel = deviceModel;
    }

    public String getDeviceModel() 
    {
        return deviceModel;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setDelFlag(String delFlag) 
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag() 
    {
        return delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("deviceId", getDeviceId())
            .append("poleId", getPoleId())
            .append("deviceName", getDeviceName())
            .append("deviceType", getDeviceType())
            .append("deviceModel", getDeviceModel())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
