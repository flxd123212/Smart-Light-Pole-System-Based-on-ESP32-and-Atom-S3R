@echo off
title Vue-81
echo ===== 启动 Vue 前端 (端口 81, 代理->8081) =====
cd /d C:\Users\flxd\Desktop\工作文件\25物联网大赛项目\project\SMARTL~1\start-vue
if not exist node_modules (
  echo ===== npm install =====
  call npm install --registry=https://registry.npmmirror.com
)
if exist node_modules\.cache (
  rd /s /q node_modules\.cache
  echo ===== 缓存已清除 =====
)
set port=81
call npm run dev
pause
