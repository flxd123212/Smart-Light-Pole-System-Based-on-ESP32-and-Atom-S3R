import request from '@/utils/request'

// 查询传感器数据列表
export function listIotSensorData(query) {
  return request({
    url: '/IotSensorData/IotSensorData/list',
    method: 'get',
    params: query
  })
}

// 查询传感器数据详细
export function getIotSensorData(dataId) {
  return request({
    url: '/IotSensorData/IotSensorData/' + dataId,
    method: 'get'
  })
}

// 新增传感器数据
export function addIotSensorData(data) {
  return request({
    url: '/IotSensorData/IotSensorData',
    method: 'post',
    data: data
  })
}

// 修改传感器数据
export function updateIotSensorData(data) {
  return request({
    url: '/IotSensorData/IotSensorData',
    method: 'put',
    data: data
  })
}

// 删除传感器数据
export function delIotSensorData(dataId) {
  return request({
    url: '/IotSensorData/IotSensorData/' + dataId,
    method: 'delete'
  })
}
