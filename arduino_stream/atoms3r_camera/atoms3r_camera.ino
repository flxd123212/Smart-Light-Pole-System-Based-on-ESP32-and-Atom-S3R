#include <Arduino.h>
#include "esp_camera.h"
#include <WiFi.h>
#include <WebServer.h>
#include <HTTPClient.h>

#define POWER_GPIO_NUM  18

// 原始引脚（已验证）
#define PWDN_GPIO_NUM   -1
#define RESET_GPIO_NUM  -1
#define XCLK_GPIO_NUM   21
#define SIOD_GPIO_NUM   12
#define SIOC_GPIO_NUM    9
#define Y9_GPIO_NUM     13
#define Y8_GPIO_NUM     11
#define Y7_GPIO_NUM     17
#define Y6_GPIO_NUM      4
#define Y5_GPIO_NUM     48
#define Y4_GPIO_NUM     46
#define Y3_GPIO_NUM     42
#define Y2_GPIO_NUM      3
#define VSYNC_GPIO_NUM  10
#define HREF_GPIO_NUM   14
#define PCLK_GPIO_NUM   40

const char* ssid      = "flxd";
const char* password  = "flxd123212";
/* 单机测试: Flask 在同一台机器 */
const char* flaskHost = "192.168.223.134";
const int   flaskPort = 5000;
/* 双服务器部署: Flask 在 AI 服务器
const char* flaskHost = "192.168.223.135";
const int   flaskPort = 5000; */

WebServer sv(80);
bool captureReq = false;
bool streaming = false;

// ---- 摄像头初始化 ----
bool initCamera() {
    camera_config_t config{};
    config.ledc_channel = LEDC_CHANNEL_0;
    config.ledc_timer   = LEDC_TIMER_0;
    config.pin_d0       = Y2_GPIO_NUM;
    config.pin_d1       = Y3_GPIO_NUM;
    config.pin_d2       = Y4_GPIO_NUM;
    config.pin_d3       = Y5_GPIO_NUM;
    config.pin_d4       = Y6_GPIO_NUM;
    config.pin_d5       = Y7_GPIO_NUM;
    config.pin_d6       = Y8_GPIO_NUM;
    config.pin_d7       = Y9_GPIO_NUM;
    config.pin_xclk     = XCLK_GPIO_NUM;
    config.pin_pclk     = PCLK_GPIO_NUM;
    config.pin_vsync    = VSYNC_GPIO_NUM;
    config.pin_href     = HREF_GPIO_NUM;
    config.pin_sscb_sda = SIOD_GPIO_NUM;
    config.pin_sscb_scl = SIOC_GPIO_NUM;
    config.pin_pwdn     = PWDN_GPIO_NUM;
    config.pin_reset    = RESET_GPIO_NUM;
    config.xclk_freq_hz = 20000000;
    config.pixel_format = PIXFORMAT_JPEG;
    config.frame_size   = FRAMESIZE_HD;
    config.jpeg_quality = 12;
    config.fb_count     = 2;

    esp_err_t err = esp_camera_init(&config);
    if (err != ESP_OK) { Serial.printf("CAM FAIL: 0x%x\n", err); return false; }
    Serial.println("CAM OK: 720P fb=2");
    return true;
}

// ---- HTTP POST 到 Flask ----
bool httpPost(const char* path, const uint8_t* d, size_t n, int t = 3000) {
    if (WiFi.status() != WL_CONNECTED) return false;
    WiFiClient wc; HTTPClient h;
    h.setReuse(true);
    h.begin(wc, String("http://") + flaskHost + ":" + flaskPort + path);
    h.addHeader("Content-Type", "image/jpeg");
    h.setTimeout(t);
    int code = h.POST((uint8_t*)d, n);
    h.end();
    return code == 200;
}

// ---- 首页 ----
void handleRoot() {
    String h = "<html><meta charset='utf-8'><body style='background:#111;text-align:center'>";
    h += "<h2 style='color:white'>AtomS3R M12 720P</h2>";
    h += "<img src='/stream' style='max-width:100%'><br>";
    h += "<form action='/capture' method='POST'>";
    h += "<button style='font-size:20px;padding:10px 30px'>\U0001f4f8</button></form></body></html>";
    sv.send(200, "text/html", h);
}

