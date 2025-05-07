#!/bin/bash

# 停止 Java 服务
echo "停止 Java 服务..."
pkill -f 'java -jar ./eureka-server/target/eureka-server-0.0.1-SNAPSHOT.jar'
pkill -f 'java -jar ./gateway/target/gateway-0.0.1-SNAPSHOT.jar'
pkill -f 'java -jar ./config-server/target/config-server-0.0.1-SNAPSHOT.jar'
pkill -f 'java -jar ./auth-service/target/auth-service-0.0.1-SNAPSHOT.jar'
pkill -f 'java -jar ./user-service/target/user-service-0.0.1-SNAPSHOT.jar'
pkill -f 'java -jar ./job-service/target/job-service-0.0.1-SNAPSHOT.jar'
pkill -f 'java -jar ./notification-service/target/notification-service-0.0.1-SNAPSHOT.jar'
pkill -f 'java -jar ./file-storage/target/file-storage-0.0.1-SNAPSHOT.jar'

# 停止 Docker Compose 服务
echo "停止 Docker Compose 服务..."
docker-compose down

# 可选：删除日志文件
echo "删除日志文件..."
rm -f ./ConfigServer.log
rm -f ./EurekaServer.log
rm -f ./GatewayApplication.log

echo "服务已关闭并清理完成！"
