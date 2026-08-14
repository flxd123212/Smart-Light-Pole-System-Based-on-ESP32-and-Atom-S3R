import request from '@/utils/request'

// 查询摄像头抓拍记录列表
export function listIotCameraCapture(query) {
  return request({
    url: '/IotCameraCapture/IotCameraCapture/list',
    method: 'get',
    params: query
  })
}

// 查询摄像头抓拍记录详细
export function getIotCameraCapture(captureId) {
  return request({
    url: '/IotCameraCapture/IotCameraCapture/' + captureId,
    method: 'get'
  })
}

// 新增摄像头抓拍记录
export function addIotCameraCapture(data) {
  return request({
    url: '/IotCameraCapture/IotCameraCapture',
    method: 'post',
    data: data
  })
}

// 修改摄像头抓拍记录
export function updateIotCameraCapture(data) {
  return request({
    url: '/IotCameraCapture/IotCameraCapture',
    method: 'put',
    data: data
  })
}

// 删除摄像头抓拍记录
export function delIotCameraCapture(captureId) {
  return request({
    url: '/IotCameraCapture/IotCameraCapture/' + captureId,
    method: 'delete'
  })
}
