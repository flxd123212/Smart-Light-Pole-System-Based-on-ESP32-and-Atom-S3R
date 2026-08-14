package com.iot.service;

import java.util.List;
import com.iot.domain.IotControlLog;

/**
 * 控制日志Service接口
 * 
 * @author extrao
 * @date 2026-07-24
 */
public interface IIotControlLogService 
{
    /**
     * 查询控制日志
     * 
     * @param logId 控制日志主键
     * @return 控制日志
     */
    public IotControlLog selectIotControlLogByLogId(Long logId);

    /**
     * 查询控制日志列表
     * 
     * @param iotControlLog 控制日志
     * @return 控制日志集合
     */
    public List<IotControlLog> selectIotControlLogList(IotControlLog iotControlLog);

    /**
     * 新增控制日志
     * 
     * @param iotControlLog 控制日志
     * @return 结果
     */
    public int insertIotControlLog(IotControlLog iotControlLog);

    /**
     * 修改控制日志
     * 
     * @param iotControlLog 控制日志
     * @return 结果
     */
    public int updateIotControlLog(IotControlLog iotControlLog);

    /**
     * 批量删除控制日志
     * 
     * @param logIds 需要删除的控制日志主键集合
     * @return 结果
     */
    public int deleteIotControlLogByLogIds(Long[] logIds);

    /**
     * 删除控制日志信息
     * 
     * @param logId 控制日志主键
     * @return 结果
     */
    public int deleteIotControlLogByLogId(Long logId);
}
