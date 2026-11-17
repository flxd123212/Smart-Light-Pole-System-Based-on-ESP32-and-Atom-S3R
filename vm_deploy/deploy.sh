#!/bin/bash
# ============================================================
# 智能灯杆 AI 服务一键部署脚本 (Linux 虚拟机 / Ubuntu 20.04)
# 内容: 安装依赖 + 启动 YOLO(:5001) + 启动 Flask(:5000)
# 用法: bash deploy.sh
# ============================================================
set -e

# ---- 如果在 VMware 共享文件夹(/mnt/hgfs)里运行: venv 无法创建符号链接 ----
if [ -z "$VM_DEPLOY_HOME" ] && echo "$(pwd)" | grep -q '/mnt/hgfs'; then
  echo "[提示] 共享文件夹不支持 venv, 自动复制到 ~/vm_deploy 并重新执行..."
  mkdir -p ~/vm_deploy
  cp -r ./* ~/vm_deploy/ 2>/dev/null || true
  rm -rf ~/vm_deploy/venv
  cd ~/vm_deploy
  VM_DEPLOY_HOME=1 bash ~/vm_deploy/deploy.sh "$@"
  exit $?
fi

cd "$(dirname "$0")"

echo "==== 1/4 检查磁盘空间 / Python3 / venv ===="
df -h / | tail -1
python3 --version
if ! python3 -c "import ensurepip" >/dev/null 2>&1; then
  echo "缺少 python3-venv, 正在安装..."
  sudo apt update && sudo apt install -y python3-venv python3-pip
fi

echo "==== 2/4 创建虚拟环境并安装依赖 ===="
if [ ! -d "venv" ]; then
  python3 -m venv venv
fi
source venv/bin/activate
pip install --upgrade pip -i https://pypi.tuna.tsinghua.edu.cn/simple

echo "---- 2.1 安装 CPU 版 torch (官方 CPU 源, 约170MB; 失败自动换阿里云) ----"
pip install "torch==2.3.1+cpu" "torchvision==0.18.1+cpu" --index-url https://download.pytorch.org/whl/cpu   || pip install "torch==2.3.1+cpu" "torchvision==0.18.1+cpu" -f https://mirrors.aliyun.com/pytorch-wheels/cpu/

echo "---- 2.2 安装其余依赖 (清华源) ----"
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

echo "---- 2.3 清理 pip 缓存释放磁盘 ----"
pip cache purge >/dev/null 2>&1 || true

echo "==== 3/4 启动 YOLO 服务 (端口 5001) ===="
nohup python yolo11-flask.py > yolo.log 2>&1 &
echo "YOLO PID: $!"
sleep 10
curl -s -o /dev/null -w "YOLO /infer HTTP %{http_code}\n" -X POST http://127.0.0.1:5001/infer -H "Content-Type: application/json" -d '{"image":"data:image/jpeg;base64,"}' || echo "YOLO 启动中, 稍后验证"

echo "==== 4/4 启动 Flask (端口 5000) ===="
if [ -z "$MQTT_HOST" ]; then MQTT_HOST=192.168.223.134; fi   # EMQX 所在 Windows 主机 IP (当前热点网段)
if [ -z "$CAMERA_CODE" ]; then CAMERA_CODE=CAM001; fi
if [ -z "$LAMP_ID" ]; then LAMP_ID=LP001; fi
export MQTT_HOST CAMERA_CODE LAMP_ID
nohup python app.py > flask.log 2>&1 &
echo "Flask PID: $!"
echo
echo "============================================================"
echo " 部署完成!"
echo "  YOLO : http://127.0.0.1:5001/infer"
echo "  Flask: http://0.0.0.0:5000"
echo "  MQTT 上报主题: light/camera/$CAMERA_CODE/capture -> $MQTT_HOST:1883"
echo " 日志: tail -f flask.log / yolo.log"
echo "============================================================"
