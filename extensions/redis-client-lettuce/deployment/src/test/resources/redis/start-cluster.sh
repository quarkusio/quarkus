#!/bin/sh
# Starts a Redis cluster inside a single redis container: one redis-server per port given as an argument, all
# announcing 127.0.0.1, then `redis-cli --cluster create` with one replica per upstream node.
#
# Every node binds and announces the same port on 127.0.0.1, so the address a node advertises in the cluster
# topology (MOVED redirects, CLUSTER SLOTS/NODES) is reachable both from the other nodes, which share the
# container's loopback, and from a client on the Docker host, provided each port is published on the same host
# port. The ports must not exceed 55535: the cluster bus of a node listens on its port plus 10000.
# Prints "CLUSTER READY" once every node reports cluster_state:ok, then keeps the container alive.
set -e

if [ $# -lt 6 ]; then
  echo "usage: $0 <port> <port> <port> <port> <port> <port> [<port>...] (at least 3 upstream nodes with 1 replica each)" >&2
  exit 1
fi

NODES=""
for port in "$@"; do
  redis-server --port "$port" --cluster-enabled yes --cluster-config-file "nodes-$port.conf" \
    --cluster-node-timeout 5000 --cluster-announce-ip 127.0.0.1 --appendonly no --save "" \
    --protected-mode no --dir /tmp --logfile "/tmp/redis-$port.log" --daemonize yes
  NODES="$NODES 127.0.0.1:$port"
done

for port in "$@"; do
  i=0
  until redis-cli -p "$port" ping 2>/dev/null | grep -q PONG; do
    i=$((i + 1))
    if [ $i -gt 100 ]; then
      echo "The Redis node on port $port did not start:" >&2
      cat "/tmp/redis-$port.log" >&2
      exit 1
    fi
    sleep 0.1
  done
done

# shellcheck disable=SC2086
redis-cli --cluster create $NODES --cluster-replicas 1 --cluster-yes

for port in "$@"; do
  until redis-cli -p "$port" cluster info 2>/dev/null | grep -q 'cluster_state:ok'; do sleep 0.2; done
done

echo "CLUSTER READY"
exec tail -f /dev/null
