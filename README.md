# Smart Light Pole System Based on ESP32 and AtomS3R M12

> **2025 IoT Application Innovation Design Competition — Skill Challenge**
> A multi-sensor smart light pole system with real-time environmental monitoring, AI-powered pedestrian detection, and remote LED control.

---

## System Architecture

```
┌────────────────── Hardware Layer (WiFi LAN) ──────────────────┐
│                                                                │
│  AtomS3R M12 Camera       ESP32-S3-DevKitC-1 (Pole Controller) │
│  ├─ MJPEG Stream (/stream) ├─ DHT11  Temp/Humidity (GPIO4)     │
│  ├─ POST /capture (photo)  ├─ Photoresistor Lux (GPIO1, ADC)   │
│  └─ POST stream frames     ├─ INA219 Voltage/Current (I2C)     │
│                             ├─ 2-Ch Relay IN → GPIO7 (LED Ctrl) │
│                             └─ Green LED + 100Ω current limit   │
└─────────────┬────────────────────┬─────────────────────────────┘
              │ HTTP                │ MQTT (lamp/LP001)
              ▼                     ▼
┌────────────────── Server Layer ────────────────────────────────┐
│                                                                │
│  EMQX :1883 (MQTT Broker)                                      │
│      ↕                                                         │
│  RuoYi-Vue (Spring Boot :8080 + Vue :80)                      │
│  ├─ MQTT Handler → iot_sensor_data / alarm / LED status       │
│  ├─ REST API for historical data, curves, alarm rules          │
│  └─ Element-UI frontend for management dashboard               │
│                                                                │
│  Flask (:5000) — Camera stream proxy + photo management        │
│  ├─ MJPEG stream relay from AtomS3R                            │
│  ├─ Photo archive + gallery                                    │
│  ├─ SSE events for real-time photo notifications               │
│  └─ AI detection trigger → MQTT → ESP32 → camera capture       │
│                                                                │
│  YOLOv11 (:5001) — Pedestrian detection HTTP service           │
│  MySQL (:3306) — 7 business tables (RuoYi codegen)             │
└────────────────────────────────────────────────────────────────┘
```

## Features

| Feature | Description | Points |
|---|---|---|
| **Pole List & Detail View** | List all poles, click for LED status, sensor data, video | 10 |
| **Real-time Sensor Monitoring** | Temp, humidity, light, voltage, current — refresh every 5s | 10 |
| **Historical Sensor Data** | Filter by pole ID + time range, curve chart visualization | 20 |
| **Live Camera Stream** | MJPEG stream from each pole's camera | 10 |
| **Alarm Rules & Real-time Alerts** | Custom min/max thresholds, popup on violation, history log | 10 |
| **Remote LED Control** | Web/APP send ON/OFF command, hardware responds in real time | 10 |
| **Video Frame Capture** | Capture frame from camera, display original image | 10 |
| **AI Pedestrian Detection** | HTTP call to YOLO, return annotated image + person count | 10 |
| **Detection History** | Filter by pole ID + time, show original + annotated images | 10 |

## Hardware Wiring (ESP32-S3-DevKitC-1)

### Pin Assignment

| Device | Signal | ESP32 Pin | Board Header |
|---|---|---|---|
| DHT11 | DATA | GPIO4 | J1-4 |
| Photoresistor Module | AO (Analog Out) | GPIO1 | J3-4 |
| INA219 | SDA | GPIO8 | J1-12 |
| INA219 | SCL | GPIO9 | J1-15 |
| 2-Ch Relay | IN2 (CH2) | GPIO7 | J1-7 |
| Green LED | Anode | ← 100Ω ← INA219 VIN- | — |
| Green LED | Cathode | GND | J1-22 |
| AtomS3R M12 | WiFi | Independent board | 192.168.223.223 |

### Power Distribution

| Rail | Pin | Supplies |
|---|---|---|
| 3V3 | J1-1, J1-2 | DHT11 VCC, Photoresistor VCC, INA219 VCC |
| 5V | J1-21 | Relay DC+, Relay COM2 |
| GND | J1-22 | All modules, LED cathode |

### INA219 Wiring (with 100Ω pull-down to prevent leakage)

```
Relay NO2 ─┬─ INA219 VIN+ (bus voltage measurement)
            └─ 100Ω → GND (pull-down, prevents LED ghosting)
INA219 VIN- → 100Ω → Green LED Anode
Green LED Cathode → GND
```

### LED Current Path

```
Relay ON: 5V → COM2 → NO2 → INA219 VIN+ → [0.1Ω shunt] → VIN- → 100Ω → LED → GND
Relay OFF: VIN+ pulled to GND via 100Ω → LED completely off (no leakage)
```

### Important Notes

- **ADC1 (GPIO1-10)** must be used for analog — ADC2 conflicts with WiFi
- **GPIO35/36/37** are used by Octal PSRAM — do not use
- **GPIO19/20** are USB D-/D+ — do not use
- The photoresistor module AO is **inverted**: dark → HIGH (raw 4095), bright → LOW
- The 2-channel relay module is **active HIGH**: HIGH → relay ON, LOW → relay OFF

