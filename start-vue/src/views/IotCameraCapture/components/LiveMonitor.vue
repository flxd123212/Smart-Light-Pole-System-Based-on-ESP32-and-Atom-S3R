<template>
  <div class="live-monitor" ref="root" :class="{ 'is-fullscreen': isFullscreen }">
    <!-- ===== 顶部标题栏 ===== -->
    <div class="lm-header">
      <div class="lm-title">
        <span class="lm-dot" :class="online ? 'dot-on' : 'dot-off'"></span>
        <span class="lm-name">{{ title }}</span>
        <span class="lm-badge" :class="online ? 'badge-on' : 'badge-off'">
          {{ online ? '在线' : '离线' }}
        </span>
        <span class="lm-badge badge-ai" v-if="yoloOnline">
          <i class="el-icon-cpu"></i> YOLO
        </span>
        <span class="lm-ip" v-if="cameraIp">{{ cameraIp }}</span>
      </div>
      <div class="lm-actions">
        <el-radio-group v-model="mode" size="mini" @change="restartStream">
          <el-radio-button label="overlay">AI 画框</el-radio-button>
          <el-radio-button label="annotated">服务端标注</el-radio-button>
          <el-radio-button label="raw">原始画面</el-radio-button>
        </el-radio-group>
        <el-button size="mini" icon="el-icon-camera" @click="manualCapture">抓拍</el-button>
        <el-button size="mini" :icon="paused ? 'el-icon-video-play' : 'el-icon-video-pause'"
                   @click="togglePause">{{ paused ? '继续' : '暂停' }}</el-button>
        <el-button size="mini" icon="el-icon-picture-outline" @click="snapshot">截图</el-button>
        <el-button size="mini" icon="el-icon-full-screen" @click="toggleFullscreen"></el-button>
      </div>
    </div>

    <!-- ===== 画面区 ===== -->
    <div class="lm-body">
      <div class="lm-stage" ref="stage" :style="{ height: isFullscreen ? '' : height }">
        <img ref="video" class="lm-video" :src="videoSrc" alt=""
             @load="onFrameLoad" @error="onFrameError" />
        <canvas ref="overlay" class="lm-canvas"></canvas>

        <!-- 检测到人时的醒目提示 -->
        <transition name="lm-fade">
          <div class="lm-alert" v-if="personCount > 0 && !paused">
            <i class="el-icon-user-solid"></i>
            检测到 <b>{{ personCount }}</b> 人
          </div>
        </transition>

        <!-- 左上角实时参数 -->
        <div class="lm-hud">
          <span><i class="el-icon-video-camera"></i> {{ cameraFps }} fps</span>
          <span v-if="latencyMs >= 0"><i class="el-icon-timer"></i> {{ latencyMs }} ms</span>
          <span v-if="mode !== 'raw'"><i class="el-icon-aim"></i> {{ boxes.length }} 目标</span>
        </div>

        <!-- 离线 / 暂停遮罩 -->
        <div class="lm-mask" v-if="!online">
          <i class="el-icon-video-camera-solid"></i>
          <p>{{ offlineText }}</p>
          <span class="lm-mask-tip">{{ baseUrl }}</span>
          <el-button size="mini" type="primary" plain @click="restartStream">重新连接</el-button>
        </div>
        <div class="lm-mask lm-mask-light" v-else-if="paused">
          <i class="el-icon-video-pause"></i>
          <p>已暂停</p>
        </div>
      </div>

      <!-- ===== 右侧检测列表 ===== -->
      <div class="lm-side">
        <div class="lm-side-title">
          <i class="el-icon-data-analysis"></i> 实时识别
          <span class="lm-side-time" v-if="lastUpdate">{{ lastUpdate }}</span>
        </div>
        <div class="lm-count">
          <div class="lm-count-num" :class="{ 'is-active': personCount > 0 }">{{ personCount }}</div>
          <div class="lm-count-label">当前画面行人数</div>
        </div>
        <div class="lm-list">
          <div class="lm-item" v-for="(b, i) in sortedBoxes" :key="i"
               :class="{ 'is-person': b.is_person }">
            <span class="lm-item-name">
              <i :class="b.is_person ? 'el-icon-user-solid' : 'el-icon-s-flag'"></i>
              {{ labelText(b.label) }}
            </span>
            <span class="lm-item-conf">{{ Math.round((b.confidence || 0) * 100) }}%</span>
          </div>
          <div class="lm-empty" v-if="!boxes.length">
            <i class="el-icon-view"></i>
            <span>{{ online ? '画面中暂无目标' : '等待摄像头接入' }}</span>
          </div>
        </div>
        <div class="lm-tip" v-if="fallbackTip">
          <i class="el-icon-warning-outline"></i> {{ fallbackTip }}
        </div>
      </div>
    </div>
  </div>
