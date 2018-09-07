#!/bin/sh
cd /workspace
apt-get install wget
if [ -d .graal-install/graalvm-ce-1.0.0-rc6 ]; then
echo "Graal already present, exiting"
exit
fi
mkdir .graal-install
cd .graal-install
wget https://github.com/oracle/graal/releases/download/vm-1.0.0-rc6/graalvm-ce-1.0.0-rc6-linux-amd64.tar.gz
tar -xf graalvm-ce-1.0.0-rc6-linux-amd64.tar.gz
