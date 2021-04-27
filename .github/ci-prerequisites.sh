# Reclaim disk space, otherwise we only have 13 GB free at the start of a job

time docker rmi node:10 node:12 mcr.microsoft.com/azure-pipelines/node8-typescript:latest
docker image prune -a -f

# That is 18 GB
time sudo rm -rf /usr/share/dotnet
# That is 1.2 GB
time sudo rm -rf /usr/share/swift
