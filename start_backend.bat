@echo off
title iotlight-admin-8081
echo ===== 启动后端 (端口 8081) =====
java -jar iotlight-admin\target\iotlight-admin.jar --server.port=8081
pause
