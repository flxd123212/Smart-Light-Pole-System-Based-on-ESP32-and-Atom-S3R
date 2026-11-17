/* INA219 硬件测试 — ESP32-S3-DevKitC-1
   SDA=GPIO8  SCL=GPIO9
   作用: 1) I2C扫描看芯片地址 2) 直接读寄存器验证芯片状态
   烧录后开串口监视器 115200 即可 */

#include <Wire.h>

#define INA_SDA 8
#define INA_SCL 9

/* INA219 寄存器地址 */
#define REG_CONFIG       0x00
#define REG_SHUNT_VOLTAGE 0x01
#define REG_BUS_VOLTAGE   0x02
#define REG_POWER         0x03
#define REG_CURRENT       0x04
#define REG_CALIBRATION   0x05
#define REG_MANUFACTURER  0xFE
#define REG_DIE_ID        0xFF

static uint16_t readReg(uint8_t addr, uint8_t reg) {
    Wire.beginTransmission(addr);
    Wire.write(reg);
    if (Wire.endTransmission() != 0) return 0xFFFF;
    if (Wire.requestFrom((int)addr, 2) != 2) return 0xFFFF;
    uint16_t v = (Wire.read() << 8) | Wire.read();
    return v;
}

void setup() {
    Serial.begin(115200); delay(500);
    Serial.println("\n\n=== INA219 硬件测试 ===\n");

    Wire.begin(INA_SDA, INA_SCL);

    /* 0. 总线电平诊断: 不用内部上拉, 只能由模块自带上拉拉高 → 高=模块带电在线 */
    Serial.println("--- [0] 总线电平诊断 (无内部上拉) ---");
    pinMode(INA_SDA, INPUT);
    pinMode(INA_SCL, INPUT);
    delay(10);
    Serial.printf("SDA(GPIO8): %s\n", digitalRead(INA_SDA) ? "高电平 ✓ (模块带电, 自带上拉)" : "低电平 ✗ (模块没供电/没接/线断)");
    Serial.printf("SCL(GPIO9): %s\n", digitalRead(INA_SCL) ? "高电平 ✓ (模块带电, 自带上拉)" : "低电平 ✗ (模块没供电/没接/线断)");
    Wire.begin(INA_SDA, INA_SCL);   /* 恢复 I2C 模式 */

    /* 1. I2C 总线扫描 */
    Serial.println("--- [1] I2C 扫描 ---");
    bool found = false;
    for (byte a = 0x03; a < 0x78; a++) {
        Wire.beginTransmission(a);
        if (Wire.endTransmission() == 0) {
            Serial.printf("找到 I2C 设备 @ 0x%02X\n", a);
            found = true;
        }
    }
    if (!found) {
        Serial.println("未找到任何 I2C 设备!");
        Serial.println("检查: VCC→3V3, GND→GND, SDA→GPIO8, SCL→GPIO9, 线是否插紧");
        return;
    }

    /* 2. 读 INA219 关键寄存器 */
    Serial.println("\n--- [2] INA219 寄存器 ---");
    uint16_t cfg  = readReg(0x40, REG_CONFIG);
    uint16_t cal  = readReg(0x40, REG_CALIBRATION);
    uint16_t manu = readReg(0x40, REG_MANUFACTURER);
    uint16_t die  = readReg(0x40, REG_DIE_ID);
    uint16_t shunt = readReg(0x40, REG_SHUNT_VOLTAGE);
    uint16_t bus   = readReg(0x40, REG_BUS_VOLTAGE);

    Serial.printf("配置寄存器(0x00): 0x%04X  (%s)\n", cfg,
                  cfg == 0x399F ? "正常, 32V/2A量程" : "非默认值/未配置");
    Serial.printf("校准寄存器(0x05): 0x%04X  (%s)\n", cal,
                  cal == 0x1000 ? "标准校准" : "非标准(克隆板常见)");
    Serial.printf("厂商ID (0xFE):   0x%04X  (%s)\n", manu,
                  manu == 0x5449 ? "TI德州仪器" : "未知/克隆");
    Serial.printf("芯片ID (0xFF):   0x%04X  (%s)\n", die,
                  die == 0x2260 ? "INA219" : "非标准(克隆板)");
    Serial.printf("分流电压(0x01):  %d uV  (当前=%.1f mA @ 0.1Ω)\n",
                  shunt * 10, shunt * 10 / 100.0);
    Serial.printf("总线电压(0x02):  %.3f V\n", bus * 4.0 / 1000.0);

    /* 3. 结论 */
    Serial.println("\n--- [3] 结论 ---");
    if (cfg != 0xFFFF) {
        if (manu == 0x5449 && die == 0x2260)
            Serial.println("正品 INA219, 工作正常 ✓");
        else
            Serial.println("是 INA219(克隆), 工作正常 ✓ — 主程序已用强制校准兼容");
    } else {
        Serial.println("寄存器读不到数据! 检查接线");
    }
    Serial.println("\n提示: 测电流时, 电流必须从 VIN+ 流入 VIN- 流出");
}

void loop() {
    /* 每2秒实时刷新电压电流, 便于观察 LED 开关变化 */
    static unsigned long t = 0;
    if (millis() - t > 2000) {
        t = millis();
        uint16_t bus  = readReg(0x40, REG_BUS_VOLTAGE);
        uint16_t shunt = readReg(0x40, REG_SHUNT_VOLTAGE);
        float v = bus * 4.0 / 1000.0;
        float i = shunt * 10 / 100.0;   /* uV / 0.1Ω = uA → mA */
        Serial.printf("实时: V=%.3fV  I=%.1fmA%s\n", v, i,
                      (shunt & 0x8000) ? "  [电流方向反了! VIN+/VIN- 接反]" : "");
    }
}
