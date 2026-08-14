package com.iot.service.impl;

import java.util.List;
import com.iotlight.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.iot.mapper.IotControlLogMapper;
import com.iot.domain.IotControlLog;
import com.iot.service.IIotControlLogService;

/**
 * 控制日志Service业务层处理
 * 
 * @author extrao
 * @date 2026-07-24
 */
@Service
public class IotControlLogServiceImpl implements IIotControlLogService 
{
    @Autowired
    private IotControlLogMapper iotControlLogMapper;

    /**
     * 查询控制日志
     * 
     * @param logId 控制日志主键
     * @return 控制日志
     */
    @Override
    public IotControlLog selectIotControlLogByLogId(Long logId)
    {
        return iotControlLogMapper.selectIotControlLogByLogId(logId);
    }

    /**
     * 查询控制日志列表
     * 
     * @param iotControlLog 控制日志
     * @return 控制日志
     */
    @Override
    public List<IotControlLog> selectIotControlLogList(IotControlLog iotControlLog)
    {
        return iotControlLogMapper.selectIotControlLogList(iotControlLog);
    }

    /**
     * 新增控制日志
     * 
     * @param iotControlLog 控制日志
     * @return 结果
     */
    @Override
    public int insertIotControlLog(IotControlLog iotControlLog)
    {
        iotControlLog.setCreateTime(DateUtils.getNowDate());
        return iotControlLogMapper.insertIotControlLog(iotControlLog);
    }

    /**
     * 修改控制日志
     * 
     * @param iotControlLog 控制日志
     * @return 结果
     */
    @Override
    public int updateIotControlLog(IotControlLog iotControlLog)
    {
        return iotControlLogMapper.updateIotControlLog(iotControlLog);
    }

    /**
     * 批量删除控制日志
     * 
     * @param logIds 需要删除的控制日志主键
     * @return 结果
     */
    @Override
    public int deleteIotControlLogByLogIds(Long[] logIds)
    {
        return iotControlLogMapper.deleteIotControlLogByLogIds(logIds);
    }

    /**
     * 删除控制日志信息
     * 
     * @param logId 控制日志主键
     * @return 结果
     */
    @Override
    public int deleteIotControlLogByLogId(Long logId)
    {
        return iotControlLogMapper.deleteIotControlLogByLogId(logId);
    }
}
