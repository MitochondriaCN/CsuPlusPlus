#!/usr/bin/env bash
set -euo pipefail

image="${1:?Usage: deploy.sh IMAGE_REFERENCE}"
name=csuplusplus
previous=csuplusplus-previous
env_file=/opt/csuplusplus/app.env

test -s "$env_file"
# 拉取成功后才停止旧容器；已有镜像层会由 Docker 复用。
# 为网络卡住设置单次超时，并采用指数退避重试。
pull_attempts=5
pull_timeout=180
retry_delay=10
for ((attempt = 1; attempt <= pull_attempts; attempt++)); do
  echo "Pulling $image (attempt $attempt/$pull_attempts, timeout ${pull_timeout}s)"
  if timeout --kill-after=10s "${pull_timeout}s" docker pull "$image"; then
    break
  else
    pull_status=$?
    echo "Image pull failed (exit $pull_status)." >&2
  fi
  if [ "$attempt" -eq "$pull_attempts" ]; then
    echo 'Image pull failed after all attempts; existing container was not changed.' >&2
    exit 1
  fi
  echo "Retrying in ${retry_delay}s..."
  sleep "$retry_delay"
  retry_delay=$((retry_delay * 2))
done

# 保留旧容器，若新版本启动失败则恢复；数据库迁移不随容器回滚。
if docker container inspect "$previous" >/dev/null 2>&1; then
  docker rm -f "$previous"
fi
has_previous=false
if docker container inspect "$name" >/dev/null 2>&1; then
  docker stop --time 30 "$name"
  docker rename "$name" "$previous"
  has_previous=true
fi

rollback() {
  trap - ERR
  docker logs --tail 100 "$name" || true
  docker rm -f "$name" || true
  if [ "$has_previous" = true ]; then
    docker rename "$previous" "$name"
    docker start "$name"
  fi
  exit 1
}
trap rollback ERR

docker run -d \
  --name "$name" \
  --restart unless-stopped \
  --env-file "$env_file" \
  --env ASPNETCORE_ENVIRONMENT=Production \
  --env ASPNETCORE_HTTP_PORTS=8080 \
  --publish 8080:8080 \
  --add-host host.docker.internal:host-gateway \
  --log-opt max-size=10m \
  --log-opt max-file=3 \
  "$image"

# 检查实际 HTTP 响应。匿名健康检查接口应返回 200；无需在服务器安装 curl。
# docker port 映射到宿主机 8080，使用 Linux Bash 的 /dev/tcp 发起请求。
ready=false
for attempt in {1..60}; do
  if timeout 3 bash -c '
    exec 3<>/dev/tcp/127.0.0.1/8080
    printf "GET /health/check HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n" >&3
    IFS= read -r status <&3
    [[ "$status" == HTTP/*" 200 "* ]]
  ' 2>/dev/null; then
    ready=true
    break
  fi
  sleep 2
done
if [ "$ready" != true ]; then
  echo 'New container did not become ready; restoring previous container.' >&2
  false
fi

trap - ERR
if [ "$has_previous" = true ]; then
  docker rm "$previous"
fi
echo "Deployed $image"
