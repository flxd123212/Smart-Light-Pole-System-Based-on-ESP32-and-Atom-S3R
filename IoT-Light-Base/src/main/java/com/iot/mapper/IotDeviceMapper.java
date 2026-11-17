package com.iot.mapper;

import java.util.List;
import com.iot.domain.IotDevice;

/**
 * 设备Mapper接口
 * 
 * @author iot
 * @date 2026-07-24
 */
public interface IotDeviceMapper 
{
    /**
     * 查询设备
     * 
     * @param deviceId 设备主键
     * @return 设备
     */
    public IotDevice selectIotDeviceByDeviceId(Long deviceId);

    /**
     * 查询设备列表
     * 
     * @param iotDevice 设备
     * @return 设备集合
     */
    public List<IotDevice> selectIotDeviceList(IotDevice iotDevice);

    /**
     * 根据设备名称查询设备
     *
     * @param deviceName 设备名称
     * @return 设备
     */
    public IotDevice selectIotDeviceByDeviceName(String deviceName);

    /**
     * 新增设备
     * 
     * @param iotDevice 设备
     * @return 结果
     */
    public int insertIotDevice(IotDevice iotDevice);

    /**
     * 修改设备
     * 
     * @param iotDevice 设备
     * @return 结果
     */
    public int updateIotDevice(IotDevice iotDevice);

    /**
     * 删除设备
     * 
     * @param deviceId 设备主键
     * @return 结果
     */
    public int deleteIotDeviceByDeviceId(Long deviceId);

    /**
     * 批量删除设备
     * 
     * @param deviceIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteIotDeviceByDeviceIds(Long[] deviceIds);

    /**
     * 按灯杆ID软删除设备（停用灯杆时级联停用）
     *
     * @param poleId 灯杆ID
     * @return 结果
     */
    public int deleteIotDeviceByPoleId(Long poleId);

    /**
     * 恢复灯杆下已软删除的设备（Flask重新连接时调用）
     *
     * @param poleId 灯杆ID
     * @return 结果
     */
    public int reactivateIotDeviceByPoleId(Long poleId);
}
