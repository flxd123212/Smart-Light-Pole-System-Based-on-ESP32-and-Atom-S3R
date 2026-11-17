import request from '@/utils/request'

// 查询控制日志列表
export function listIotControlLog(query) {
  return request({
    url: '/IotControlLog/IotControlLog/list',
    method: 'get',
    params: query
  })
}

// 查询控制日志详细
export function getIotControlLog(logId) {
  return request({
    url: '/IotControlLog/IotControlLog/' + logId,
    method: 'get'
  })
}

// 新增控制日志
export function addIotControlLog(data) {
  return request({
    url: '/IotControlLog/IotControlLog',
    method: 'post',
    data: data
  })
}

// 修改控制日志
export function updateIotControlLog(data) {
  return request({
    url: '/IotControlLog/IotControlLog',
    method: 'put',
    data: data
  })
}

// 删除控制日志
export function delIotControlLog(logId) {
  return request({
    url: '/IotControlLog/IotControlLog/' + logId,
    method: 'delete'
  })
}
