# 虚拟机部署指南 — Flask + YOLO (AI 图像服务)

> 目标: 在 VMware 虚拟机 (Ubuntu/CentOS) 上运行 Flask(:5000) 和 YOLO(:5001)，
> 摄像头(AtomS3R)推流拍照到 Flask，Flask 做 YOLO 行人识别，并把结果通过 MQTT
> 上报给 Windows 主机上的 iot-light-main 后端 (主题 `light/camera/CAM001/capture`)。

---

## 一、架构与网络拓扑 (全链路)

```
ESP32-S3 传感器 ──MQTT lamp/LP001──▶ EMQX(:1883, Windows主机) ──▶ iot-light-main 后端(:8088, Windows主机) ──▶ MySQL iot-light
ESP32-S3 轮询 ──HTTP──▶ Flask(:5000, 虚拟机) ◀──HTTP 推流/拍照── AtomS3R 摄像头
Flask 检测线程 ──HTTP /infer──▶ YOLO(:5001, 虚拟机)
Flask ──MQTT light/camera/CAM001/capture──▶ EMQX ──▶ iot-light-main 后端 ──▶ 写 iot_camera_capture 表
浏览器 ──HTTP──▶ Vue 前端(:80/8081) ──▶ 后端 :8088
浏览器 ──HTTP──▶ Flask :5000 (实时画面/图库)
```

- Windows 主机 IP (当前): **192.168.2.31**  (EMQX / MySQL / Redis / 后端 / Vue 都在这里)
- 虚拟机: 跑 **Flask :5000 + YOLO :5001**
- AtomS3R / ESP32: 连接 WiFi `flxd`，与主机、虚拟机同一局域网

---

## 二、桥接网络 (让虚拟机与 ESP32 同一网段) ★核心

你的两个虚拟机 (D:\VMware\flxd123212PRIVATE_CONTACT_REMOVED 和 D:\VMware\centos) 目前都是 **NAT** 模式，
虚拟机只能通过 192.168.127.x 上网，**ESP32 设备无法直接访问它**。

### 方法 A: 桥接 (推荐，最省事)
1. 关闭虚拟机 → 打开 VMware Workstation → 选中虚拟机 → **编辑虚拟机设置** → 网络适配器
2. 网络连接改为 **桥接模式(Bridged)** → 勾选"复制物理网络连接状态"（可选）
3. 开机后在虚拟机里执行 `ip addr`，确认拿到 **192.168.2.x** 的 IP (与主机同一网段)
   - 若桥接后没拿到 IP：检查虚拟网络编辑器 (编辑→虚拟网络编辑器) 里 VMnet0 桥接到的
     物理网卡是否为 **WLAN/无线网卡**（笔记本 WiFi 桥接偶尔不灵，见方法 B）
4. 记下虚拟机 IP，例如 192.168.2.10，后面所有配置都用它

### 方法 B: NAT + 端口转发 (桥接失败时的备选)
1. 保持 NAT，在 VMware 里设置端口转发:
   - 主机 5000 → 虚拟机 5000 (Flask)
   - 主机 5001 → 虚拟机 5001 (YOLO)
   - (编辑→虚拟网络编辑器→VMnet8(NAT)→NAT设置→添加)
2. 这样 ESP32/浏览器访问 **192.168.2.31:5000** 就等于访问虚拟机 Flask。
3. 注意: 虚拟机内 Flask 的 MQTT 上报地址仍是 Windows 主机 IP (192.168.2.31:1883)，
   虚拟机通过 NAT 网关可以访问到主机。

> 无论哪种方式，都要保证:
> - 虚拟机 → 主机 1883 (EMQX) 通: `nc -vz 192.168.2.31 1883` 或 `telnet`
> - 主机/ESP32 → 虚拟机 5000 (Flask) 通
> - Windows 防火墙放行 1883、5000、5001、8088 端口(入站规则)

---

## 三、把文件上传到虚拟机 (3 种方法任选)

部署包就是本目录 (vm_deploy)，包含:
```
vm_deploy/
├── app.py               # Flask 图像服务 (MQTT 上报版)
├── yolo11-flask.py      # YOLO 推理服务 (:5001)
├── yolo11n.pt           # YOLO11 模型
├── requirements.txt     # Python 依赖
├── deploy.sh            # 一键部署脚本 (安装依赖+启动)
└── templates/index.html # Flask 前端页
```

