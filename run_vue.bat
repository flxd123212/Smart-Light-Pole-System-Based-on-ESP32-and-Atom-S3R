@echo off
title Vue-81
pushd %~dp0start-vue
if exist node_modules\.cache rd /s /q node_modules\.cache
set port=81
npm run dev
pause