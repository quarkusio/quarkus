@echo off

rem See remarks in docker-prune.sh
if "%GITHUB_ACTIONS%"== "true" (
  docker container prune -f || exit /b
  docker image prune -f || exit /b
  docker network prune -f || exit /b
  docker volume prune -f || exit /b
)
