import request from '@/utils/request'

/**
 * 摄像头实时监控 — Flask(:5000) / YOLO(:5001) 接口封装
 * ====================================================
 * Flask 与 RuoYi 前端不同源, 这些接口不走 @/utils/request (它会加 token 和
 * /dev-api 前缀), 统一用原生 fetch 直连; Flask 端已放行 CORS。
 *
 * 地址优先级: localStorage 手工设置 > .env(VUE_APP_FLASK_BASE_API) > 当前主机:5000
 */

const FLASK_KEY = 'iot-camera-flask-base'
const YOLO_KEY = 'iot-camera-yolo-base'

function trimSlash(url) {
  return String(url || '').trim().replace(/\/+$/, '')
}

/** Flask 默认地址: 跟随当前访问的主机名, 换网段无需改代码 */
export function defaultFlaskBase() {
  return trimSlash(
    process.env.VUE_APP_FLASK_BASE_API ||
      `${window.location.protocol}//${window.location.hostname}:5000`
  )
}

/** YOLO 默认地址: 与 Flask 同机, 端口 5001 */
export function defaultYoloBase() {
  if (process.env.VUE_APP_YOLO_BASE_API) return trimSlash(process.env.VUE_APP_YOLO_BASE_API)
  return defaultFlaskBase().replace(/:5000$/, ':5001')
}

export function getFlaskBase() {
  return trimSlash(localStorage.getItem(FLASK_KEY) || defaultFlaskBase())
}

export function getYoloBase() {
  const saved = localStorage.getItem(YOLO_KEY)
  if (saved) return trimSlash(saved)
  const flask = localStorage.getItem(FLASK_KEY)
  if (flask) return trimSlash(flask).replace(/:\d+$/, ':5001')
  return defaultYoloBase()
}

export function saveFlaskBase(flask, yolo) {
  if (flask) localStorage.setItem(FLASK_KEY, trimSlash(flask))
  else localStorage.removeItem(FLASK_KEY)
  if (yolo) localStorage.setItem(YOLO_KEY, trimSlash(yolo))
  else localStorage.removeItem(YOLO_KEY)
}

export function resetBase() {
  localStorage.removeItem(FLASK_KEY)
  localStorage.removeItem(YOLO_KEY)
}

// ==================== 视频流地址 (直接喂给 <img>) ====================

/** 原始 MJPEG 实时流 (前端自己画框时用) */
export function mjpegUrl(base) {
  return `${base || getFlaskBase()}/api/stream/mjpeg?t=${Date.now()}`
}

/** 服务端已画好框的 MJPEG 流 (兜底方案) */
export function annotatedUrl(base) {
  return `${base || getFlaskBase()}/api/stream/annotated?t=${Date.now()}`
}

/** 最新一帧原图 (轮询模式) */
export function latestFrameUrl(base) {
  return `${base || getFlaskBase()}/api/stream/latest?t=${Date.now()}`
}

/** 最新一帧 AI 标注图 (截图用) */
export function detectFrameUrl(base) {
  return `${base || getFlaskBase()}/api/detect/frame?t=${Date.now()}`
}

/** 抓拍图片地址 */
export function photoUrl(name, base) {
  const b = base || getFlaskBase()
  if (!name) return ''
  if (/^https?:\/\//.test(name)) return name
  if (name.startsWith('/api/photo/')) return b + name
  return `${b}/api/photo/${name}`
}

// ==================== JSON 接口 ====================

function jsonFetch(url, options = {}, timeout = 6000) {
  // fetch 没有超时参数, Flask 挂掉时会一直 pending, 这里用 AbortController 兜底
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeout)
  return fetch(url, { cache: 'no-store', signal: controller.signal, ...options })
    .then(res => {
      if (!res.ok) throw new Error('HTTP ' + res.status)
      return res.json()
    })
    .finally(() => clearTimeout(timer))
}

/** 最近一次 YOLO 检测结果 (归一化坐标); 调用即告诉 Flask "有人在看", 它会切高频推理 */
export function fetchDetection(base) {
  return jsonFetch(`${base || getFlaskBase()}/api/detect/latest?t=${Date.now()}`)
}

/** 摄像头/YOLO/MQTT 状态汇总 */
export function fetchStatus(base) {
  return jsonFetch(`${base || getFlaskBase()}/api/status?t=${Date.now()}`)
}

/** 手动触发一次抓拍 (ESP32 轮询到后让 AtomS3R 拍照并入库) */
export function triggerCapture(base) {
  return jsonFetch(`${base || getFlaskBase()}/api/trigger/set`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ trigger: true })
  })
}

/** 图库列表 (按时间倒序的文件名数组) */
export function listPhotos(base) {
  return jsonFetch(`${base || getFlaskBase()}/api/photos?t=${Date.now()}`)
}

/** 直接调 YOLO 推理 (本机摄像头调试模式用, YOLO 服务已开 CORS) */
export function inferImage(dataUrl, conf = 0.3, base) {
  return jsonFetch(
    `${base || getYoloBase()}/infer`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ image: dataUrl, annotate: false, conf })
    },
    15000
  )
}

/** 后端 MQTT 抓拍指令 (走 RuoYi 网关, 需要登录态) */
export function mqttCapture(cameraCode) {
  return request({
    url: '/mqtt/camera/' + cameraCode + '/capture',
    method: 'post'
  })
}
