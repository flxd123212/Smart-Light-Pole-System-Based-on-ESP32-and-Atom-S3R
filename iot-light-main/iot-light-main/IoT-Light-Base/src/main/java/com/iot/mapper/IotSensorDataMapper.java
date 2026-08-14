package com.iot.mapper;

import java.util.List;

import com.iot.domain.IotAlarmRule;
import com.iot.domain.IotSensorData;

/**
 * 传感器数据Mapper接口
 *
 * @author extrao
 * @date 2026-07-24
 */
public interface IotSensorDataMapper
{
    /**
     * 查询传感器数据
     *
     * @param dataId 传感器数据主键
     * @return 传感器数据
     */
    public IotSensorData selectIotSensorDataByDataId(Long dataId);

    /**
     * 查询传感器数据列表
     *
     * @param iotSensorData 传感器数据
     * @return 传感器数据集合
     */
    public List<IotSensorData> selectIotSensorDataList(IotSensorData iotSensorData);

    /**
     * 新增传感器数据
     *
     * @param iotSensorData 传感器数据
     * @return 结果
     */
    public int insertIotSensorData(IotSensorData iotSensorData);

    /**
     * 修改传感器数据
     *
     * @param iotSensorData 传感器数据
     * @return 结果
     */
    public int updateIotSensorData(IotSensorData iotSensorData);

    /**
     * 删除传感器数据
     *
     * @param dataId 传感器数据主键
     * @return 结果
     */
    public int deleteIotSensorDataByDataId(Long dataId);

    /**
     * 批量删除传感器数据
     *
     * @param dataIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteIotSensorDataByDataIds(Long[] dataIds);

    /**
     * 查询指定灯杆的启用报警规则
     *
     * @param poleId 灯杆ID
     * @return 报警规则集合
     */
    public List<IotAlarmRule> selectEnabledRulesByPoleId(Long poleId);
}
