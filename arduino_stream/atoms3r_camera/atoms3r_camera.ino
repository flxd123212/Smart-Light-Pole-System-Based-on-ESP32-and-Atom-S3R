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

const char* ssid      = "YOUR_WIFI_SSID";
const char* password  = "YOUR_WIFI_PASSWORD";
/* 全链路部署: Flask 在 Windows 主机, 按实际 IP 修改 */
const char* flaskHost = "192.168.105.134";  /* 本机 IP (Flask :5000 所在) */
const int   flaskPort = 5000;

WebServer sv(80);
bool captureReq = false;
bool streaming = false;

/* ---- FPS 优化 ----
   1) 推流用 VGA (640x480), YOLO 检测只需 640x360, 720p 纯属浪费带宽;
      拍照时才临时切回 720p 高清。
   2) 流上传复用一条 keep-alive 长连接, 避免每帧重新 TCP 握手
      (旧实现每帧新建 WiFiClient, 2.4G 高延迟下每帧要 300~500ms, 帧率被压到 1~2fps)。
   3) 推流间隔 150ms ≈ 6~7fps; 上传超时放宽到 1000ms, 慢帧不再被误杀。
*/
#define STREAM_INTERVAL_MS 150
#define STREAM_FRAMESIZE   FRAMESIZE_VGA   /* 推流分辨率 (640x480) */
#define PHOTO_FRAMESIZE    FRAMESIZE_HD    /* 拍照分辨率 (1280x720) */

/* 流上传用全局持久连接 (keep-alive 复用) */
static WiFiClient  streamWc;
static HTTPClient  streamHttp;
static bool        streamHttpInited = false;

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
    config.frame_size   = STREAM_FRAMESIZE;   /* 推流 VGA; 拍照时再切 720p */
    config.jpeg_quality = 12;
    config.fb_count     = 2;

    esp_err_t err = esp_camera_init(&config);
    if (err != ESP_OK) { Serial.printf("CAM FAIL: 0x%x\n", err); return false; }
    Serial.println("CAM OK: VGA(stream)/720P(photo) fb=2");
    return true;
}

// ---- HTTP POST 到 Flask (拍照等低频请求, 每次新建连接) ----
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

// ---- 流上传: 复用 keep-alive 长连接 (高帧率关键) ----
bool httpPostStream(const uint8_t* d, size_t n) {
    if (WiFi.status() != WL_CONNECTED) return false;
    const String url = String("http://") + flaskHost + ":" + flaskPort + "/api/upload/stream";
    if (!streamHttpInited) {
        streamHttp.setReuse(true);          // 保持连接供下次复用
        streamHttp.setTimeout(1000);        // 放宽超时, 慢帧不误杀
        streamHttp.setConnectTimeout(1000); // 连接超时也设上, 避免卡死
        streamHttpInited = true;
    }
    streamHttp.begin(streamWc, url);
    streamHttp.addHeader("Content-Type", "image/jpeg");
    streamHttp.addHeader("Connection", "keep-alive");
    int code = streamHttp.POST((uint8_t*)d, n);
    if (code != 200) {
        // 服务端重启/断连会留下半开连接: setReuse(true) 的 end() 不会关底层 socket,
        // 必须先 streamWc.stop() 强制断开, 否则重试仍发在同一根死连接上,
        // 每次等超时失败 -> 推流停摆 (这就是 Flask 重启后前端没画面的原因)
        streamHttp.end();
        streamWc.stop();
        streamHttp.begin(streamWc, url);
        streamHttp.addHeader("Content-Type", "image/jpeg");
        streamHttp.addHeader("Connection", "keep-alive");
        code = streamHttp.POST((uint8_t*)d, n);
    }
    streamHttp.end();                       // setReuse(true) 下 end() 保留连接
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
        // 同时转发到 Flask (长连接)
        httpPostStream(fb->buf, fb->len);
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
    sensor_t* s = esp_camera_sensor_get();
    // 推流用 VGA, 拍照时临时切回 720p 高清, 拍完还原
    if (s) s->set_framesize(s, PHOTO_FRAMESIZE);
    delay(200);  // 等分辨率切换稳定
    // 丢弃切换后的前两帧 (传感器刚切分辨率时输出不稳定)
    for (int i = 0; i < 2; i++) {
        camera_fb_t* fb = esp_camera_fb_get();
        if (fb) esp_camera_fb_return(fb);
        delay(50);
    }
    camera_fb_t* fb = esp_camera_fb_get();
    if (!fb) {
        Serial.println("CAP FAIL");
        if (s) s->set_framesize(s, STREAM_FRAMESIZE);
        return;
    }
    Serial.printf("PHOTO %u B\n", fb->len);
    httpPost("/api/upload/photo", fb->buf, fb->len, 8000);
    esp_camera_fb_return(fb);
    if (s) s->set_framesize(s, STREAM_FRAMESIZE);
    // 切回 VGA 后也丢弃前两帧, 避免花屏帧推流到 Flask
    delay(200);
    for (int i = 0; i < 2; i++) {
        camera_fb_t* fb = esp_camera_fb_get();
        if (fb) esp_camera_fb_return(fb);
        delay(50);
    }
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

    // 非推流时保持 Flask 有帧 (长连接, 150ms 一帧 ≈ 6fps)
    if (!streaming && !captureReq && millis() - lf > STREAM_INTERVAL_MS) {
        lf = millis();
        camera_fb_t* fb = esp_camera_fb_get();
        if (fb) { httpPostStream(fb->buf, fb->len);
                  esp_camera_fb_return(fb); }
    }
    delay(1);
}