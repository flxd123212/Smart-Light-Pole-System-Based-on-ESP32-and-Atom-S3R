package com.iot.service.impl;

import java.util.List;
import com.iotlight.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.iot.mapper.IotDeviceMapper;
import com.iot.domain.IotDevice;
import com.iot.service.IIotDeviceService;

/**
 * 设备Service业务层处理
 * 
 * @author iot
 * @date 2026-07-24
 */
@Service
public class IotDeviceServiceImpl implements IIotDeviceService 
{
    @Autowired
    private IotDeviceMapper iotDeviceMapper;

    /**
     * 查询设备
     * 
     * @param deviceId 设备主键
     * @return 设备
     */
    @Override
    public IotDevice selectIotDeviceByDeviceId(Long deviceId)
    {
        return iotDeviceMapper.selectIotDeviceByDeviceId(deviceId);
    }

    /**
     * 查询设备列表
     * 
     * @param iotDevice 设备
     * @return 设备
     */
    @Override
    public List<IotDevice> selectIotDeviceList(IotDevice iotDevice)
    {
        return iotDeviceMapper.selectIotDeviceList(iotDevice);
    }

    /**
     * 新增设备
     * 
     * @param iotDevice 设备
     * @return 结果
     */
    @Override
    public int insertIotDevice(IotDevice iotDevice)
    {
        iotDevice.setCreateTime(DateUtils.getNowDate());
        return iotDeviceMapper.insertIotDevice(iotDevice);
    }

    /**
     * 修改设备
     * 
     * @param iotDevice 设备
     * @return 结果
     */
    @Override
    public int updateIotDevice(IotDevice iotDevice)
    {
        iotDevice.setUpdateTime(DateUtils.getNowDate());
        return iotDeviceMapper.updateIotDevice(iotDevice);
    }

    /**
     * 批量删除设备
     * 
     * @param deviceIds 需要删除的设备主键
     * @return 结果
     */
    @Override
    public int deleteIotDeviceByDeviceIds(Long[] deviceIds)
    {
        return iotDeviceMapper.deleteIotDeviceByDeviceIds(deviceIds);
    }

    /**
     * 删除设备信息
     * 
     * @param deviceId 设备主键
     * @return 结果
     */
    @Override
    public int deleteIotDeviceByDeviceId(Long deviceId)
    {
        return iotDeviceMapper.deleteIotDeviceByDeviceId(deviceId);
    }
}
