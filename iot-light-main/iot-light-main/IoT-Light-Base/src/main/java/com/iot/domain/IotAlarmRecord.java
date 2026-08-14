package com.iot.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.iotlight.common.annotation.Excel;
import com.iotlight.common.core.domain.BaseEntity;

/**
 * 报警记录对象 iot_alarm_record
 * 
 * @author extrao
 * @date 2026-07-24
 */
public class IotAlarmRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录主键 */
    private Long recordId;

    /** 灯杆ID */
    @Excel(name = "灯杆ID")
    private Long poleId;

    /** 触发的规则ID */
    @Excel(name = "触发的规则ID")
    private Long ruleId;

    /** 报警参数类型：0温度 / 1湿度 / 2光照 / 3电压 / 4电流 / 5行人 */
    @Excel(name = "报警参数类型：0温度 / 1湿度 / 2光照 / 3电压 / 4电流 / 5行人")
    private String paramType;

    /** 触发时的数值 */
    @Excel(name = "触发时的数值")
    private String alarmValue;

    /** 报警描述信息 */
    @Excel(name = "报警描述信息")
    private String alarmMessage;

    /** 处理状态：0未处理 / 1已处理 / 2已忽略 */
    @Excel(name = "处理状态：0未处理 / 1已处理 / 2已忽略")
    private String status;

    /** 处理人 */
    @Excel(name = "处理人")
    private String handleBy;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "处理时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date handleTime;

    /** 处理备注 */
    @Excel(name = "处理备注")
    private String handleRemark;

    /** 删除标志：0存在 / 2删除 */
    private String delFlag;

    public void setRecordId(Long recordId) 
    {
        this.recordId = recordId;
    }

    public Long getRecordId() 
    {
        return recordId;
    }

    public void setPoleId(Long poleId) 
    {
        this.poleId = poleId;
    }

    public Long getPoleId() 
    {
        return poleId;
    }

    public void setRuleId(Long ruleId) 
    {
        this.ruleId = ruleId;
    }

    public Long getRuleId() 
    {
        return ruleId;
    }

    public void setParamType(String paramType) 
    {
        this.paramType = paramType;
    }

    public String getParamType() 
    {
        return paramType;
    }

    public void setAlarmValue(String alarmValue) 
    {
        this.alarmValue = alarmValue;
    }

    public String getAlarmValue() 
    {
        return alarmValue;
    }

    public void setAlarmMessage(String alarmMessage) 
    {
        this.alarmMessage = alarmMessage;
    }

    public String getAlarmMessage() 
    {
        return alarmMessage;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setHandleBy(String handleBy) 
    {
        this.handleBy = handleBy;
    }

    public String getHandleBy() 
    {
        return handleBy;
    }

    public void setHandleTime(Date handleTime) 
    {
        this.handleTime = handleTime;
    }

    public Date getHandleTime() 
    {
        return handleTime;
    }

    public void setHandleRemark(String handleRemark) 
    {
        this.handleRemark = handleRemark;
    }

    public String getHandleRemark() 
    {
        return handleRemark;
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
            .append("recordId", getRecordId())
            .append("poleId", getPoleId())
            .append("ruleId", getRuleId())
            .append("paramType", getParamType())
            .append("alarmValue", getAlarmValue())
            .append("alarmMessage", getAlarmMessage())
            .append("status", getStatus())
            .append("handleBy", getHandleBy())
            .append("handleTime", getHandleTime())
            .append("handleRemark", getHandleRemark())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
