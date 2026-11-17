package com.iot.mapper;

import java.util.List;
import com.iot.domain.IotSensorData;

/**
 * 传感器数据Mapper接口
 *
 * @author iot
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
     * 按灯杆ID删除传感器数据（停用灯杆时级联清除）
     *
     * @param poleId 灯杆ID
     * @return 结果
     */
    public int deleteIotSensorDataByPoleId(Long poleId);
}