### 方法 1: scp (命令行, 需要虚拟机开 SSH)
```bash
# 先在虚拟机安装并启动 ssh: sudo apt install openssh-server && sudo systemctl enable --now ssh
scp -r vm_deploy/* 用户名@虚拟机IP:/home/用户名/vm_deploy/
# 例如: scp -r vm_deploy/* flxd@192.168.2.10:/home/flxd/vm_deploy/
```

### 方法 2: VMware 共享文件夹 (推荐, 图形界面)
1. 虚拟机设置 → 选项 → 共享文件夹 → 总是启用 → 添加 → 选择 **vm_deploy** 文件夹
2. 虚拟机里访问: `ls /mnt/hgfs/vm_deploy/` (需要 vmware-tools / open-vm-tools)
3. **不要在共享文件夹里直接建 venv/运行** —— vmhgfs 文件系统不支持符号链接,
   会报 `[Errno 95] Operation not supported: 'lib' -> '.../venv/lib64'`
4. 直接运行 `bash deploy.sh` 即可 —— 脚本会自动把文件复制到 `~/vm_deploy`
   并在那里建 venv 运行 (见下方"常见问题")

### 方法 3: 拖拽复制 / U盘
- 安装 open-vm-tools 后可直接拖拽文件进虚拟机桌面
- 或用 U 盘拷贝 (虚拟机设置里把 U 盘直通给虚拟机)

---

## 四、在虚拟机里启动 (一键)

```bash
cd ~/vm_deploy
chmod +x deploy.sh
./deploy.sh
```

脚本会: 建虚拟环境 → 装依赖(清华源) → 启动 YOLO :5001 → 启动 Flask :5000

**手动启动 (不用脚本):**
```bash
python3 -m venv venv && source venv/bin/activate
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
python yolo11-flask.py &            # YOLO :5001
export MQTT_HOST=192.168.2.31       # ← 改成 EMQX 所在 Windows 主机 IP
python app.py &                     # Flask :5000
```

**验证:**
```bash
curl http://127.0.0.1:5000/api/stream/latest          # Flask 活着
curl -X POST http://127.0.0.1:5001/infer -H "Content-Type: application/json" -d '{"image":"data:image/jpeg;base64,"}'  # YOLO 活着(400=正常)
tail -f flask.log                                       # 看到 "MQTT ✅ 已连接" 说明上报链路 OK
```

**常见问题:**
| 现象 | 解决 |
|---|---|
| `[Errno 95] Operation not supported: ... venv/lib64` | 在共享文件夹里建 venv 了。直接 `bash deploy.sh`, 脚本会自动复制到 ~/vm_deploy 再建 |
| Python 3.8 (Ubuntu 20.04) | 已兼容: requirements.txt 已锁定 flask 3.0.3 / torch 2.4.1 / ultralytics 8.3.6 等 3.8 可用版本 |
| 提示未安装 python3-venv | 脚本会自动 `sudo apt install python3-venv`, 需要输入密码 |
| pip 安装很慢/超时 | 已默认清华源; 失败重跑 deploy.sh 即可续装 |

---

## 五、烧录/修改设备端

| 文件 | 要改的地方 |
|---|---|
| `arduino_stream/esp32_s3_wroom/esp32_s3_wroom.ino` | `mqttHost`=Windows主机IP(192.168.2.31)；`flaskHost`=虚拟机IP(如192.168.2.10) |
| `arduino_stream/atoms3r_camera/atoms3r_camera.ino` | `flaskHost`=虚拟机IP(如192.168.2.10) |

ESP32 上电后: 连 WiFi `flxd` → 连 EMQX → 每5秒上报 `lamp/LP001` → 轮询 Flask
`/api/trigger/consume` → 有触发就 POST AtomS3R `/capture` 拍照 → AtomS3R 把照片
POST 给 Flask → Flask 保存照片并通过 MQTT 上报抓拍记录。

---

## 六、端口汇总

| 服务 | 位置 | 端口 |
|---|---|---|
| EMQX (MQTT) | Windows 主机 | 1883 |
| EMQX 控制台 | Windows 主机 | 18083 (admin 已登录) |
| iot-light-main 后端 | Windows 主机 | 8088 |
| Vue 前端 | Windows 主机 | 8081 (npm run dev) |
| Flask | 虚拟机 | 5000 |
| YOLO | 虚拟机 | 5001 |
| MySQL | Windows 主机 | 3306 |
| Redis | Windows 主机 | 6379 |
