#!/bin/bash

set -e

# 定义 Java 服务（名称 JAR路径 项目路径 启动端口）
SERVICES=(
  "EurekaServer ./eureka-server/target/eureka-server-0.0.1-SNAPSHOT.jar ./eureka-server 8761"
  "GatewayApplication ./gateway/target/gateway-0.0.1-SNAPSHOT.jar ./gateway 8080"
  "ConfigServer ./config-server/target/config-server-0.0.1-SNAPSHOT.jar ./config-server 8888"
  "AUTH-SERVICE ./auth-service/target/auth-service-0.0.1-SNAPSHOT.jar ./auth-service 0"
  "USER-SERVICE ./user-service/target/user-service-0.0.1-SNAPSHOT.jar ./user-service 0"
  "JOB-SERVICE ./job-service/target/job-service-0.0.1-SNAPSHOT.jar ./job-service 0"
  "NOTIFICATION-SERVICE ./notification-service/target/notification-service-0.0.1-SNAPSHOT.jar ./notification-service 0"
  "FILE-STORAGE ./file-storage/target/file-storage-0.0.1-SNAPSHOT.jar ./file-storage 0"
)

# 定义要等待健康的容器名（与 docker-compose.yml 中保持一致）
CONTAINERS=("postgres" "zookeeper" "kafka")

# 打包
echo "🔨 开始打包所有服务..."
for SERVICE in "${SERVICES[@]}"; do
  NAME=$(echo $SERVICE | awk '{print $1}')
  JAR=$(echo $SERVICE | awk '{print $2}')
  DIR=$(echo $SERVICE | awk '{print $3}')

  echo "📦 打包 $NAME..."
  (cd "$DIR" && chmod +x mvnw && ./mvnw clean package -DskipTests)

  if [ ! -f "$JAR" ]; then
    echo "❌ 打包失败：未找到 $JAR"
    exit 1
  fi
done

echo "✅ 所有服务打包完成！"

# 启动容器
echo "🟡 启动 Docker Compose 容器..."
docker compose up -d

# 等待容器健康检查通过
wait_for_container_healthy() {
  local NAME=$1
  local RETRIES=30
  local COUNT=0

  echo "⏳ 等待容器 $NAME 健康..."

  until [ "$(docker inspect -f '{{.State.Health.Status}}' "$NAME")" == "healthy" ]; do
    sleep 2
    COUNT=$((COUNT+1))
    if [ $COUNT -ge $RETRIES ]; then
      echo "❌ 容器 $NAME 健康检查超时"
      exit 1
    fi
  done

  echo "✅ 容器 $NAME 已通过健康检查"
}

for NAME in "${CONTAINERS[@]}"; do
  wait_for_container_healthy "$NAME"
done

# 等待端口可用
wait_for_port() {
  local PORT=$1
  local NAME=$2
  local RETRIES=30
  local COUNT=0

  echo "⏳ 等待服务 $NAME 端口 $PORT 启动..."
  until nc -z localhost $PORT; do
    sleep 1
    COUNT=$((COUNT+1))
    if [ $COUNT -ge $RETRIES ]; then
      echo "❌ 端口 $PORT 启动超时"
      exit 1
    fi
  done
  echo "✅ 服务 $NAME 已启动在端口 $PORT"
}

wait_for_service_in_eureka() {
  local SERVICE_NAME=$1
  local RETRIES=30
  local COUNT=0

  echo "⏳ 等待服务 $SERVICE_NAME 注册到 Eureka..."
  until curl -s http://localhost:8761/eureka/apps/$SERVICE_NAME | grep -q "<status>UP</status>"; do
    sleep 1
    COUNT=$((COUNT+1))
    if [ $COUNT -ge $RETRIES ]; then
      echo "❌ 服务 $SERVICE_NAME 未注册或未启动"
      exit 1
    fi
  done
  echo "✅ 服务 $SERVICE_NAME 已注册并处于 UP 状态"
}


# 启动 Spring Boot 服务
LOG_DIR="./logs"
mkdir -p "$LOG_DIR"
for SERVICE in "${SERVICES[@]}"; do
  NAME=$(echo $SERVICE | awk '{print $1}')
  JAR=$(echo $SERVICE | awk '{print $2}')
  PORT=$(echo $SERVICE | awk '{print $4}')
  LOG="${LOG_DIR}/${NAME}.log"

  echo "🟢 启动 $NAME..."
  nohup java -jar "$JAR" > "$LOG" 2>&1 &
#  nohup java -jar "$JAR" > /dev/null 2>&1 &

  if [ "$PORT" -eq 0 ]; then
    wait_for_service_in_eureka "$SERVICE_NAME"
  else
    wait_for_port "$PORT" "$SERVICE_NAME"
  fi
#  if [ "$PORT" -gt 0 ]; then
#    wait_for_port "$PORT" "$SERVICE_NAME"
#  fi
done

echo "🎉 所有容器与微服务均已成功启动！"
