Set-Location "D:\工作文件\25物联网大赛项目\project\Smart Light Pole System Based on ESP32 and Atom‑S3R"
Write-Host "===== 启动后端 (端口 8081) ====="
java -jar iotlight-admin\target\iotlight-admin.jar --server.port=8081
pause
