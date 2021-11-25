# Reclaim disk space, otherwise we only have 13 GB free at the start of a job

time docker rmi node:10 node:12
# That is 18 GB
time sudo rm -rf /usr/share/dotnet
# That is 1.2 GB
time sudo rm -rf /usr/share/swift
