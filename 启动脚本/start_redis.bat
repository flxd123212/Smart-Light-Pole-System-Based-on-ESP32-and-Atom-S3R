@echo off
chcp 65001 >nul
title Redis-6379
echo ===== 启动 Redis (端口 6379) =====
D:\Redis\redis-server.exe --port 6379
pause
