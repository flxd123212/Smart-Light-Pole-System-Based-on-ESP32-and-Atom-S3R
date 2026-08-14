package com.iot.service.impl;

import java.util.List;
import com.iotlight.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.iot.mapper.IotAlarmRecordMapper;
import com.iot.domain.IotAlarmRecord;
import com.iot.service.IIotAlarmRecordService;

/**
 * 报警记录Service业务层处理
 * 
 * @author extrao
 * @date 2026-07-24
 */
@Service
public class IotAlarmRecordServiceImpl implements IIotAlarmRecordService 
{
    @Autowired
    private IotAlarmRecordMapper iotAlarmRecordMapper;

    /**
     * 查询报警记录
     * 
     * @param recordId 报警记录主键
     * @return 报警记录
     */
    @Override
    public IotAlarmRecord selectIotAlarmRecordByRecordId(Long recordId)
    {
        return iotAlarmRecordMapper.selectIotAlarmRecordByRecordId(recordId);
    }

    /**
     * 查询报警记录列表
     * 
     * @param iotAlarmRecord 报警记录
     * @return 报警记录
     */
    @Override
    public List<IotAlarmRecord> selectIotAlarmRecordList(IotAlarmRecord iotAlarmRecord)
    {
        return iotAlarmRecordMapper.selectIotAlarmRecordList(iotAlarmRecord);
    }

    /**
     * 新增报警记录
     * 
     * @param iotAlarmRecord 报警记录
     * @return 结果
     */
    @Override
    public int insertIotAlarmRecord(IotAlarmRecord iotAlarmRecord)
    {
        iotAlarmRecord.setCreateTime(DateUtils.getNowDate());
        return iotAlarmRecordMapper.insertIotAlarmRecord(iotAlarmRecord);
    }

    /**
     * 修改报警记录
     * 
     * @param iotAlarmRecord 报警记录
     * @return 结果
     */
    @Override
    public int updateIotAlarmRecord(IotAlarmRecord iotAlarmRecord)
    {
        return iotAlarmRecordMapper.updateIotAlarmRecord(iotAlarmRecord);
    }

    /**
     * 批量删除报警记录
     * 
     * @param recordIds 需要删除的报警记录主键
     * @return 结果
     */
    @Override
    public int deleteIotAlarmRecordByRecordIds(Long[] recordIds)
    {
        return iotAlarmRecordMapper.deleteIotAlarmRecordByRecordIds(recordIds);
    }

    /**
     * 删除报警记录信息
     * 
     * @param recordId 报警记录主键
     * @return 结果
     */
    @Override
    public int deleteIotAlarmRecordByRecordId(Long recordId)
    {
        return iotAlarmRecordMapper.deleteIotAlarmRecordByRecordId(recordId);
    }
}
