# 基于 ESP32 和 AtomS3R M12 的智能灯杆系统



---

## 系统架构

```
┌────────────────── 硬件层 (WiFi 局域网) ──────────────────────┐
│                                                               │
│  AtomS3R M12 摄像头        ESP32-S3-DevKitC-1 (单杆控制器)    │
│  ├─ MJPEG 视频流 (/stream)  ├─ DHT11  温湿度  (GPIO4)        │
│  ├─ POST /capture (拍照)    ├─ 光敏模块 光照  (GPIO1, ADC)   │
│  └─ POST 流帧到 Flask       ├─ INA219 电压电流 (I2C)         │
│                              ├─ 2路继电器 IN2→GPIO7 (LED 控) │
│                              └─ 绿色 LED + 100Ω 限流电阻      │
└─────────────┬─────────────────────┬──────────────────────────┘
              │ HTTP                 │ MQTT (lamp/LP001)
              ▼                      ▼
┌────────────────── 服务层 ─────────────────────────────────────┐
│                                                               │
│  EMQX :1883 (MQTT 消息代理)                                   │
│      ↕                                                        │
│  RuoYi-Vue (Spring Boot :8080 + Vue :80)                      │
│  ├─ MQTT 消息处理 → 传感器数据 / 报警 / LED 状态             │
│  ├─ REST API → 历史数据、曲线、报警规则配置                   │
│  └─ Element-UI 管理后台                                       │
│                                                               │
│  Flask (:5000) — 摄像头流代理 + 照片管理                      │
│  ├─ 转发 AtomS3R 的 MJPEG 视频流                              │
│  ├─ 照片存档 + 图库                                           │
│  ├─ SSE 事件 → 实时拍照通知                                   │
│  └─ AI 检测触发 → trigger → ESP32 → 拍照                      │
│                                                               │
│  YOLOv11 (:5001) — 行人检测 HTTP 服务                         │
│  MySQL (:3306) — 7 张业务表 (RuoYi 代码生成)                  │
└───────────────────────────────────────────────────────────────┘
```

## 功能清单

| 功能 | 说明 | 分值 |
|---|---|---|
| **灯杆列表与详情页** | 显示全部灯杆，点击查看 LED 状态、传感数据、视频 | 10 |
| **实时传感数据监测** | 温度、湿度、光照、电压、电流 — 每 5 秒刷新 | 10 |
| **历史传感数据查询** | 按灯杆编号 + 时间范围筛选，曲线图可视化 | 20 |
| **实时视频监控** | 每根灯杆的 MJPEG 视频流 | 10 |
| **报警规则配置与弹窗** | 自定义上下限，超限弹窗提示，历史报警记录 | 10 |
| **远程 LED 控制** | Web 下发开/关指令，硬件实时响应 | 10 |
| **视频帧截图与展示** | 截取摄像头帧，前端展示原始图片 | 10 |
| **AI 行人识别** | HTTP 调用 YOLO，返回带框标注图 + 人数 | 10 |
| **识别记录检索** | 按灯杆编号 + 时间筛选，原图 + 标注图显示 | 10 |

## 硬件接线 (ESP32-S3-DevKitC-1)

### 引脚分配

| 设备 | 信号 | ESP32 引脚 | 板子位置 |
|---|---|---|---|
| DHT11 | DATA | GPIO4 | J1-4 |
| 光敏模块 | AO（模拟输出） | GPIO1 | J3-4 |
| INA219 | SDA | GPIO8 | J1-12 |
| INA219 | SCL | GPIO9 | J1-15 |
| 2路继电器 | IN2 (CH2) | GPIO7 | J1-7 |
| 绿色 LED | 阳极 | ← 100Ω ← INA219 VIN- | — |
| 绿色 LED | 阴极 | GND | J1-22 |
| AtomS3R M12 | WiFi | 独立板 | 192.168.223.223 |

### 供电分配

| 电源 | 位置 | 供电器件 |
|---|---|---|
| 3V3 | J1-1, J1-2 | DHT11 VCC、光敏 VCC、INA219 VCC |
| 5V | J1-21 | 继电器 DC+、继电器 COM2 |
| GND | J1-22 | 所有模块 GND、LED 阴极 |

### INA219 接线（带 100Ω 下拉防漏电）

```
继电器 NO2 ─┬─ INA219 VIN+ (总线电压测量)
             └─ 100Ω → GND (下拉，防止 LED 微亮)
INA219 VIN- → 100Ω → 绿色 LED 阳极
绿色 LED 阴极 → GND
```

### LED 电流回路

```
继电器吸合: 5V → COM2 → NO2 → INA219 VIN+ → [0.1Ω 分流] → VIN- → 100Ω → LED → GND
继电器断开: VIN+ 被 100Ω 拉到 GND → LED 彻底关断（无漏电）
```

### 注意事项

