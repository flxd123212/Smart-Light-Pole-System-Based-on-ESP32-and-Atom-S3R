import request from '@/utils/request'

// 查询设备列表
export function listIotDevice(query) {
  return request({
    url: '/IotDevice/IotDevice/list',
    method: 'get',
    params: query
  })
}

// 查询设备详细
export function getIotDevice(deviceId) {
  return request({
    url: '/IotDevice/IotDevice/' + deviceId,
    method: 'get'
  })
}

// 新增设备
export function addIotDevice(data) {
  return request({
    url: '/IotDevice/IotDevice',
    method: 'post',
    data: data
  })
}

// 修改设备
export function updateIotDevice(data) {
  return request({
    url: '/IotDevice/IotDevice',
    method: 'put',
    data: data
  })
}

// 删除设备
export function delIotDevice(deviceId) {
  return request({
    url: '/IotDevice/IotDevice/' + deviceId,
    method: 'delete'
  })
}
