@echo off
echo 🔧 AWS CLI Setup for Video Translation Service
echo ===============================================
echo.

REM Run PowerShell script
powershell -ExecutionPolicy Bypass -File "setup-aws.ps1"

echo.
echo Press any key to exit...
pause >nul 