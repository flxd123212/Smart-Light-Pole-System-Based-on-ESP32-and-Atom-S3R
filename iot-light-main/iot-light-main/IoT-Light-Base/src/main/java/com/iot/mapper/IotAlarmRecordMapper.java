package com.iot.mapper;

import java.util.List;
import com.iot.domain.IotAlarmRecord;

/**
 * 报警记录Mapper接口
 *
 * @author extrao
 * @date 2026-07-24
 */
public interface IotAlarmRecordMapper
{
    /**
     * 查询报警记录
     *
     * @param recordId 报警记录主键
     * @return 报警记录
     */
    public IotAlarmRecord selectIotAlarmRecordByRecordId(Long recordId);

    /**
     * 查询报警记录列表
     *
     * @param iotAlarmRecord 报警记录
     * @return 报警记录集合
     */
    public List<IotAlarmRecord> selectIotAlarmRecordList(IotAlarmRecord iotAlarmRecord);

    /**
     * 新增报警记录
     *
     * @param iotAlarmRecord 报警记录
     * @return 结果
     */
    public int insertIotAlarmRecord(IotAlarmRecord iotAlarmRecord);

    /**
     * 修改报警记录
     *
     * @param iotAlarmRecord 报警记录
     * @return 结果
     */
    public int updateIotAlarmRecord(IotAlarmRecord iotAlarmRecord);

    /**
     * 删除报警记录
     *
     * @param recordId 报警记录主键
     * @return 结果
     */
    public int deleteIotAlarmRecordByRecordId(Long recordId);

    /**
     * 批量删除报警记录
     *
     * @param recordIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteIotAlarmRecordByRecordIds(Long[] recordIds);

}
