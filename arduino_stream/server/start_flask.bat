@echo off
chcp 65001 >nul
title Flask-5000
set PYTHONIOENCODING=utf-8
echo ===== 智能灯杆 Flask 服务器 (端口 5000) =====
echo.
python app.py
pause