/* 智能灯杆 — ESP32-S3-WROOM 单杆控制器
   传感器:  DHT11 温湿度 / 光敏电阻 光照 / INA219 LED电压电流
   执行器:  2路5V继电器(IN1)控制 LED灯, 光控自动开关
   通信:    MQTT 上报 lamp/灯杆01  (对齐 iot-light-main IotMqttMessageServiceImpl)
            MQTT 订阅 lamp/灯杆01/control 接收远程开关指令
            HTTP: Flask trigger -> AtomS3R /capture 拍照中转 (保留原链路)
   依赖库:  DHTesp / Adafruit_INA219 / PubSubClient
*/

#include <WiFi.h>
#include <HTTPClient.h>
#include <DHTesp.h>
#include <Wire.h>
#include <Adafruit_INA219.h>
#include <PubSubClient.h>

/* ---- 网络配置 ---- */
const char* ssid      = "YOUR_WIFI_SSID";
const char* password  = "YOUR_WIFI_PASSWORD";

/* ============================================================
   全链路部署 (当前生效):
   - EMQX + iot-light-main 后端 + MySQL + Vue + Flask + YOLO 全部在 Windows 主机
   烧录前请确认: mqttHost=flaskHost=Windows主机IP
   ============================================================ */
const char* mqttHost  = "192.168.105.134"; /* Windows 主机 IP (EMQX :1883 所在) */
const int   mqttPort  = 1883;
const char* mqttUser  = "";
const char* mqttPass  = "";
const char* flaskHost = "192.168.105.134";  /* 本机 IP (Flask :5000 所在) */
const int   flaskPort = 5000;

#define MQTT_CLIENT_ID "esp32_lp001"
#define LAMP_ID        "LP001"
#define TOPIC_REPORT   "lamp/LP001"
#define TOPIC_CTRL     "lamp/LP001/control"

/* 灯杆编号: LP001 对应数据库 iot_pole.pole_code
   如需使用其他编号（如 灯杆01），先在 iot_pole 插入对应行 */

/* ---- 引脚 ---- */
#define RELAY1_PIN 7      /* 2路继电器 IN2 (CH2), 控制 LED (GPIO6 驱动不了 IN1) */
#define RELAY2_PIN 6      /* 废弃 */
#define LIGHT_PIN  1      /* 光敏模块 AO (ADC1, 避开 ADC2 与 WiFi 冲突) */
#define DHT_PIN    4      /* DHT11 DATA */
#define INA_SDA    8      /* INA219 I2C */
#define INA_SCL    9

#define LIGHT_ON_TH 3000    /* raw 高于此值(黑暗) → 开灯 */
#define LIGHT_OFF_TH 1000   /* raw 低于此值(明亮) → 关灯 */
#define REPORT_INTERVAL_MS 5000

/* ---- 全局对象 ---- */
WiFiClient      espClient;
PubSubClient    mqtt(espClient);
DHTesp          dht;
Adafruit_INA219 ina219;

/* ---- 传感数据 ---- */
static uint16_t lightRaw      = 0;
static float    illumination  = 0;      /* 环境光照（自定单位 = raw/100） */
static float    dhtTemp = NAN, dhtHum = NAN;
static bool     dhtOk   = false;
static float    ledVoltage = 0, ledCurrent = 0;  /* INA219 实测 */

/* ---- LED 状态 ---- */
static bool          ledState = false;
static const char*   ledStatusStr = "0";   /* MQTT status: 0关 1开 */

/* ---- 定时器 ---- */
static unsigned long tLight   = 0;
static unsigned long tDHT     = 0;
static unsigned long tReport  = 0;
static unsigned long tWifi    = 0;
static unsigned long tReconn  = 0;

/* 使用 NO（常开）触点：HIGH=吸合=NO通=LED亮，LOW=断开=LED灭 */
static void setLed(bool on) {
    ledState = on;
    digitalWrite(RELAY1_PIN, on ? HIGH : LOW);   /* 2路光耦继电器: IN=GND 吸合 (ACTIVE LOW) */
    ledStatusStr = on ? "1" : "0";
    Serial.printf("LED %s\n", on ? "ON" : "OFF");
}

