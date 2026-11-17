@echo off
chcp 65001 >nul
title EMQX-1883
echo ===== 启动 EMQX (MQTT Broker, 端口 1883) =====
echo 控制台: http://localhost:18083
D:\emqx\bin\emqx start
if errorlevel 1 (
  echo EMQX 启动失败，请检查 D:\emqx 安装路径
  pause
  exit /b 1
)
echo ===== EMQX 已启动 =====
timeout /t 5 >nul
echo 验证: netstat -an ^| findstr 1883
pause
