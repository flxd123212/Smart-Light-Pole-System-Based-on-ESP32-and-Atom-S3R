package com.iot.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.iotlight.common.annotation.Excel;
import com.iotlight.common.core.domain.BaseEntity;

/**
 * 报警规则对象 iot_alarm_rule
 * 
 * @author extrao
 * @date 2026-07-24
 */
public class IotAlarmRule extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 规则主键 */
    private Long ruleId;

    /** 灯杆ID（NULL表示全局规则） */
    @Excel(name = "灯杆ID", readConverterExp = "N=ULL表示全局规则")
    private Long poleId;

    /** 参数类型：0温度 / 1湿度 / 2光照 / 3电压 / 4电流 */
    @Excel(name = "参数类型：0温度 / 1湿度 / 2光照 / 3电压 / 4电流")
    private String paramType;

    /** 下限值 */
    @Excel(name = "下限值")
    private BigDecimal minValue;

    /** 上限值 */
    @Excel(name = "上限值")
    private BigDecimal maxValue;

    /** 是否启用：0停用 / 1启用 */
    @Excel(name = "是否启用：0停用 / 1启用")
    private String enabled;

    public void setRuleId(Long ruleId) 
    {
        this.ruleId = ruleId;
    }

    public Long getRuleId() 
    {
        return ruleId;
    }

    public void setPoleId(Long poleId) 
    {
        this.poleId = poleId;
    }

    public Long getPoleId() 
    {
        return poleId;
    }

    public void setParamType(String paramType) 
    {
        this.paramType = paramType;
    }

    public String getParamType() 
    {
        return paramType;
    }

    public void setMinValue(BigDecimal minValue) 
    {
        this.minValue = minValue;
    }

    public BigDecimal getMinValue() 
    {
        return minValue;
    }

    public void setMaxValue(BigDecimal maxValue) 
    {
        this.maxValue = maxValue;
    }

    public BigDecimal getMaxValue() 
    {
        return maxValue;
    }

    public void setEnabled(String enabled) 
    {
        this.enabled = enabled;
    }

    public String getEnabled() 
    {
        return enabled;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("ruleId", getRuleId())
            .append("poleId", getPoleId())
            .append("paramType", getParamType())
            .append("minValue", getMinValue())
            .append("maxValue", getMaxValue())
            .append("enabled", getEnabled())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
