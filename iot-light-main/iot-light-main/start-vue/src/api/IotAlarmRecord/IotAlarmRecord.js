import request from '@/utils/request'

// 查询报警记录列表
export function listIotAlarmRecord(query) {
  return request({
    url: '/IotAlarmRecord/IotAlarmRecord/list',
    method: 'get',
    params: query
  })
}

// 查询报警记录详细
export function getIotAlarmRecord(recordId) {
  return request({
    url: '/IotAlarmRecord/IotAlarmRecord/' + recordId,
    method: 'get'
  })
}

// 新增报警记录
export function addIotAlarmRecord(data) {
  return request({
    url: '/IotAlarmRecord/IotAlarmRecord',
    method: 'post',
    data: data
  })
}

// 修改报警记录
export function updateIotAlarmRecord(data) {
  return request({
    url: '/IotAlarmRecord/IotAlarmRecord',
    method: 'put',
    data: data
  })
}

// 删除报警记录
export function delIotAlarmRecord(recordId) {
  return request({
    url: '/IotAlarmRecord/IotAlarmRecord/' + recordId,
    method: 'delete'
  })
}
