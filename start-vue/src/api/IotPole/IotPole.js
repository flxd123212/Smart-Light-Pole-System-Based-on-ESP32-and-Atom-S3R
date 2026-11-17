import request from '@/utils/request'

// 查询灯杆列表
export function listIotPole(query) {
  return request({
    url: '/IotPole/IotPole/list',
    method: 'get',
    params: query
  })
}

// 查询灯杆详细
export function getIotPole(poleId) {
  return request({
    url: '/IotPole/IotPole/' + poleId,
    method: 'get'
  })
}

// 新增灯杆
export function addIotPole(data) {
  return request({
    url: '/IotPole/IotPole',
    method: 'post',
    data: data
  })
}

// 修改灯杆
export function updateIotPole(data) {
  return request({
    url: '/IotPole/IotPole',
    method: 'put',
    data: data
  })
}

// 删除灯杆
export function delIotPole(poleId) {
  return request({
    url: '/IotPole/IotPole/' + poleId,
    method: 'delete'
  })
}
