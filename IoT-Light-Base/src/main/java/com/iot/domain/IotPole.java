package com.iot.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.iotlight.common.annotation.Excel;
import com.iotlight.common.core.domain.BaseEntity;

/**
 * 灯杆对象 iot_pole
 * 
 * @author extrao
 * @date 2026-07-24
 */
public class IotPole extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 灯杆主键ID */
    private Long poleId;

    /** 灯杆编号（唯一） */
    @Excel(name = "灯杆编号", readConverterExp = "唯=一")
    private String poleCode;

    /** 灯杆名称 */
    @Excel(name = "灯杆名称")
    private String poleName;

    /** 安装位置描述 */
    @Excel(name = "安装位置描述")
    private String location;

    /** 纬度坐标 */
    @Excel(name = "纬度坐标")
    private BigDecimal latitude;

    /** 经度坐标 */
    @Excel(name = "经度坐标")
    private BigDecimal longitude;

    /** 状态：0正常 / 1离线 / 2故障 */
    @Excel(name = "状态：0正常 / 1离线 / 2故障")
    private String status;

    /** LED灯状态：0关闭 / 1开启 */
    @Excel(name = "LED灯状态：0关闭 / 1开启")
    private String ledStatus;

    /** 显示排序号 */
    @Excel(name = "显示排序号")
    private Long orderNum;

    /** 删除标志：0存在 / 2删除 */
    private String delFlag;

    public void setPoleId(Long poleId) 
    {
        this.poleId = poleId;
    }

    public Long getPoleId() 
    {
        return poleId;
    }

    public void setPoleCode(String poleCode) 
    {
        this.poleCode = poleCode;
    }

    public String getPoleCode() 
    {
        return poleCode;
    }

    public void setPoleName(String poleName) 
    {
        this.poleName = poleName;
    }

    public String getPoleName() 
    {
        return poleName;
    }

    public void setLocation(String location) 
    {
        this.location = location;
    }

    public String getLocation() 
    {
        return location;
    }

    public void setLatitude(BigDecimal latitude) 
    {
        this.latitude = latitude;
    }

    public BigDecimal getLatitude() 
    {
        return latitude;
    }

    public void setLongitude(BigDecimal longitude) 
    {
        this.longitude = longitude;
    }

    public BigDecimal getLongitude() 
    {
        return longitude;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setLedStatus(String ledStatus) 
    {
        this.ledStatus = ledStatus;
    }

    public String getLedStatus() 
    {
        return ledStatus;
    }

    public void setOrderNum(Long orderNum) 
    {
        this.orderNum = orderNum;
    }

    public Long getOrderNum() 
    {
        return orderNum;
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
            .append("poleId", getPoleId())
            .append("poleCode", getPoleCode())
            .append("poleName", getPoleName())
            .append("location", getLocation())
            .append("latitude", getLatitude())
            .append("longitude", getLongitude())
            .append("status", getStatus())
            .append("ledStatus", getLedStatus())
            .append("orderNum", getOrderNum())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