- **ADC1（GPIO1-10）** 用于模拟输入 — ADC2 与 WiFi 冲突不可用
- **GPIO35/36/37** 被 Octal PSRAM 占用 — 不可使用
- **GPIO19/20** 是 USB D-/D+ — 不可使用
- 光敏 AO 为 **反相输出**：越暗电压越高（raw=4095），越亮电压越低
- 2路继电器为 **高电平触发**：HIGH→吸合、LOW→断开

## 固件

### ESP32-S3-WROOM (`arduino_stream/esp32_s3_wroom/`)

| 周期 | 动作 |
|---|---|
| 每 5 秒 | 读 DHT11（温度/湿度） |
| 每 5 秒 | 读光敏电阻（环境光照） |
| 每 5 秒 | 读 INA219（LED 电压/电流） |
| 每 5 秒 | 自动开关灯（暗→亮、亮→灭，带滞回区间） |
| 每 5 秒 | MQTT 上报到 `lamp/LP001` |
| 每 500ms | 轮询 Flask 拍照触发 → POST AtomS3R /capture |
| MQTT 命令 | 订阅 `lamp/LP001/control` 接收远程开关指令 |

**依赖库：** DHTesp、Adafruit_INA219、PubSubClient

### AtomS3R M12 (`arduino_stream/atoms3r_camera/`)

| 路由 | 方法 | 功能 |
|---|---|---|
| / | GET | 网页界面（视频流 + 拍照按钮） |
| /stream | GET | MJPEG 视频流（720P） |
| /capture | POST | 拍照，自动上传 Flask `/api/upload/photo` |

持续推送视频帧到 Flask `/api/upload/stream`，供 AI 检测线程使用。

## MQTT 协议

### 设备上报（ESP32 → 服务器）

**Topic：** `lamp/LP001`

```json
{
  "lampId": "LP001",
  "temperature": 26.5,
  "humidity": 75.0,
  "illumination": 40.95,
  "voltage": 5.02,
  "current": 0.018,
  "status": "1"
}
```

| 字段 | 来源 | 说明 |
|---|---|---|
| lampId | 固定值 | 必须与 `iot_pole.pole_code` 一致 |
| temperature | DHT11 | 温度 ℃ |
| humidity | DHT11 | 湿度 %RH |
| illumination | 光敏 ADC/100 | 自定义单位（0-40.95） |
| voltage | INA219 | LED 电压 V |
| current | INA219 | LED 电流 A |
| status | LED 状态 | "0"=关, "1"=开 |

### 平台指令（服务器 → ESP32）

**Topic：** `lamp/LP001/control`

| 消息体 | 效果 |
|---|---|
| `"1"` 或 `{"cmd":"on"}` | 开灯（继电器吸合） |
| `"0"` 或 `{"cmd":"off"}` | 关灯（继电器断开） |

## REST API

### RuoYi 后端 (:8080)

需 `Authorization: Bearer <token>`（登录 admin/admin123）。

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 传感器数据 | GET | `/IotSensorData/IotSensorData/list` | 按 poleId、时间范围查询 |
| 传感器数据 | GET | `/IotSensorData/IotSensorData/{dataId}` | 单条详情 |
| 灯杆 | GET | `/IotPole/IotPole/list` | 全部灯杆 |
| 灯杆 | GET | `/IotPole/IotPole/{poleId}` | 单杆详情 |
| 摄像头抓拍 | GET | `/IotCameraCapture/IotCameraCapture/list` | 抓拍记录列表 |
| 摄像头抓拍 | POST | `/IotCameraCapture/IotCameraCapture` | 新增抓拍记录 |
| 报警规则 | GET | `/IotAlarmRule/IotAlarmRule/list` | 规则列表 |
| 报警规则 | POST | `/IotAlarmRule/IotAlarmRule` | 新建规则 |
| 报警记录 | GET | `/IotAlarmRecord/IotAlarmRecord/list` | 报警历史 |
| 控制日志 | GET | `/IotControlLog/IotControlLog/list` | 远程控制记录 |
| 设备 | GET | `/IotDevice/IotDevice/list` | 设备清单 |

### Flask 服务 (:5000)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/` | 摄像头网页界面 |
| GET | `/api/stream/mjpeg` | 实时 MJPEG 视频流 |
| GET | `/api/stream/latest` | 最新一帧 JPEG |
| GET | `/api/events` | SSE 事件推送（拍照、报警） |
| POST | `/api/upload/photo` | 接收 AtomS3R 照片 |
| GET | `/api/photos` | 图库列表（按时间倒序） |
| GET | `/api/photo/<filename>` | 单张照片 |
| POST | `/api/trigger/set` | 前端请求拍照 |
| GET | `/api/trigger/consume` | ESP32 轮询拍照指令 |
| POST | `/infer` | AI 识别代理 → YOLO:5001 |

### YOLO 服务 (:5001)

```bash
POST http://127.0.0.1:5001/infer
Content-Type: application/json

{ "image": "data:image/jpeg;base64,..." }
```

响应：
```json
{
  "image": "(base64 编码标注图)",
  "inference_results": [
    { "class": 0, "confidence": 0.95, "bbox": [100, 200, 300, 400] }
  ]
}
```