## Firmware

### ESP32-S3-WROOM (`arduino_stream/esp32_s3_wroom/`)

Senses and controls one lamp pole:

| Interval | Action |
|---|---|
| Every 5s | Read DHT11 (temp/humidity) |
| Every 5s | Read photoresistor (ambient light) |
| Every 5s | Read INA219 (LED voltage/current) |
| Every 5s | Auto LED control (dark→ON, bright→OFF with hysteresis) |
| Every 5s | MQTT publish to `lamp/LP001` |
| Every 500ms | Poll Flask trigger → POST AtomS3R /capture |
| On MQTT command | Subscribe `lamp/LP001/control` for remote ON/OFF |

**Required Libraries:** DHTesp, Adafruit_INA219, PubSubClient, Arduino_ESP32_BLE (via board manager)

### AtomS3R M12 Camera (`arduino_stream/atoms3r_camera/`)

| Route | Method | Function |
|---|---|---|
| / | GET | Web UI with stream + capture button |
| /stream | GET | MJPEG video stream (720P) |
| /capture | POST | Take photo, POST to Flask `/api/upload/photo` |

Stream frames are also continuously POSTed to Flask `/api/upload/stream` for AI detection.

## MQTT Protocol

### Device Report (ESP32 → Server)

**Topic:** `lamp/LP001`

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

| Field | Source | Description |
|---|---|---|
| lampId | Config | Must match `iot_pole.pole_code` in DB |
| temperature | DHT11 | °C |
| humidity | DHT11 | %RH |
| illumination | Photoresistor AO/100 | Custom unit (0-40.95) |
| voltage | INA219 | LED working voltage (V) |
| current | INA219 | LED working current (A) |
| status | LED state | "0"=OFF, "1"=ON |

### Platform Command (Server → ESP32)

**Topic:** `lamp/LP001/control`

| Payload | Effect |
|---|---|
| `"1"` or `{"cmd":"on"}` | LED ON (relay engage) |
| `"0"` or `{"cmd":"off"}` | LED OFF (relay release) |

## REST API

### RuoYi Backend (:8080)

All endpoints require `Authorization: Bearer <token>` (login: admin/admin123).

| Module | Method | Path | Description |
|---|---|---|---|
| Sensor Data | GET | `/IotSensorData/IotSensorData/list` | Query with poleId, time range |
| Sensor Data | GET | `/IotSensorData/IotSensorData/{dataId}` | Single record detail |
| Pole | GET | `/IotPole/IotPole/list` | All poles |
| Pole | GET | `/IotPole/IotPole/{poleId}` | Single pole detail |
| Camera Capture | GET | `/IotCameraCapture/IotCameraCapture/list` | Query captures |
| Camera Capture | POST | `/IotCameraCapture/IotCameraCapture` | Add new capture |
| Alarm Rule | GET | `/IotAlarmRule/IotAlarmRule/list` | All alarm rules |
| Alarm Rule | POST | `/IotAlarmRule/IotAlarmRule` | Create rule |
| Alarm Record | GET | `/IotAlarmRecord/IotAlarmRecord/list` | Query alarm history |
| Control Log | GET | `/IotControlLog/IotControlLog/list` | Control command logs |
| Device | GET | `/IotDevice/IotDevice/list` | Device inventory |

### Flask Server (:5000)

| Method | Path | Description |
|---|---|---|
| GET | `/` | Camera web UI |
| GET | `/api/stream/mjpeg` | Live MJPEG stream |
| GET | `/api/stream/latest` | Latest frame JPEG |
| GET | `/api/events` | SSE feed (photo/alarm events) |
| POST | `/api/upload/photo` | Receive photo from AtomS3R |
| GET | `/api/photos` | Photo gallery (reverse chronological) |
| GET | `/api/photo/<filename>` | Single photo |
| POST | `/api/trigger/set` | Request photo capture |
| GET | `/api/trigger/consume` | ESP32 polls for capture orders |
| POST | `/infer` | AI detection proxy → YOLO:5001 |

### YOLO Service (:5001)

```bash
POST http://127.0.0.1:5001/infer
Content-Type: application/json

{ "image": "data:image/jpeg;base64,/9j/4AAQ..." }
```

Response:
```json
{
  "image": "(base64 JPEG with bounding boxes)",
  "inference_results": [
    { "class": 0, "confidence": 0.95, "bbox": [100, 200, 300, 400] }
  ]
}
```

## Database Schema

**Database:** `iot-light` (MySQL 8.0)

### 7 Business Tables

