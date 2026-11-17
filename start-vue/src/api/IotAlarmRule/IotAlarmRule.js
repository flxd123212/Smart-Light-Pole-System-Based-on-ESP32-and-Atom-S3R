import request from '@/utils/request'

// 查询报警规则列表
export function listIotAlarmRule(query) {
  return request({
    url: '/IotAlarmRule/IotAlarmRule/list',
    method: 'get',
    params: query
  })
}

// 查询报警规则详细
export function getIotAlarmRule(ruleId) {
  return request({
    url: '/IotAlarmRule/IotAlarmRule/' + ruleId,
    method: 'get'
  })
}

// 新增报警规则
export function addIotAlarmRule(data) {
  return request({
    url: '/IotAlarmRule/IotAlarmRule',
    method: 'post',
    data: data
  })
}

// 修改报警规则
export function updateIotAlarmRule(data) {
  return request({
    url: '/IotAlarmRule/IotAlarmRule',
    method: 'put',
    data: data
  })
}

// 删除报警规则
export function delIotAlarmRule(ruleId) {
  return request({
    url: '/IotAlarmRule/IotAlarmRule/' + ruleId,
    method: 'delete'
  })
}
