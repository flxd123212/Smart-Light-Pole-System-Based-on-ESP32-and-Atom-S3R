package com.iot.service.impl;

import java.util.List;
import com.iotlight.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.iot.mapper.IotSensorDataMapper;
import com.iot.domain.IotSensorData;
import com.iot.service.IIotSensorDataService;

/**
 * 传感器数据Service业务层处理
 * 
 * @author extrao
 * @date 2026-07-24
 */
@Service
public class IotSensorDataServiceImpl implements IIotSensorDataService 
{
    @Autowired
    private IotSensorDataMapper iotSensorDataMapper;

    /**
     * 查询传感器数据
     * 
     * @param dataId 传感器数据主键
     * @return 传感器数据
     */
    @Override
    public IotSensorData selectIotSensorDataByDataId(Long dataId)
    {
        return iotSensorDataMapper.selectIotSensorDataByDataId(dataId);
    }

    /**
     * 查询传感器数据列表
     * 
     * @param iotSensorData 传感器数据
     * @return 传感器数据
     */
    @Override
    public List<IotSensorData> selectIotSensorDataList(IotSensorData iotSensorData)
    {
        return iotSensorDataMapper.selectIotSensorDataList(iotSensorData);
    }

    /**
     * 新增传感器数据
     * 
     * @param iotSensorData 传感器数据
     * @return 结果
     */
    @Override
    public int insertIotSensorData(IotSensorData iotSensorData)
    {
        iotSensorData.setCreateTime(DateUtils.getNowDate());
        return iotSensorDataMapper.insertIotSensorData(iotSensorData);
    }

    /**
     * 修改传感器数据
     * 
     * @param iotSensorData 传感器数据
     * @return 结果
     */
    @Override
    public int updateIotSensorData(IotSensorData iotSensorData)
    {
        return iotSensorDataMapper.updateIotSensorData(iotSensorData);
    }

    /**
     * 批量删除传感器数据
     * 
     * @param dataIds 需要删除的传感器数据主键
     * @return 结果
     */
    @Override
    public int deleteIotSensorDataByDataIds(Long[] dataIds)
    {
        return iotSensorDataMapper.deleteIotSensorDataByDataIds(dataIds);
    }

    /**
     * 删除传感器数据信息
     * 
     * @param dataId 传感器数据主键
     * @return 结果
     */
    @Override
    public int deleteIotSensorDataByDataId(Long dataId)
    {
        return iotSensorDataMapper.deleteIotSensorDataByDataId(dataId);
    }
}
