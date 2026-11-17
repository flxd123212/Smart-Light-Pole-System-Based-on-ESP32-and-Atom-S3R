package com.iot.mapper;

import java.util.List;
import com.iot.domain.IotAlarmRule;

/**
 * 报警规则Mapper接口
 *
 * @author iot
 * @date 2026-07-24
 */
public interface IotAlarmRuleMapper
{
  /**
   * 查询报警规则
   *
   * @param ruleId 报警规则主键
   * @return 报警规则
   */
  public IotAlarmRule selectIotAlarmRuleByRuleId(Long ruleId);

  /**
   * 查询报警规则列表
   *
   * @param iotAlarmRule 报警规则
   * @return 报警规则集合
   */
  public List<IotAlarmRule> selectIotAlarmRuleList(IotAlarmRule iotAlarmRule);

  /**
   * 新增报警规则
   *
   * @param iotAlarmRule 报警规则
   * @return 结果
   */
  public int insertIotAlarmRule(IotAlarmRule iotAlarmRule);

  /**
   * 修改报警规则
   *
   * @param iotAlarmRule 报警规则
   * @return 结果
   */
  public int updateIotAlarmRule(IotAlarmRule iotAlarmRule);

  /**
   * 删除报警规则
   *
   * @param ruleId 报警规则主键
   * @return 结果
   */
  public int deleteIotAlarmRuleByRuleId(Long ruleId);

  /**
   * 批量删除报警规则
   *
   * @param ruleIds 需要删除的数据主键集合
   * @return 结果
   */
  public int deleteIotAlarmRuleByRuleIds(Long[] ruleIds);
}