## 数据库

**库名：** `iot-light`（MySQL 8.0）

### 7 张业务表

| 表名 | 用途 | 关键字段 |
|---|---|---|
| `iot_pole` | 灯杆注册 | `pole_id, pole_code, led_status, status` |
| `iot_device` | 设备清单 | `device_id, pole_id, device_type(0~4)` |
| `iot_sensor_data` | 传感器数据 | `data_id, pole_id, temperature, humidity, illumination, voltage, current, collect_time` |
| `iot_alarm_rule` | 报警规则 | `rule_id, pole_id, param_type(0~4), min_value, max_value` |
| `iot_alarm_record` | 报警记录 | `record_id, pole_id, param_type, alarm_value, status` |
| `iot_control_log` | 控制日志 | `log_id, pole_id, control_type(0~2), result` |
| `iot_camera_capture` | 摄像头抓拍 | `capture_id, pole_id, image_url, person_count, result_json, capture_time` |

### 种子数据

```sql
INSERT INTO `iot_pole` VALUES (
  100, 'LP001', '灯杆01', '测试路段',
  NULL, NULL, '0', '0', 1, '0',
  'admin', NOW(), '', NULL, NULL
);
```

## AI 行人检测流程

```
1. Flask 检测线程（每 2 秒）截取最新视频帧
2. POST 帧到 YOLO :5001 推理
3. 检测到行人（class=0, confidence>0.5）：
   a. 缓存 AI 结果（人数 + bbox JSON）
   b. 设置 trigger_flag = True
4. ESP32 每 500ms 轮询 /api/trigger/consume
5. 收到 True → POST AtomS3R /capture
6. AtomS3R 拍照 → POST 到 Flask /api/upload/photo
7. Flask 保存照片，读取缓存 AI 结果
8. 写入 iot_camera_capture 表（pymysql）
9. SSE 广播 "photo" → 前端自动刷新图库
```

## LED 自动控制逻辑

```cpp
if (lightRaw > LIGHT_ON_TH)           // 黑暗 (raw > 3000): 开灯
    led = true;
else if (lightRaw < LIGHT_OFF_TH)     // 明亮 (raw < 1000): 关灯
    led = false;
// 中间区间: 保持当前状态（滞回防抖）
```

## 部署

### 启动顺序

```
1. MySQL          — 导入 sql/data.sql 创建 iot-light 库
2. EMQX           — D:\bin\emqx.cmd start（MQTT 代理 :1883）
3. Spring Boot    — java -jar iotlight-admin.jar（RuoYi :8080）
4. Vue 前端       — cd start-vue && npm run dev（管理界面 :80）
5. YOLO           — D:\yolo_model\run.bat（AI 服务 :5001）
6. Flask          — python app.py（摄像头服务 :5000）
7. AtomS3R M12    — 上电（自动注册到 Flask）
8. ESP32-S3       — 上电（自动连 WiFi + MQTT）
```

### 远程访问

| 访问内容 | URL |
|---|---|
| **管理后台** | `http://<服务器IP>`（端口 80） |
| **摄像头页面** | `http://<服务器IP>:5000` |
| **REST API** | `http://<服务器IP>:8080` |

### 双服务器拓扑

```
服务器 1 (192.168.223.134): MySQL, EMQX, Spring Boot, Vue
服务器 2 (192.168.223.135): Flask, YOLO
```

配置文件中有注释块可切换单机/双机模式。

## 技术栈

| 组件 | 技术 |
|---|---|
| MCU | ESP32-S3 (Xtensa LX7 双核) |
| 摄像头 | AtomS3R M12 (ESP32-S3 + OV2640) |
| 传感器 | DHT11、光敏电阻(LM393)、INA219 |
| MQTT 代理 | EMQX 5.x |
| 后端 | Spring Boot 3.x + RuoYi-Vue 3.9 |
| 前端 | Vue 2 + Element-UI + ECharts |
| AI | YOLOv11（HTTP 服务） |
| 数据库 | MySQL 8.0 |
| 中间件 | Flask (Python 3)、Redis |

## 项目结构

```
Smart Light Pole System/
├── arduino_stream/
│   ├── atoms3r_camera/            # AtomS3R 摄像头固件
│   ├── esp32_s3_wroom/            # ESP32-S3 单杆控制器固件
│   └── server/
│       ├── app.py                 # Flask 服务器
│       └── templates/index.html   # 摄像头网页
├── iot-light-main/                # RuoYi-Vue 后台
│   ├── iotlight-admin/            # Spring Boot 入口
│   ├── IoT-Light-Base/            # 代码生成模块（7 张表）
│   ├── start-vue/                 # Vue 前端
│   └── sql/data.sql               # 建库脚本 + 种子数据
├── 智能灯杆硬件接线.md             # 硬件接线说明
├── 智能灯杆系统集成文档.md          # 集成部署文档
└── README.md                      # 本文件
```

