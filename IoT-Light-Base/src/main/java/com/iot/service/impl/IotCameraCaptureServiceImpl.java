package com.iot.service.impl;

import java.util.List;
import com.iotlight.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.iot.mapper.IotCameraCaptureMapper;
import com.iot.domain.IotCameraCapture;
import com.iot.service.IIotCameraCaptureService;

/**
 * 摄像头抓拍记录Service业务层处理
 * 
 * @author extrao
 * @date 2026-07-24
 */
@Service
public class IotCameraCaptureServiceImpl implements IIotCameraCaptureService 
{
    @Autowired
    private IotCameraCaptureMapper iotCameraCaptureMapper;

    /**
     * 查询摄像头抓拍记录
     * 
     * @param captureId 摄像头抓拍记录主键
     * @return 摄像头抓拍记录
     */
    @Override
    public IotCameraCapture selectIotCameraCaptureByCaptureId(Long captureId)
    {
        return iotCameraCaptureMapper.selectIotCameraCaptureByCaptureId(captureId);
    }

    /**
     * 查询摄像头抓拍记录列表
     * 
     * @param iotCameraCapture 摄像头抓拍记录
     * @return 摄像头抓拍记录
     */
    @Override
    public List<IotCameraCapture> selectIotCameraCaptureList(IotCameraCapture iotCameraCapture)
    {
        return iotCameraCaptureMapper.selectIotCameraCaptureList(iotCameraCapture);
    }

    /**
     * 新增摄像头抓拍记录
     * 
     * @param iotCameraCapture 摄像头抓拍记录
     * @return 结果
     */
    @Override
    public int insertIotCameraCapture(IotCameraCapture iotCameraCapture)
    {
        iotCameraCapture.setCreateTime(DateUtils.getNowDate());
        return iotCameraCaptureMapper.insertIotCameraCapture(iotCameraCapture);
    }

    /**
     * 修改摄像头抓拍记录
     * 
     * @param iotCameraCapture 摄像头抓拍记录
     * @return 结果
     */
    @Override
    public int updateIotCameraCapture(IotCameraCapture iotCameraCapture)
    {
        return iotCameraCaptureMapper.updateIotCameraCapture(iotCameraCapture);
    }

    /**
     * 批量删除摄像头抓拍记录
     * 
     * @param captureIds 需要删除的摄像头抓拍记录主键
     * @return 结果
     */
    @Override
    public int deleteIotCameraCaptureByCaptureIds(Long[] captureIds)
    {
        return iotCameraCaptureMapper.deleteIotCameraCaptureByCaptureIds(captureIds);
    }

    /**
     * 删除摄像头抓拍记录信息
     * 
     * @param captureId 摄像头抓拍记录主键
     * @return 结果
     */
    @Override
    public int deleteIotCameraCaptureByCaptureId(Long captureId)
    {
        return iotCameraCaptureMapper.deleteIotCameraCaptureByCaptureId(captureId);
    }
}
