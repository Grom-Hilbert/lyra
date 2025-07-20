@echo off
REM Lyra应用启动检查脚本

echo 开始检查Lyra应用...

REM 检查Java版本
echo 检查Java版本...
java -version
if %ERRORLEVEL% neq 0 (
    echo ERROR: Java未正确安装或配置
    exit /b 1
)

REM 检查项目结构
echo 检查项目结构...
if not exist "build.gradle" (
    echo ERROR: 未找到build.gradle文件，请确保在项目根目录运行
    exit /b 1
)

REM 清理构建
echo 清理之前的构建...
call gradlew clean

REM 编译项目
echo 编译项目...
call gradlew compileJava
if %ERRORLEVEL% neq 0 (
    echo ERROR: 编译失败
    exit /b 1
)

REM 运行测试
echo 运行测试...
call gradlew test --tests DatabaseIntegrationTest
if %ERRORLEVEL% neq 0 (
    echo ERROR: 数据库集成测试失败
    exit /b 1
)

REM 尝试启动应用（短时间）
echo 尝试启动应用进行快速检查...
timeout /t 2 /nobreak > nul
call gradlew bootRun --args="--server.port=9999" &
set APP_PID=%!

REM 等待一段时间
timeout /t 15 /nobreak > nul

REM 检查应用是否响应
echo 检查应用是否响应...
curl -f http://localhost:9999/actuator/health 2>nul
if %ERRORLEVEL% equ 0 (
    echo SUCCESS: 应用启动成功
) else (
    echo WARNING: 应用可能启动失败或端口未响应
)

REM 停止应用
taskkill /f /pid %APP_PID% 2>nul

echo 检查完成 