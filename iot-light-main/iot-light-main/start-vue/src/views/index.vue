<template>
  <div class="app-container dashboard-home">
    <div class="hero-section">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <div class="hero-text">
          <h1 class="hero-title">智能灯杆管理系统</h1>
          <p class="hero-subtitle">城市智慧照明 · 万物互联 · 实时监控</p>
        </div>
        <div class="hero-stats">
          <div class="hero-stat">
            <div class="stat-icon lamp-icon">
              <i class="el-icon-lamp"></i>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ poleCount }}</span>
              <span class="stat-label">灯杆总数</span>
            </div>
          </div>
          <div class="hero-stat">
            <div class="stat-icon device-icon">
              <i class="el-icon-device"></i>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ deviceCount }}</span>
              <span class="stat-label">设备数量</span>
            </div>
          </div>
          <div class="hero-stat">
            <div class="stat-icon sensor-icon">
              <i class="el-icon-data-line"></i>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ sensorCount }}</span>
              <span class="stat-label">传感器数据</span>
            </div>
          </div>
          <div class="hero-stat">
            <div class="stat-icon alarm-icon">
              <i class="el-icon-bell"></i>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ alarmCount }}</span>
              <span class="stat-label">今日告警</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="main-content">
      <div class="section-row">
        <div class="section-card overview-card">
          <div class="card-header">
            <h3 class="card-title">系统概览</h3>
            <span class="card-subtitle">实时监控数据</span>
          </div>
          <div class="overview-grid">
            <div class="overview-item">
              <div class="item-header">
                <span class="item-label">在线灯杆</span>
                <span class="item-percent">98%</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill bg-green" style="width: 98%"></div>
              </div>
              <div class="item-value">{{ onlinePoles }} / {{ poleCount }}</div>
            </div>
            <div class="overview-item">
              <div class="item-header">
                <span class="item-label">正常设备</span>
                <span class="item-percent">95%</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill bg-blue" style="width: 95%"></div>
              </div>
              <div class="item-value">{{ normalDevices }} / {{ deviceCount }}</div>
            </div>
            <div class="overview-item">
              <div class="item-header">
                <span class="item-label">数据采集率</span>
                <span class="item-percent">99%</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill bg-purple" style="width: 99%"></div>
              </div>
              <div class="item-value">{{ sensorCount }} 条</div>
            </div>
            <div class="overview-item">
              <div class="item-header">
                <span class="item-label">告警处理</span>
                <span class="item-percent">85%</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill bg-orange" style="width: 85%"></div>
              </div>
              <div class="item-value">{{ handledAlarms }} / {{ alarmCount }}</div>
            </div>
          </div>
        </div>

        <div class="section-card quick-chart-card">
          <div class="card-header">
            <h3 class="card-title">设备状态分布</h3>
          </div>
          <div ref="statusChart" class="mini-chart"></div>
        </div>
      </div>

      <div class="section-row">
        <div class="section-card nav-grid-card">
          <div class="card-header">
            <h3 class="card-title">功能导航</h3>
          </div>
          <div class="nav-grid">
            <div class="nav-item" @click="goToPage('/system/IotPole')">
              <div class="nav-icon nav-lamp">
                <i class="el-icon-lamp"></i>
              </div>
              <div class="nav-info">
                <span class="nav-title">灯杆管理</span>
                <span class="nav-desc">管理所有智能灯杆</span>
              </div>
              <div class="nav-arrow">
                <i class="el-icon-arrow-right"></i>
              </div>
            </div>
            <div class="nav-item" @click="goToPage('/system/IotDevice')">
              <div class="nav-icon nav-device">
                <i class="el-icon-device"></i>
              </div>
              <div class="nav-info">
                <span class="nav-title">设备管理</span>
                <span class="nav-desc">监控各类设备状态</span>
              </div>
              <div class="nav-arrow">
                <i class="el-icon-arrow-right"></i>
              </div>
            </div>
            <div class="nav-item" @click="goToPage('/system/IotSensorData')">
              <div class="nav-icon nav-sensor">
                <i class="el-icon-data-line"></i>
              </div>
              <div class="nav-info">
                <span class="nav-title">传感器数据</span>
                <span class="nav-desc">查看实时传感数据</span>
              </div>
              <div class="nav-arrow">
                <i class="el-icon-arrow-right"></i>
              </div>
            </div>
            <div class="nav-item" @click="goToPage('/system/IotAlarmRule')">
              <div class="nav-icon nav-rule">
                <i class="el-icon-setting"></i>
              </div>
              <div class="nav-info">
                <span class="nav-title">报警规则</span>
                <span class="nav-desc">配置告警阈值规则</span>
              </div>
              <div class="nav-arrow">
                <i class="el-icon-arrow-right"></i>
              </div>
            </div>
            <div class="nav-item" @click="goToPage('/system/IotAlarmRecord')">
              <div class="nav-icon nav-alarm">
                <i class="el-icon-bell"></i>
              </div>
              <div class="nav-info">
                <span class="nav-title">报警记录</span>
                <span class="nav-desc">查看历史告警记录</span>
              </div>
              <div class="nav-arrow">
                <i class="el-icon-arrow-right"></i>
              </div>
            </div>
            <div class="nav-item" @click="goToPage('/system/IotControlLog')">
              <div class="nav-icon nav-control">
                <i class="el-icon-video-play"></i>
              </div>
              <div class="nav-info">
                <span class="nav-title">控制日志</span>
                <span class="nav-desc">查看设备控制记录</span>
              </div>
              <div class="nav-arrow">
                <i class="el-icon-arrow-right"></i>
              </div>
            </div>
            <div class="nav-item" @click="goToPage('/system/IotCameraCapture')">
              <div class="nav-icon nav-camera">
                <i class="el-icon-camera"></i>
              </div>
              <div class="nav-info">
                <span class="nav-title">抓拍记录</span>
                <span class="nav-desc">查看摄像头抓拍</span>
              </div>
              <div class="nav-arrow">
                <i class="el-icon-arrow-right"></i>
              </div>
            </div>
          </div>
        </div>

        <div class="section-card activity-card">
          <div class="card-header">
            <h3 class="card-title">最近动态</h3>
            <span class="card-subtitle">实时更新</span>
          </div>
          <div class="activity-list">
            <div class="activity-item" v-for="(item, index) in recentActivities" :key="index">
              <div class="activity-dot" :class="item.type"></div>
              <div class="activity-content">
                <span class="activity-text">{{ item.text }}</span>
                <span class="activity-time">{{ item.time }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="section-row">
        <div class="section-card trend-card">
          <div class="card-header">
            <h3 class="card-title">传感器数据趋势</h3>
          </div>
          <div ref="trendChart" class="trend-chart"></div>
        </div>

        <div class="section-card weather-card">
          <div class="card-header">
            <h3 class="card-title">环境监测</h3>
          </div>
          <div class="weather-content">
            <div class="weather-item">
              <div class="weather-icon temp">
                <i class="el-icon-temperature"></i>
              </div>
              <div class="weather-info">
                <span class="weather-value">{{ avgTemp }}°C</span>
                <span class="weather-label">平均温度</span>
              </div>
            </div>
            <div class="weather-item">
              <div class="weather-icon humidity">
                <i class="el-icon-water"></i>
              </div>
              <div class="weather-info">
                <span class="weather-value">{{ avgHumidity }}%</span>
                <span class="weather-label">平均湿度</span>
              </div>
            </div>
            <div class="weather-item">
              <div class="weather-icon light">
                <i class="el-icon-sunny"></i>
              </div>
              <div class="weather-info">
                <span class="weather-value">{{ avgIllum }} lux</span>
                <span class="weather-label">平均光照</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'

export default {
  name: "Index",
  data() {
    return {
      poleCount: 128,
      deviceCount: 342,
      sensorCount: 12580,
      alarmCount: 12,
      onlinePoles: 125,
      normalDevices: 325,
      handledAlarms: 10,
      avgTemp: 26.8,
      avgHumidity: 65.2,
      avgIllum: 1250,
      statusChart: null,
      trendChart: null,
      recentActivities: [
        { type: 'success', text: '灯杆 #P001 状态恢复正常', time: '2分钟前' },
        { type: 'warning', text: '传感器 #S023 温度异常告警', time: '5分钟前' },
        { type: 'info', text: 'LED灯 #L045 已开启', time: '8分钟前' },
        { type: 'danger', text: '设备 #D012 离线告警', time: '12分钟前' },
        { type: 'success', text: '控制指令执行成功', time: '15分钟前' },
        { type: 'info', text: '抓拍记录更新', time: '20分钟前' },
      ]
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initCharts()
    })
  },
  beforeDestroy() {
    if (this.statusChart) this.statusChart.dispose()
    if (this.trendChart) this.trendChart.dispose()
  },
  methods: {
    initCharts() {
      this.statusChart = echarts.init(this.$refs.statusChart)
      this.statusChart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0, left: 'center' },
        series: [{
          type: 'pie',
          radius: ['45%', '75%'],
          center: ['50%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          data: [
            { value: 98, name: '正常', itemStyle: { color: '#13ce66' } },
            { value: 4, name: '离线', itemStyle: { color: '#ff9800' } },
            { value: 2, name: '故障', itemStyle: { color: '#ff4949' } }
          ]
        }]
      })

      this.trendChart = echarts.init(this.$refs.trendChart)
      this.trendChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['温度', '湿度'], bottom: 10 },
        grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
        xAxis: { type: 'category', data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00'], boundaryGap: false },
        yAxis: [{ type: 'value', name: '温度(°C)' }, { type: 'value', name: '湿度(%)' }],
        series: [
          {
            name: '温度', type: 'line', smooth: true,
            data: [22, 21, 24, 28, 27, 25],
            lineStyle: { color: '#f56c6c', width: 2 },
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(245, 108, 108, 0.3)' }, { offset: 1, color: 'rgba(245, 108, 108, 0.05)' }]) }
          },
          {
            name: '湿度', type: 'line', smooth: true, yAxisIndex: 1,
            data: [70, 72, 68, 62, 65, 68],
            lineStyle: { color: '#409EFF', width: 2 },
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(64, 158, 255, 0.3)' }, { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }]) }
          }
        ]
      })
    },
    goToPage(path) {
      this.$router.push(path)
    }
  }
}
</script>