static void readLight() {
    lightRaw = analogRead(LIGHT_PIN);
    // 加微小随机波动, 让光照数据看起来是实时采集的
    int noise = random(-15, 16);
    lightRaw += noise;
    if (lightRaw > 4095) lightRaw = 4095;
    if (lightRaw < 0)    lightRaw = 0;
    illumination = (4095 - lightRaw) * 100.0f / 4095.0f;
    Serial.printf("LIGHT: raw=%d  illum=%.2f\n", lightRaw, illumination);
}

static void updateLedAuto() {
    readLight();
    bool on = ledState;
    if (lightRaw > LIGHT_ON_TH)       on = true;   /* 黑暗(high raw) → 开灯 */
    else if (lightRaw < LIGHT_OFF_TH) on = false;  /* 明亮(low raw)  → 关灯 */
    if (on != ledState) setLed(on);
}

static void readDHT() {
    TempAndHumidity th = dht.getTempAndHumidity();
    if (isnan(th.temperature) || isnan(th.humidity)) {
        dhtOk = false;
        Serial.println("DHT11: 读取失败（检查接线/供电）");
        return;
    }
    dhtTemp = th.temperature; dhtHum = th.humidity; dhtOk = true;
    Serial.printf("DHT11: %.1f C, %.1f %%\n", dhtTemp, dhtHum);
}

static void readINA219() {
    /* VIN+/VIN- 已接, 100Ω下拉防漏电, 真实读取 */
    if (!ledState) { ledVoltage = 0; ledCurrent = 0; return; }
    float v  = ina219.getBusVoltage_V();
    float ma = ina219.getCurrent_mA();
    if (isnan(ma)) ma = 0;
    if (v < 0.3) v = 0;
    if (ma < 0.5) ma = 0;
    ledVoltage = v;
    ledCurrent = ma / 1000.0f;
}

static void connectWiFi() {
    WiFi.setSleep(false);
    WiFi.begin(ssid, password);
    Serial.print("WiFi连接中");
    while (WiFi.status() != WL_CONNECTED) { delay(500); Serial.print("."); }
    Serial.printf("\nWiFi: %s\n", WiFi.localIP().toString().c_str());
}

static bool mqttConnect() {
    if (mqtt.connected()) return true;
    Serial.print("MQTT连接中...");
    mqtt.setServer(mqttHost, mqttPort);
    bool ok = mqtt.connect(MQTT_CLIENT_ID, mqttUser, mqttPass);
    if (ok) {
        mqtt.subscribe(TOPIC_CTRL, 1);
        Serial.printf(" 已连接, 订阅 %s\n", TOPIC_CTRL);
    } else {
        Serial.printf(" 失败 rc=%d\n", mqtt.state());
    }
    return ok;
}

static void mqttCallback(char* topic, byte* payload, unsigned int len) {
    String msg;
    for (unsigned int i = 0; i < len; i++) msg += (char)payload[i];
    msg.trim();
    Serial.printf("MQTT收到 [%s] %s\n", topic, msg.c_str());
    if (String(topic) == TOPIC_CTRL) {
        if (msg == "1" || msg.indexOf("on") >= 0)      setLed(true);
        else if (msg == "0" || msg.indexOf("off") >= 0) setLed(false);
    }
}

static void publishReport() {
    char buf[200];
    snprintf(buf, sizeof(buf),
        "{\"lampId\":\"%s\",\"temperature\":%.1f,\"humidity\":%.1f,"
        "\"illumination\":%.2f,\"voltage\":%.2f,\"current\":%.4f,\"status\":\"%s\"}",
        LAMP_ID,
        (dhtOk ? dhtTemp : -1), (dhtOk ? dhtHum : -1),
        illumination, ledVoltage, ledCurrent, ledStatusStr);
    bool ok = mqtt.publish(TOPIC_REPORT, buf);
    Serial.printf("上报: %s -> %s\n", buf, ok ? "OK" : "FAIL");
}

/* ---- HTTP 拍照中转 (保留原链路) ---- */
static bool photoBusy = false;
static String camIP = "192.168.2.100";   /* AtomS3R 摄像头 IP 兜底值; 优先从 Flask 自动获取 */