</template>

<script>
/**
 * 摄像头实时监控 + YOLO 行人识别窗口
 * ------------------------------------------------------------
 * 画面: Flask (默认 5000 端口) 的 MJPEG 流  /api/stream/mjpeg
 * 识别: 每 300ms 拉一次 /api/detect/latest 拿归一化检测框, 用 canvas 叠加绘制
 * 兜底: 老版 YOLO 服务不回坐标时, 自动切到 /api/stream/annotated (服务端画框)
 */
const PERSON_COLOR = '#00e676'
const OTHER_COLOR = '#4facfe'
const CN_LABELS = {
  person: '行人', car: '汽车', bus: '公交车', truck: '卡车',
  bicycle: '自行车', motorcycle: '摩托车', dog: '狗', cat: '猫',
  'traffic light': '红绿灯', umbrella: '雨伞', backpack: '背包',
  handbag: '手提包', cell: '手机', 'cell phone': '手机', chair: '椅子'
}

export default {
  name: 'LiveMonitor',
  props: {
    // Flask AI 服务地址; 默认与前端同主机的 5000 端口
    baseUrl: {
      type: String,
      default() {
        return process.env.VUE_APP_FLASK_BASE_API ||
          `${window.location.protocol}//${window.location.hostname}:5000`
      }
    },
    title: { type: String, default: '摄像头实时监控' },
    height: { type: String, default: '420px' },
    // 检测结果轮询间隔 (ms)
    interval: { type: Number, default: 300 }
  },
  data() {
    return {
      mode: 'overlay',            // overlay | annotated | raw
      paused: false,
      isFullscreen: false,
      streamKey: Date.now(),
      online: false,
      yoloOnline: false,
      cameraIp: '',
      cameraFps: 0,
      latencyMs: -1,
      personCount: 0,
      boxes: [],                  // 服务端返回的原始检测框
      lastUpdate: '',
      fallbackTip: '',
      offlineText: '摄像头未接入 / AI 服务未启动',
      failCount: 0
    }
  },
  computed: {
    videoSrc() {
      const t = this.streamKey
      if (this.paused) return `${this.baseUrl}/api/stream/latest?t=${t}`
      if (this.mode === 'annotated') return `${this.baseUrl}/api/stream/annotated?t=${t}`
      return `${this.baseUrl}/api/stream/mjpeg?t=${t}`
    },
    sortedBoxes() {
      return [...this.boxes].sort((a, b) => {
        if (a.is_person !== b.is_person) return a.is_person ? -1 : 1
        return (b.confidence || 0) - (a.confidence || 0)
      })
    }
  },
  mounted() {
    // 动画用的框状态 (做平滑跟随, 避免每次刷新框在跳)
    this.renderBoxes = []
    this.timer = null
    this.rafId = null
    this.controller = null
    this.watchdog = null
    this.lastFrameAt = 0

    this.onResize = () => this.resizeCanvas()
    window.addEventListener('resize', this.onResize)
    this.onFsChange = () => {
      this.isFullscreen = !!document.fullscreenElement
      this.$nextTick(this.resizeCanvas)
    }
    document.addEventListener('fullscreenchange', this.onFsChange)
    this.onVisibility = () => {
      if (document.hidden) { this.stopPolling() } else if (!this.paused) { this.startPolling() }
    }
    document.addEventListener('visibilitychange', this.onVisibility)

    this.$nextTick(() => {
      this.resizeCanvas()
      this.startPolling()
      this.startRender()
      // 看门狗: MJPEG 连接被中间设备掐断时自动重连
      this.watchdog = setInterval(() => {
        if (this.paused || document.hidden) return
        if (this.lastFrameAt && Date.now() - this.lastFrameAt > 15000) {
          this.restartStream()
        }
      }, 5000)
    })
  },
  beforeDestroy() {
    this.stopped = true
    if (this._retryTimer) clearTimeout(this._retryTimer)
    this.stopPolling()
    if (this.rafId) cancelAnimationFrame(this.rafId)
    if (this.watchdog) clearInterval(this.watchdog)
    window.removeEventListener('resize', this.onResize)
    document.removeEventListener('fullscreenchange', this.onFsChange)
    document.removeEventListener('visibilitychange', this.onVisibility)
    // 必须置空 src, 否则 MJPEG 长连接会一直挂着不释放
    if (this.$refs.video) this.$refs.video.src = ''
  },
  methods: {
    labelText(label) {
      return CN_LABELS[label] || label || '目标'
    },

    // ==================== 检测结果轮询 ====================
    startPolling() {
      this.stopPolling()
      this.poll()
    },
    stopPolling() {
      if (this.timer) { clearTimeout(this.timer); this.timer = null }
      if (this.controller) { try { this.controller.abort() } catch (e) { /* ignore */ } this.controller = null }
    },
    poll() {
      if (this._isDestroyed || this.stopped) return
      this.controller = typeof AbortController !== 'undefined' ? new AbortController() : null
      const opts = this.controller ? { signal: this.controller.signal } : {}
      fetch(`${this.baseUrl}/api/detect/latest?t=${Date.now()}`, opts)
        .then(res => (res.ok ? res.json() : Promise.reject(new Error('HTTP ' + res.status))))
        .then(data => this.applyDetection(data))
        .catch(() => this.onPollError())
        .then(() => {
          const wait = this.online ? this.interval : 2000
          this.timer = setTimeout(() => this.poll(), this.paused ? 2000 : wait)
        })
    },
    applyDetection(data) {
      this.failCount = 0
      this.online = !!data.camera_online
      this.yoloOnline = !!data.yolo_online
      this.cameraIp = data.camera_ip || ''
      this.cameraFps = data.camera_fps || 0
      this.latencyMs = typeof data.latency_ms === 'number' ? data.latency_ms : -1
      this.offlineText = data.camera_online ? '' : '摄像头未接入 (等待设备上传画面)'

      const fresh = data.age_ms >= 0 && data.age_ms < 6000
      const boxes = fresh && Array.isArray(data.boxes) ? data.boxes : []
      const prevCount = this.personCount
      this.boxes = boxes
      this.personCount = fresh ? (data.person_count || 0) : 0
      this.lastUpdate = fresh ? this.nowText() : ''

      // 老版 YOLO 服务不回坐标 -> 自动切到服务端标注流
      if (this.mode === 'overlay' && boxes.length && data.has_bbox === false) {
        this.fallbackTip = 'YOLO 服务未返回坐标, 已切换为服务端标注画面'
        this.mode = 'annotated'
        this.restartStream()
      }

      this.targetBoxes = boxes.filter(b => Array.isArray(b.bbox_norm) && b.bbox_norm.length === 4)
      if (prevCount === 0 && this.personCount > 0) {
        this.$emit('person-detected', this.personCount)
      }
      this.$emit('status', {
        online: this.online, personCount: this.personCount, fps: this.cameraFps
      })
    },
    onPollError() {
      this.failCount++
      if (this.failCount >= 2) {
        this.online = false
        this.yoloOnline = false
        this.boxes = []
        this.targetBoxes = []
        this.personCount = 0
        this.offlineText = 'AI 服务未启动 (请检查 Flask 5000 端口)'
      }
    },
    nowText() {
      const d = new Date()
      const p = n => String(n).padStart(2, '0')
      return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
    },

    // ==================== 画面 ====================
    onFrameLoad() {
      this.lastFrameAt = Date.now()
      this.resizeCanvas()
    },
    onFrameError() {
      // MJPEG 首次连接失败: 交给轮询判断在线状态, 3 秒后自动重试
      if (!this._retryTimer) {
        this._retryTimer = setTimeout(() => {
          this._retryTimer = null
          if (!this.paused) this.restartStream()
        }, 3000)
      }
    },
    restartStream() {
      this.streamKey = Date.now()
      this.lastFrameAt = Date.now()
      this.fallbackTip = this.mode === 'annotated' ? this.fallbackTip : ''
    },
    togglePause() {
      this.paused = !this.paused
      this.restartStream()
      if (!this.paused) this.startPolling()
    },
    toggleFullscreen() {
      const el = this.$refs.root
      if (!document.fullscreenElement) {
        if (el.requestFullscreen) el.requestFullscreen()
      } else if (document.exitFullscreen) {
        document.exitFullscreen()
      }
    },
    manualCapture() {
      // 不带 Content-Type -> 浏览器按简单请求发送, 不触发跨域预检
      fetch(`${this.baseUrl}/api/trigger/set`, {
        method: 'POST',
        body: JSON.stringify({ trigger: true })
      }).then(res => res.json()).then(data => {
        if (data && data.ok) {
          this.$message({ type: 'success', message: '已下发抓拍指令' })
          this.$emit('capture-triggered')
        } else {
          this.$message({ type: 'warning', message: '抓拍指令未被接受' })
        }
      }).catch(() => {
        this.$message({ type: 'error', message: '抓拍失败: AI 服务未连接' })
      })
    },
    snapshot() {
      const url = this.mode === 'raw'
        ? `${this.baseUrl}/api/stream/latest?t=${Date.now()}`
        : `${this.baseUrl}/api/detect/frame?t=${Date.now()}`
      fetch(url).then(res => res.blob()).then(blob => {
        const a = document.createElement('a')
        a.href = URL.createObjectURL(blob)
        a.download = `snapshot_${Date.now()}.jpg`
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        setTimeout(() => URL.revokeObjectURL(a.href), 3000)
      }).catch(() => {
        this.$message({ type: 'error', message: '截图失败: 无法读取画面' })
      })
    },

    // ==================== canvas 画框 ====================
    resizeCanvas() {
      const canvas = this.$refs.overlay
      const stage = this.$refs.stage
      if (!canvas || !stage) return
      const dpr = window.devicePixelRatio || 1
      const w = stage.clientWidth
      const h = stage.clientHeight
      canvas.width = Math.max(1, Math.round(w * dpr))
      canvas.height = Math.max(1, Math.round(h * dpr))
      canvas.style.width = w + 'px'
      canvas.style.height = h + 'px'
      this.dpr = dpr
    },
    // 计算 object-fit: contain 之后画面在容器里的真实位置, 框才不会偏
    videoRect() {
      const img = this.$refs.video
      const stage = this.$refs.stage
      if (!img || !stage) return null
      const cw = stage.clientWidth
      const ch = stage.clientHeight
      const iw = img.naturalWidth || 640
      const ih = img.naturalHeight || 480
      if (!cw || !ch || !iw || !ih) return null
      const scale = Math.min(cw / iw, ch / ih)
      const dw = iw * scale
      const dh = ih * scale
      return { x: (cw - dw) / 2, y: (ch - dh) / 2, w: dw, h: dh }
    },
    startRender() {
      const loop = () => {
        this.rafId = requestAnimationFrame(loop)
        this.drawFrame()
      }
      this.rafId = requestAnimationFrame(loop)
    },
    drawFrame() {
      const canvas = this.$refs.overlay
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      const dpr = this.dpr || 1
      ctx.setTransform(1, 0, 0, 1, 0, 0)
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      // 服务端标注模式 / 原始模式不需要前端画框
      if (this.mode !== 'overlay' || this.paused) { this.renderBoxes = []; return }

      const rect = this.videoRect()
      if (!rect) return
      ctx.scale(dpr, dpr)

      this.renderBoxes = this.tweenBoxes(this.targetBoxes || [])
      const t = Date.now() / 1000
      this.renderBoxes.forEach(b => this.paintBox(ctx, b, rect, t))
    },
    // 用上一帧的框向目标框插值, 让框平滑跟随而不是每 300ms 闪跳
    tweenBoxes(targets) {
      const prev = this.renderBoxes || []
      const used = new Array(prev.length).fill(false)
      const center = bb => [(bb[0] + bb[2]) / 2, (bb[1] + bb[3]) / 2]
      return targets.map(t => {
        const tb = t.bbox_norm
        const tc = center(tb)
        let bestI = -1
        let bestD = 0.12   // 归一化距离阈值: 超过就认为是新目标
        prev.forEach((p, i) => {
          if (used[i]) return
          const pc = center(p.bbox)
          const d = Math.hypot(pc[0] - tc[0], pc[1] - tc[1])
          if (d < bestD) { bestD = d; bestI = i }
        })
        if (bestI >= 0) {
          used[bestI] = true
          const p = prev[bestI]
          const k = 0.35   // 缓动系数
          const bbox = p.bbox.map((v, i) => v + (tb[i] - v) * k)
          return { ...t, bbox, alpha: Math.min(1, (p.alpha || 0) + 0.25) }
        }
        return { ...t, bbox: tb.slice(), alpha: 0.35 }
      })
    },
    paintBox(ctx, box, rect, t) {
      const [nx1, ny1, nx2, ny2] = box.bbox
      const x = rect.x + nx1 * rect.w
      const y = rect.y + ny1 * rect.h
      const w = (nx2 - nx1) * rect.w
      const h = (ny2 - ny1) * rect.h
      if (w <= 1 || h <= 1) return

      const person = box.is_person
      const color = person ? PERSON_COLOR : OTHER_COLOR
      const alpha = box.alpha == null ? 1 : box.alpha
      ctx.save()
      ctx.globalAlpha = alpha

      // 半透明底 + 主体描边
      ctx.fillStyle = person ? 'rgba(0, 230, 118, 0.10)' : 'rgba(79, 172, 254, 0.08)'
      ctx.fillRect(x, y, w, h)
      ctx.strokeStyle = color
      ctx.lineWidth = person ? 2 : 1.5
      ctx.setLineDash([])
      ctx.strokeRect(x, y, w, h)

      // 四角加粗直角 (监控画面的经典视觉)
      const c = Math.min(18, w / 3, h / 3)
      ctx.lineWidth = person ? 4 : 3
      ctx.beginPath()
      ctx.moveTo(x, y + c); ctx.lineTo(x, y); ctx.lineTo(x + c, y)
      ctx.moveTo(x + w - c, y); ctx.lineTo(x + w, y); ctx.lineTo(x + w, y + c)
      ctx.moveTo(x, y + h - c); ctx.lineTo(x, y + h); ctx.lineTo(x + c, y + h)
      ctx.moveTo(x + w - c, y + h); ctx.lineTo(x + w, y + h); ctx.lineTo(x + w, y + h - c)
      ctx.stroke()

      // 行人框呼吸光晕, 让"有人"更显眼
      if (person) {
        ctx.globalAlpha = alpha * (0.25 + 0.2 * Math.sin(t * 3))
        ctx.lineWidth = 6
        ctx.strokeRect(x - 3, y - 3, w + 6, h + 6)
        ctx.globalAlpha = alpha
      }

      // 标签
      const text = `${this.labelText(box.label)} ${Math.round((box.confidence || 0) * 100)}%`
      ctx.font = '600 12px -apple-system, "Microsoft YaHei", sans-serif'
      const tw = ctx.measureText(text).width + 12
      const th = 18
      const ty = y - th < rect.y ? y + 2 : y - th - 2
      ctx.fillStyle = color
      ctx.fillRect(x, ty, tw, th)
      ctx.fillStyle = person ? '#08341c' : '#04233c'
      ctx.textBaseline = 'middle'
      ctx.fillText(text, x + 6, ty + th / 2 + 0.5)
      ctx.restore()
    }
  }
}
</script>

