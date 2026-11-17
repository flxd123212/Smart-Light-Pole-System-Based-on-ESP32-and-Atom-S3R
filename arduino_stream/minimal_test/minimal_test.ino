/* 最小摄像头测试 — 完全还原原始 smart_lamp_post */
#include <Arduino.h>
#include "esp_camera.h"

// 原始引脚 (smart_lamp_post 工作过)
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
#define POWER_N          18

static bool initCamera() {
    camera_config_t config{};
    config.ledc_channel    = LEDC_CHANNEL_0;
    config.ledc_timer      = LEDC_TIMER_0;
    config.pin_d0          = Y2_GPIO_NUM;
    config.pin_d1          = Y3_GPIO_NUM;
    config.pin_d2          = Y4_GPIO_NUM;
    config.pin_d3          = Y5_GPIO_NUM;
    config.pin_d4          = Y6_GPIO_NUM;
    config.pin_d5          = Y7_GPIO_NUM;
    config.pin_d6          = Y8_GPIO_NUM;
    config.pin_d7          = Y9_GPIO_NUM;
    config.pin_xclk        = XCLK_GPIO_NUM;
    config.pin_pclk        = PCLK_GPIO_NUM;
    config.pin_vsync       = VSYNC_GPIO_NUM;
    config.pin_href        = HREF_GPIO_NUM;
    config.pin_sscb_sda    = SIOD_GPIO_NUM;
    config.pin_sscb_scl    = SIOC_GPIO_NUM;
    config.pin_pwdn        = PWDN_GPIO_NUM;
    config.pin_reset       = RESET_GPIO_NUM;
    config.xclk_freq_hz    = 20000000;
    config.pixel_format    = PIXFORMAT_JPEG;
    config.frame_size      = FRAMESIZE_QVGA;
    config.jpeg_quality    = 12;
    config.fb_count        = 2;

    esp_err_t err = esp_camera_init(&config);
    if (err != ESP_OK) {
        Serial.printf("FAIL: 0x%x\n", err);
        return false;
    }
    Serial.println("CAMERA OK");
    return true;
}

void setup() {
    Serial.begin(115200);
    delay(1000);
    Serial.println("\n=== MINIMAL CAMERA TEST ===\n");

    pinMode(POWER_N, OUTPUT);
    digitalWrite(POWER_N, LOW);
    delay(500);

    if (!initCamera()) {
        while (1) delay(100);
    }

    camera_fb_t *fb = esp_camera_fb_get();
    if (fb) {
        Serial.printf("FRAME: %u bytes\n", fb->len);
        esp_camera_fb_return(fb);
    }
}

void loop() { delay(1000); }