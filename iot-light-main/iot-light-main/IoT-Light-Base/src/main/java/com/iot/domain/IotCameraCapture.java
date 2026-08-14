package com.iot.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.iotlight.common.annotation.Excel;
import com.iotlight.common.core.domain.BaseEntity;

/**
 * 摄像头抓拍记录对象 iot_camera_capture
 * 
 * @author extrao
 * @date 2026-07-24
 */
public class IotCameraCapture extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 抓拍主键 */
    private Long captureId;

    /** 灯杆ID */
    @Excel(name = "灯杆ID")
    private Long poleId;

    /** 原始图片存储路径 */
    @Excel(name = "原始图片存储路径")
    private String imageUrl;

    /** 检测到的行人数量 */
    @Excel(name = "检测到的行人数量")
    private Long personCount;

    /** AI识别结果JSON */
    @Excel(name = "AI识别结果JSON")
    private String resultJson;

    /** 抓拍时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "抓拍时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date captureTime;

    /** 删除标志：0存在 / 2删除 */
    private String delFlag;

    public void setCaptureId(Long captureId) 
    {
        this.captureId = captureId;
    }

    public Long getCaptureId() 
    {
        return captureId;
    }

    public void setPoleId(Long poleId) 
    {
        this.poleId = poleId;
    }

    public Long getPoleId() 
    {
        return poleId;
    }

    public void setImageUrl(String imageUrl) 
    {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() 
    {
        return imageUrl;
    }

    public void setPersonCount(Long personCount) 
    {
        this.personCount = personCount;
    }

    public Long getPersonCount() 
    {
        return personCount;
    }

    public void setResultJson(String resultJson) 
    {
        this.resultJson = resultJson;
    }

    public String getResultJson() 
    {
        return resultJson;
    }

    public void setCaptureTime(Date captureTime) 
    {
        this.captureTime = captureTime;
    }

    public Date getCaptureTime() 
    {
        return captureTime;
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
            .append("captureId", getCaptureId())
            .append("poleId", getPoleId())
            .append("imageUrl", getImageUrl())
            .append("personCount", getPersonCount())
            .append("resultJson", getResultJson())
            .append("captureTime", getCaptureTime())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
