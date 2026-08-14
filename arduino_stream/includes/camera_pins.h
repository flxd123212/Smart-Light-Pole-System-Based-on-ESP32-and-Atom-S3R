/*
  智能灯杆 — AtomS3R M12 直连版 (1080P@30fps)
  ============================================
  基于 M5Stack 官方相机例程 + camera_pins.h

  架构:
    1. 主循环: capture → POST /api/upload/stream (Flask, 供 MJPEG + 图库)
    2. 轮询 Flask /api/trigger/capture (500ms) → 是 → capture → POST /api/upload/photo
    3. 不需要 WebServer，ESP32 不需要参与图像传输

  引脚: camera_pins.h (M5Stack 官方固定)
  配置: FRAMESIZE_1080P, fb_count=2, GRAB_LATEST

  硬件: AtomS3R M12 (OV3660, ESP32-S3-PICO, 8MB PSRAM)
*/

#include "camera_pins.h"
#include <WiFi.h>
#include "esp_camera.h"

// ===== WiFi =====
const char* ssid      = "flxd";
const char* password  = "flxd123212";

// ===== Flask =====
const char* flaskHost = "192.168.223.134";
const int   flaskPort = 5000;

// ===== 摄像头配置 (官方: fb_count=2 + GRAB_LATEST) =====
static camera_config_t camera_config = {
    .pin_pwdn     = PWDN_GPIO_NUM,  .pin_reset    = RESET_GPIO_NUM,
    .pin_xclk     = XCLK_GPIO_NUM,  .pin_sscb_sda = SIOD_GPIO_NUM,
    .pin_sscb_scl = SIOC_GPIO_NUM,
    .pin_d7       = Y9_GPIO_NUM,    .pin_d6       = Y8_GPIO_NUM,
    .pin_d5       = Y7_GPIO_NUM,    .pin_d4       = Y6_GPIO_NUM,
    .pin_d3       = Y5_GPIO_NUM,    .pin_d2       = Y4_GPIO_NUM,
    .pin_d1       = Y3_GPIO_NUM,    .pin_d0       = Y2_GPIO_NUM,
    .pin_vsync    = VSYNC_GPIO_NUM, .pin_href     = HREF_GPIO_NUM,
    .pin_pclk     = PCLK_GPIO_NUM,
    .xclk_freq_hz = 20000000,
    .ledc_timer   = LEDC_TIMER_0,   .ledc_channel = LEDC_CHANNEL_0,
    .pixel_format = PIXFORMAT_JPEG,
    .frame_size   = FRAMESIZE_1080P,   // ★ 1920×1080
    .jpeg_quality = 10,                // ★ 画质 10 (0~63, 越小越好)
    .fb_count     = 2,                 // ★ 双缓冲连续模式
    .fb_location  = CAMERA_FB_IN_PSRAM,
    .grab_mode    = CAMERA_GRAB_LATEST, // ★ LATEST — 不阻塞，返回最新帧
    .sccb_i2c_port = 0,
};

#define POWER_PIN  18

// ===== HTTP POST 工具 =====
static bool httpPost(const char* path, const uint8_t* data, size_t len,
                     int timeoutMs = 2000) {
    if (WiFi.status() != WL_CONNECTED) return false;
    WiFiClient wc;
    HTTPClient http;
    http.setReuse(true);
    http.begin(wc, String("http://") + flaskHost + ":" + flaskPort + path);
    http.addHeader("Content-Type", "image/jpeg");
    http.setTimeout(timeoutMs);
    int code = http.POST((uint8_t*)data, len);
    http.end();
    return code == 200;
}

// ===== 帧捕获 + POST =====
// 始终返回最新帧 (GRAB_LATEST + fb_count=2 → 不阻塞)
static bool captureAndPost(const char* path, int timeoutMs = 2000) {
    camera_fb_t* fb = esp_camera_fb_get();
    if (!fb) { Serial.println("❌ capture fail"); return false; }

    Serial.printf("📷 %uB -> %s\n", fb->len, path);
    bool ok = httpPost(path, fb->buf, fb->len, timeoutMs);
    if (!ok) Serial.println("⚠ POST fail");

    esp_camera_fb_return(fb);
    return ok;
}

// ===== 轮询 Flask 触发 (AtomS3R 自己检查"是否需要拍照") =====
static bool checkCaptureTrigger() {
    WiFiClient wc;
    HTTPClient http;
    http.setReuse(true);
    http.begin(wc, String("http://") + flaskHost + ":" + flaskPort + "/api/trigger/consume");
    http.setTimeout(2000);
    int code = http.GET();
    if (code != 200) { http.end(); return false; }
    String resp = http.getString();
    http.end();
    return (resp.indexOf("true") >= 0 && resp.indexOf("trigger") >= 0);
}

// ===== 启动 =====
void setup() {
    Serial.begin(115200); delay(500);
    Serial.println("\n\n=== AtomS3R M12 1080P ===\n");

    // 摄像头供电
    pinMode(POWER_PIN, OUTPUT);
    digitalWrite(POWER_PIN, LOW);
    delay(500);

    // 初始化 (UXGA/JPEG/fb_count=2/GRAB_LATEST)
    if (esp_camera_init(&camera_config) != ESP_OK) {
        Serial.println("❌ Camera init fail");
        while (1) delay(100);
    }
    Serial.println("✅ Camera: 1080P JPEG fb_count=2 GRAB_LATEST");

    // 传感器参数
    sensor_t* s = esp_camera_sensor_get();
    s->set_gain_ctrl(s, 1);
    s->set_exposure_ctrl(s, 1);
    s->set_whitebal(s, 1);

    // WiFi
    WiFi.setSleep(false);
    WiFi.mode(WIFI_STA);
    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED) { delay(500); Serial.print("."); }
    Serial.printf("\n✅ WiFi: %s\n", WiFi.localIP().toString().c_str());

    Serial.println("\n📡 开始流上传 + 轮询触发...\n");
}

// ===== 主循环 =====
void loop() {
    // WiFi 检测
    static unsigned long lastWifiCheck = 0;
    if (millis() - lastWifiCheck > 5000) {
        lastWifiCheck = millis();
        if (WiFi.status() != WL_CONNECTED) {
            Serial.println("⚠ WiFi lost, reconnecting...");
            WiFi.reconnect();
            delay(2000);
            return;
        }
    }

    // 1. 帧捕获 → POST Flask (GRAB_LATEST = 不阻塞, 直接返回最新帧)
    captureAndPost("/api/upload/stream", 1000);

    // 2. 轮询拍照触发 (500ms)
    static unsigned long lastPoll = 0;
    if (millis() - lastPoll > 500) {
        lastPoll = millis();
        if (checkCaptureTrigger()) {
            Serial.println("⚡ capture trigger received!");

            // 拍照 — 仍然用 GRAB_LATEST, 但此时已是高画质
            // FRAMESIZE_1080P + quality 10
            captureAndPost("/api/upload/photo", 5000);

            Serial.println("✅ capture done");
        }
    }

    // 帧率控制 — delay(0) 让出 CPU 给 WiFi 协议栈
    delay(1);
}
", "filePath": "C:\\Users\\flxd\\Desktop\\工作文件\\25物联网大赛项目\\arduino_stream\\atoms3r_camera\\atoms3r_camera.ino"}