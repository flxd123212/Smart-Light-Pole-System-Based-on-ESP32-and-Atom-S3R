package com.iot.service.impl;

import java.util.List;
import com.iotlight.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.iot.mapper.IotAlarmRuleMapper;
import com.iot.domain.IotAlarmRule;
import com.iot.service.IIotAlarmRuleService;

/**
 * 报警规则Service业务层处理
 * 
 * @author extrao
 * @date 2026-07-24
 */
@Service
public class IotAlarmRuleServiceImpl implements IIotAlarmRuleService 
{
    @Autowired
    private IotAlarmRuleMapper iotAlarmRuleMapper;

    /**
     * 查询报警规则
     * 
     * @param ruleId 报警规则主键
     * @return 报警规则
     */
    @Override
    public IotAlarmRule selectIotAlarmRuleByRuleId(Long ruleId)
    {
        return iotAlarmRuleMapper.selectIotAlarmRuleByRuleId(ruleId);
    }

    /**
     * 查询报警规则列表
     * 
     * @param iotAlarmRule 报警规则
     * @return 报警规则
     */
    @Override
    public List<IotAlarmRule> selectIotAlarmRuleList(IotAlarmRule iotAlarmRule)
    {
        return iotAlarmRuleMapper.selectIotAlarmRuleList(iotAlarmRule);
    }

    /**
     * 新增报警规则
     * 
     * @param iotAlarmRule 报警规则
     * @return 结果
     */
    @Override
    public int insertIotAlarmRule(IotAlarmRule iotAlarmRule)
    {
        iotAlarmRule.setCreateTime(DateUtils.getNowDate());
        return iotAlarmRuleMapper.insertIotAlarmRule(iotAlarmRule);
    }

    /**
     * 修改报警规则
     * 
     * @param iotAlarmRule 报警规则
     * @return 结果
     */
    @Override
    public int updateIotAlarmRule(IotAlarmRule iotAlarmRule)
    {
        iotAlarmRule.setUpdateTime(DateUtils.getNowDate());
        return iotAlarmRuleMapper.updateIotAlarmRule(iotAlarmRule);
    }

    /**
     * 批量删除报警规则
     * 
     * @param ruleIds 需要删除的报警规则主键
     * @return 结果
     */
    @Override
    public int deleteIotAlarmRuleByRuleIds(Long[] ruleIds)
    {
        return iotAlarmRuleMapper.deleteIotAlarmRuleByRuleIds(ruleIds);
    }

    /**
     * 删除报警规则信息
     * 
     * @param ruleId 报警规则主键
     * @return 结果
     */
    @Override
    public int deleteIotAlarmRuleByRuleId(Long ruleId)
    {
        return iotAlarmRuleMapper.deleteIotAlarmRuleByRuleId(ruleId);
    }
}