<style scoped>
.live-monitor {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.live-monitor.is-fullscreen {
  border-radius: 0;
  display: flex;
  flex-direction: column;
  background: #0d1117;
}

.is-fullscreen .lm-body { flex: 1; }
.is-fullscreen .lm-stage { height: 100% !important; }

/* ---------- 头部 ---------- */
.lm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #2b3a55 0%, #1b2436 100%);
  color: #fff;
}

.lm-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; }
.lm-name { letter-spacing: 0.5px; }

.lm-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.dot-on { background: #00e676; box-shadow: 0 0 0 0 rgba(0, 230, 118, 0.7); animation: lm-pulse 1.8s infinite; }
.dot-off { background: #909399; }

@keyframes lm-pulse {
  0% { box-shadow: 0 0 0 0 rgba(0, 230, 118, 0.7); }
  70% { box-shadow: 0 0 0 8px rgba(0, 230, 118, 0); }
  100% { box-shadow: 0 0 0 0 rgba(0, 230, 118, 0); }
}

.lm-badge {
  font-size: 12px;
  font-weight: 500;
  padding: 1px 8px;
  border-radius: 10px;
}
.badge-on { background: rgba(0, 230, 118, 0.18); color: #00e676; }
.badge-off { background: rgba(255, 255, 255, 0.12); color: #c0c4cc; }
.badge-ai { background: rgba(79, 172, 254, 0.2); color: #4facfe; }

.lm-ip { font-size: 12px; color: #a6b1c2; font-family: Consolas, Monaco, monospace; }

.lm-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

/* ---------- 画面 ---------- */
.lm-body { display: flex; }

.lm-stage {
  position: relative;
  flex: 1;
  min-width: 0;
  background: #05070c;
  overflow: hidden;
}

.lm-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.lm-canvas {
  position: absolute;
  left: 0;
  top: 0;
  pointer-events: none;
}

.lm-hud {
  position: absolute;
  left: 12px;
  top: 12px;
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #e6f1ff;
  background: rgba(0, 0, 0, 0.42);
  padding: 4px 10px;
  border-radius: 14px;
  backdrop-filter: blur(2px);
}

.lm-alert {
  position: absolute;
  right: 12px;
  top: 12px;
  background: linear-gradient(135deg, #ff5f6d, #ff2d55);
  color: #fff;
  font-size: 13px;
  padding: 5px 12px;
  border-radius: 14px;
  box-shadow: 0 4px 14px rgba(255, 45, 85, 0.45);
}
.lm-alert b { font-size: 15px; margin: 0 2px; }

.lm-fade-enter-active, .lm-fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.lm-fade-enter, .lm-fade-leave-to { opacity: 0; transform: translateY(-6px); }

.lm-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #8c96a8;
  background: rgba(5, 7, 12, 0.86);
}
.lm-mask i { font-size: 40px; }
.lm-mask p { margin: 0; font-size: 14px; }
.lm-mask-tip { font-size: 12px; color: #5b6472; font-family: Consolas, Monaco, monospace; margin-bottom: 6px; }
.lm-mask-light { background: rgba(5, 7, 12, 0.45); }

/* ---------- 右侧列表 ---------- */
.lm-side {
  width: 210px;
  flex: 0 0 210px;
  background: #f7f9fc;
  border-left: 1px solid #ebeef5;
  padding: 12px;
  display: flex;
  flex-direction: column;
}
.is-fullscreen .lm-side { background: #131a26; border-color: #222c3c; color: #c9d4e4; }

.lm-side-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 4px;
}
.is-fullscreen .lm-side-title { color: #e6f1ff; }
.lm-side-time { margin-left: auto; font-size: 11px; color: #909399; font-weight: 400; }

.lm-count { text-align: center; padding: 14px 0 10px; }
.lm-count-num {
  font-size: 40px;
  font-weight: 700;
  line-height: 1;
  color: #c0c4cc;
  transition: color 0.3s;
}
.lm-count-num.is-active { color: #00b25a; }
.lm-count-label { font-size: 12px; color: #909399; margin-top: 4px; }

.lm-list { flex: 1; overflow-y: auto; max-height: 240px; }

.lm-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  padding: 6px 8px;
  border-radius: 6px;
  background: #fff;
  border: 1px solid #ebeef5;
  margin-bottom: 6px;
  color: #606266;
}
.is-fullscreen .lm-item { background: #1b2434; border-color: #2a3547; color: #b6c2d4; }
.lm-item.is-person { border-color: #00e676; color: #067a41; background: rgba(0, 230, 118, 0.08); }
.lm-item-name { display: flex; align-items: center; gap: 4px; }
.lm-item-conf { font-family: Consolas, Monaco, monospace; font-weight: 600; }

.lm-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #b7bcc5;
  font-size: 12px;
  padding: 20px 0;
}
.lm-empty i { font-size: 22px; }

.lm-tip {
  font-size: 11px;
  color: #e6a23c;
  line-height: 1.5;
  border-top: 1px dashed #ebeef5;
  padding-top: 8px;
  margin-top: 8px;
}

@media (max-width: 900px) {
  .lm-body { flex-direction: column; }
  .lm-side { width: auto; flex: none; border-left: none; border-top: 1px solid #ebeef5; }
  .lm-list { max-height: 150px; }
}
</style>
