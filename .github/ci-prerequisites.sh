# Reclaim disk space, otherwise we only have 13 GB free at the start of a job

docker rmi node:10 node:12 mcr.microsoft.com/azure-pipelines/node8-typescript:latest
# That is 18 GB
sudo rm -rf /usr/share/dotnet
# That is 1.2 GB
sudo rm -rf /usr/share/swift