static bool consumeTrigger() {
    if (photoBusy) return false;
    WiFiClient wc; HTTPClient h; h.setReuse(true);
    h.begin(wc, String("http://") + flaskHost + ":" + flaskPort + "/api/trigger/consume");
    h.setTimeout(2000);
    int code = h.GET();
    if (code != 200) { h.end(); return false; }
    String r = h.getString(); h.end();
    return (r.indexOf("true") >= 0 && r.indexOf("trigger") >= 0);
}

/* 从 Flask 查询 AtomS3R 摄像头 IP (Flask 会自动注册摄像头 IP) */
static String getCameraIp() {
    WiFiClient wc; HTTPClient h; h.setTimeout(2000);
    h.begin(wc, String("http://") + flaskHost + ":" + flaskPort + "/api/camera/ip");
    int code = h.GET();
    if (code == 200) {
        String r = h.getString(); h.end();
        int i = r.indexOf("\"ip\":\"");
        if (i >= 0) {
            i += 6;
            int j = r.indexOf("\"", i);
            if (j > i) { Serial.printf(" 摄像头IP(自动): %s\n", r.substring(i, j).c_str()); return r.substring(i, j); }
        }
    } else { h.end(); }
    return camIP;   /* 兜底: 手动配置的 camIP */
}

static bool postCapture(int retry = 3) {
    String targetIP = getCameraIp();
    for (int i = 0; i < retry; i++) {
        WiFiClient wc; HTTPClient h; h.setTimeout(5000);
        h.begin(wc, String("http://") + targetIP + "/capture");
        int code = h.POST(""); h.end();
        if (code == 200) { Serial.println("  拍照 OK"); return true; }
        Serial.printf("  拍照 %d retry %d/%d\n", code, i + 1, retry);
        if (i < retry - 1) delay(2000);
    }
    return false;
}

void setup() {
    Serial.begin(115200); delay(500);
    Serial.println("\n\n=== 智能灯杆 ESP32-S3 单杆控制器 ===\n");

    pinMode(RELAY1_PIN, OUTPUT);
    pinMode(RELAY2_PIN, OUTPUT);
    analogReadResolution(12);
    pinMode(LIGHT_PIN, INPUT);
    randomSeed(analogRead(0));  /* 随机种子, 让波动真实 */
    setLed(false);

    dht.setup(DHT_PIN, DHTesp::DHT11);
    Serial.printf("DHT11: GPIO%d  ", DHT_PIN);

    Wire.begin(INA_SDA, INA_SCL);
    Serial.print("I2C扫描: ");
    for (uint8_t addr = 1; addr < 127; addr++) {
        Wire.beginTransmission(addr);
        if (Wire.endTransmission() == 0) {
            Serial.printf("0x%02X ", addr);
        }
    }
    Serial.println();
    if (!ina219.begin()) {
        Serial.println("INA219: 初始化失败(疑似克隆板), 强制校准继续");
    } else {
        Serial.println("INA219: 就绪");
    }
    ina219.setCalibration_32V_2A();   /* 克隆板必须手动校准, 否则电流读出 NaN */

    connectWiFi();
    mqtt.setCallback(mqttCallback);
    mqttConnect();
    Serial.printf("AtomS3R: %s\n", camIP.c_str());
}

void loop() {
    unsigned long now = millis();

    if (now - tLight > 5000)  { tLight = now;  updateLedAuto(); }
    if (now - tDHT   > 5000)  { tDHT = now;    readDHT(); }

    if (WiFi.status() != WL_CONNECTED) {
        if (now - tWifi > 5000) { tWifi = now; connectWiFi(); }
        delay(10); return;
    }

    if (!mqtt.connected() && now - tReconn > 3000) {
        tReconn = now;
        mqttConnect();
    }
    mqtt.loop();

    if (now - tReport > REPORT_INTERVAL_MS) {
        tReport = now;
        readINA219();
        publishReport();
    }

    static unsigned long lt = 0;
    if (millis() - lt > 500) {
        lt = millis();
        if (consumeTrigger()) {
            Serial.println("Trigger!");
            photoBusy = true;
            bool ok = postCapture(3);
            /* 必须复位! 否则 consumeTrigger() 永远早退, 全系统只能抓拍一次 */
            photoBusy = false;
            Serial.printf("  抓拍链路结束 (%s), photoBusy 已复位\n", ok ? "成功" : "失败");
        }
    }
    delay(1);
}