// ---- MJPEG 流 (手动 HTTP 头) ----
void handleStream() {
    WiFiClient client = sv.client();
    streaming = true;

    client.println("HTTP/1.1 200 OK");
    client.println("Content-Type: multipart/x-mixed-replace; boundary=frame");
    client.println("Cache-Control: no-cache");
    client.println("Connection: keep-alive");
    client.println();

    String b = "\r\n--frame\r\nContent-Type: image/jpeg\r\n\r\n";
    while (!captureReq) {
        camera_fb_t* fb = esp_camera_fb_get();
        if (!fb) { delay(10); continue; }

        digitalWrite(LED_BUILTIN, !digitalRead(LED_BUILTIN));
        client.write((uint8_t*)b.c_str(), b.length());
        client.write(fb->buf, fb->len);
        client.write("\r\n");
        // 同时转发到 Flask
        httpPost("/api/upload/stream", fb->buf, fb->len, 200);
        esp_camera_fb_return(fb);

        if (!client.connected()) break;
        sv.handleClient();
        delay(30);
    }
    streaming = false;
    digitalWrite(LED_BUILTIN, LOW);
}

// ---- 拍照 ----
void handleCapture() { captureReq = true; sv.send(200, "text/plain", "OK"); }

void takePhoto() {
    captureReq = false;
    camera_fb_t* fb = esp_camera_fb_get();
    if (!fb) { Serial.println("CAP FAIL"); return; }
    Serial.printf("PHOTO %u B\n", fb->len);
    httpPost("/api/upload/photo", fb->buf, fb->len, 8000);
    esp_camera_fb_return(fb);
}

// ---- 启动 ----
void setup() {
    Serial.begin(115200); delay(1000);
    Serial.println("\n\n=== AtomS3R M12 WebServer ===\n");

    pinMode(POWER_GPIO_NUM, OUTPUT); digitalWrite(POWER_GPIO_NUM, LOW); delay(500);
    if (!initCamera()) { while (1) delay(100); }

    sensor_t* s = esp_camera_sensor_get();
    if (s) { s->set_gain_ctrl(s,1); s->set_exposure_ctrl(s,1); s->set_whitebal(s,1); }

    pinMode(LED_BUILTIN, OUTPUT); digitalWrite(LED_BUILTIN, LOW);

    WiFi.setSleep(false); WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED) delay(500);
    Serial.printf("WiFi: %s\n", WiFi.localIP().toString().c_str());

    // 注册 IP
    { WiFiClient wc; HTTPClient h;
      h.begin(wc, String("http://") + flaskHost + ":" + flaskPort + "/api/register/camera");
      h.addHeader("Content-Type", "application/json");
      h.POST(String("{\"ip\":\"") + WiFi.localIP().toString() + "\"}");
      h.end(); }

    sv.on("/", handleRoot);
    sv.on("/stream", handleStream);
    sv.on("/capture", HTTP_POST, handleCapture);
    sv.begin();
    Serial.printf("http://%s/\n", WiFi.localIP().toString().c_str());
    Serial.println("/stream   MJPEG");
    Serial.println("POST /capture   photo\n");
}

// ---- 主循环 ----
void loop() {
    static unsigned long lw = 0, lf = 0;
    if (millis() - lw > 5000) { lw = millis();
        if (WiFi.status() != WL_CONNECTED) WiFi.reconnect(); }
    sv.handleClient();
    if (captureReq) takePhoto();

    // 非推流时保持 Flask 有帧
    if (!streaming && !captureReq && millis() - lf > 200) {
        lf = millis();
        camera_fb_t* fb = esp_camera_fb_get();
        if (fb) { httpPost("/api/upload/stream", fb->buf, fb->len, 200);
                  esp_camera_fb_return(fb); }
    }
    delay(1);
}