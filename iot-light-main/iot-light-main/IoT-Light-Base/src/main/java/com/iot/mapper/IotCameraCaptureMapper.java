package com.iot.mapper;

import java.util.List;
import com.iot.domain.IotCameraCapture;

/**
 * 摄像头抓拍记录Mapper接口
 * 
 * @author extrao
 * @date 2026-07-24
 */
public interface IotCameraCaptureMapper 
{
    /**
     * 查询摄像头抓拍记录
     * 
     * @param captureId 摄像头抓拍记录主键
     * @return 摄像头抓拍记录
     */
    public IotCameraCapture selectIotCameraCaptureByCaptureId(Long captureId);

    /**
     * 查询摄像头抓拍记录列表
     * 
     * @param iotCameraCapture 摄像头抓拍记录
     * @return 摄像头抓拍记录集合
     */
    public List<IotCameraCapture> selectIotCameraCaptureList(IotCameraCapture iotCameraCapture);

    /**
     * 新增摄像头抓拍记录
     * 
     * @param iotCameraCapture 摄像头抓拍记录
     * @return 结果
     */
    public int insertIotCameraCapture(IotCameraCapture iotCameraCapture);

    /**
     * 修改摄像头抓拍记录
     * 
     * @param iotCameraCapture 摄像头抓拍记录
     * @return 结果
     */
    public int updateIotCameraCapture(IotCameraCapture iotCameraCapture);

    /**
     * 删除摄像头抓拍记录
     * 
     * @param captureId 摄像头抓拍记录主键
     * @return 结果
     */
    public int deleteIotCameraCaptureByCaptureId(Long captureId);

    /**
     * 批量删除摄像头抓拍记录
     * 
     * @param captureIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteIotCameraCaptureByCaptureIds(Long[] captureIds);
}