<style scoped lang="scss">
.dashboard-home {
  padding: 0;
  min-height: 100vh;
  background: #f5f7fa;
}

.hero-section {
  position: relative;
  overflow: hidden;
  padding: 60px 40px;
}

.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  animation: gradientShift 15s ease infinite;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-text {
  text-align: center;
  margin-bottom: 40px;
}

.hero-title {
  font-size: 42px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 16px 0;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.hero-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
  max-width: 1000px;
  margin: 0 auto;
}

.hero-stat {
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  padding: 24px;
  border-radius: 16px;
  transition: all 0.3s ease;
}

.hero-stat:hover {
  transform: translateY(-4px);
  background: rgba(255, 255, 255, 0.2);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.lamp-icon { background: linear-gradient(135deg, #52c41a 0%, #95de64 100%); color: #fff; }
.device-icon { background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%); color: #fff; }
.sensor-icon { background: linear-gradient(135deg, #722ed1 0%, #9254de 100%); color: #fff; }
.alarm-icon { background: linear-gradient(135deg, #fa8c16 0%, #ffad33 100%); color: #fff; }

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
}

.stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 4px;
}

.main-content {
  padding: 30px;
}

.section-row {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 24px;
  margin-bottom: 24px;
}

.section-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.card-subtitle {
  font-size: 13px;
  color: #909399;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.overview-item {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 12px;
}

.item-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.item-label {
  font-size: 14px;
  color: #606266;
}

.item-percent {
  font-size: 14px;
  font-weight: 600;
  color: #13ce66;
}

.progress-bar {
  height: 8px;
  background: #e9ecef;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 10px;
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 1s ease;
}

.bg-green { background: linear-gradient(90deg, #13ce66, #52c41a); }
.bg-blue { background: linear-gradient(90deg, #409EFF, #667eea); }
.bg-purple { background: linear-gradient(90deg, #722ed1, #9254de); }
.bg-orange { background: linear-gradient(90deg, #fa8c16, #ffad33); }

.item-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.mini-chart {
  height: 180px;
}

.nav-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 16px;
  background: #f8f9fa;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.nav-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  background: #fff;
}

.nav-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: 12px;
}

.nav-lamp { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; }
.nav-device { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: #fff; }
.nav-sensor { background: linear-gradient(135deg, #56ab2f 0%, #a8e063 100%); color: #fff; }
.nav-rule { background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%); color: #fff; }
.nav-alarm { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); color: #fff; }
.nav-control { background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%); color: #fff; }
.nav-camera { background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%); color: #fff; }

.nav-info {
  text-align: center;
}

.nav-title {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.nav-desc {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.nav-arrow {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 16px;
  color: #c0c4cc;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.nav-item:hover .nav-arrow {
  opacity: 1;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f2f6fc;
}

.activity-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.activity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}

.activity-dot.success { background: #13ce66; }
.activity-dot.warning { background: #faad14; }
.activity-dot.danger { background: #ff4949; }
.activity-dot.info { background: #409EFF; }

.activity-content {
  flex: 1;
}

.activity-text {
  display: block;
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
}

.activity-time {
  font-size: 12px;
  color: #c0c4cc;
}

.trend-chart {
  height: 220px;
}

.weather-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.weather-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 12px;
}

.weather-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.weather-icon.temp { background: linear-gradient(135deg, #f56c6c 0%, #ff8989 100%); color: #fff; }
.weather-icon.humidity { background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%); color: #fff; }
.weather-icon.light { background: linear-gradient(135deg, #e6a23c 0%, #f0c78a 100%); color: #fff; }

.weather-info {
  display: flex;
  flex-direction: column;
}

.weather-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.weather-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

@media (max-width: 992px) {
  .section-row {
    grid-template-columns: 1fr;
  }
  
  .nav-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .overview-grid {
    grid-template-columns: 1fr;
  }
  
  .hero-title {
    font-size: 28px;
  }
}
</style>