| Table | Purpose | Key Fields |
|---|---|---|
| `iot_pole` | Lamp pole registry | `pole_id, pole_code, led_status, status` |
| `iot_device` | Device inventory | `device_id, pole_id, device_type(0~4)` |
| `iot_sensor_data` | Time-series sensor data | `data_id, pole_id, temperature, humidity, illumination, voltage, current, collect_time` |
| `iot_alarm_rule` | Alarm threshold config | `rule_id, pole_id, param_type(0~4), min_value, max_value` |
| `iot_alarm_record` | Alarm event log | `record_id, pole_id, param_type, alarm_value, status` |
| `iot_control_log` | Remote control audit | `log_id, pole_id, control_type(0~2), result` |
| `iot_camera_capture` | Photo + AI result | `capture_id, pole_id, image_url, person_count, result_json, capture_time` |

### Seed Data (Required)

The ESP32 uses `lampId = "LP001"`, which must exist in `iot_pole`:

```sql
INSERT INTO `iot_pole` VALUES (
  100, 'LP001', '灯杆01', 'Test Section',
  NULL, NULL, '0', '0', 1, '0',
  'admin', NOW(), '', NULL, NULL
);
```

## AI Detection Flow (Pedestrian → Photo → Database)

```
1. Flask detection_loop (every 2s) grabs latest stream frame
2. POST frame to YOLO :5001 for inference
3. If person detected (class=0, confidence>0.5):
   a. Cache AI result (person count + bbox JSON)
   b. Set trigger_flag = True
4. ESP32 polls /api/trigger/consume every 500ms
5. Gets True → POST AtomS3R /capture
6. AtomS3R takes photo → POST to Flask /api/upload/photo
7. Flask saves photo, reads cached AI result
8. Writes to iot_camera_capture (MySQL) via pymysql
9. SSE broadcast "photo" → frontend auto-refreshes gallery
```

## LED Auto-Control Logic

```cpp
if (lightRaw > LIGHT_ON_TH)       // Dark (raw > 3000): ON
    led = true;
else if (lightRaw < LIGHT_OFF_TH) // Bright (raw < 1000): OFF
    led = false;
// Between thresholds: maintain current state (hysteresis)
```

The photoresistor AO is **inverted**: dark room → raw=4095, direct flashlight → raw≈50.

## Deployment

### Startup Order

```
1. MySQL        — import iot-light database from sql/data.sql
2. EMQX         — D:\emqx\bin\emqx.cmd start (broker at :1883)
3. Spring Boot  — java -jar iotlight-admin.jar (:8080)
4. Vue Frontend — npm run dev in start-vue/ (:80)
5. YOLO         — D:\yolo_model\run.bat (:5001)
6. Flask        — python app.py in server/ (:5000)
7. AtomS3R M12  — power on (auto-registers with Flask)
8. ESP32-S3     — power on (connects WiFi + MQTT)
```

### Remote Access

- **Vue Dashboard:** `http://<server-ip>` (port 80)
- **Camera Page:** `http://<server-ip>:5000`
- **REST API:** `http://<server-ip>:8080`

### Dual-Server Topology (Optional)

```
Server 1 (192.168.223.134): MySQL, EMQX, Spring Boot, Vue
Server 2 (192.168.223.135): Flask, YOLO
```

Switch config files between single-machine and dual-server via commented blocks.

## Technology Stack

| Component | Technology |
|---|---|
| MCU | ESP32-S3 (Xtensa LX7 dual-core) |
| Camera | AtomS3R M12 (ESP32-S3 + OV2640) |
| Sensors | DHT11, Photoresistor (LM393), INA219 |
| MQTT Broker | EMQX 5.x |
| Backend | Spring Boot 3.x + RuoYi-Vue 3.9 |
| Frontend | Vue 2 + Element-UI + ECharts |
| AI | YOLOv11 (HTTP service) |
| Database | MySQL 8.0 |
| Middleware | Flask (Python 3), Redis |

## Project Structure

```
Smart Light Pole System Based on ESP32 and Atom‑S3R/
├── arduino_stream/
│   ├── atoms3r_camera/           # AtomS3R M12 camera firmware
│   ├── esp32_s3_wroom/           # ESP32-S3 pole controller firmware
│   └── server/
│       ├── app.py                # Flask server
│       └── templates/index.html  # Camera web page
├── iot-light-main/               # RuoYi-Vue backend
│   ├── iotlight-admin/           # Spring Boot entry
│   ├── IoT-Light-Base/           # Generated modules
│   ├── start-vue/                # Vue frontend
│   └── sql/data.sql              # Database schema + seed data
├── docs/
│   ├── 智能灯杆硬件接线.md        # Hardware wiring (Chinese)
│   └── 智能灯杆系统集成文档.md     # Integration guide (Chinese)
└── README.md                     # This file
```

## Competition Context

- **Event:** 2025 9th Hunan University IoT Application Innovation Design Competition
- **Category:** Skill Challenge
- **Rules:** No internet access during competition; team-built LAN; committee-provided YOLO model via USB
- **Scoring:** Module 1 (70pts) — data collection/display/control; Module 2 (30pts) — AI video recognition

---

## License

This project is for educational/competition purposes.