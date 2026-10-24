#!/bin/sh
# Starts a Redis Sentinel deployment inside a single redis container: one master, one replica of it and the given
# sentinels monitoring the master under the name $MASTER_NAME (default mymaster), with a majority quorum.
#
# usage: start-sentinel.sh <master-port> <replica-port> <sentinel-port> <sentinel-port> [<sentinel-port>...]
#
# Every process binds and announces the same port on 127.0.0.1 (replica-announce-* for the data nodes, sentinel
# announce-* for the sentinels), so the addresses the sentinels hand out (SENTINEL GET-MASTER-ADDR-BY-NAME, SENTINEL
# REPLICAS, SENTINEL SENTINELS) are reachable both from the other processes, which share the container's loopback,
# and from a client on the Docker host, provided each port is published on the same host port.
# Prints "SENTINEL READY" once the replica is in sync and every sentinel sees the replica and the other sentinels,
# then keeps the container alive.
set -e

if [ $# -lt 4 ]; then
  echo "usage: $0 <master-port> <replica-port> <sentinel-port> <sentinel-port> [<sentinel-port>...]" >&2
  exit 1
fi

MASTER=$1
REPLICA=$2
shift 2
SENTINELS="$*"
NAME=${MASTER_NAME:-mymaster}
QUORUM=$(( $# / 2 + 1 ))
OTHERS=$(( $# - 1 ))

redis-server --port "$MASTER" --replica-announce-ip 127.0.0.1 --replica-announce-port "$MASTER" \
  --appendonly no --save "" --protected-mode no --dir /tmp --logfile "/tmp/redis-$MASTER.log" --daemonize yes
redis-server --port "$REPLICA" --replicaof 127.0.0.1 "$MASTER" --replica-announce-ip 127.0.0.1 \
  --replica-announce-port "$REPLICA" --appendonly no --save "" --protected-mode no --dir /tmp \
  --logfile "/tmp/redis-$REPLICA.log" --daemonize yes

for port in $SENTINELS; do
  cat > "/tmp/sentinel-$port.conf" <<CONF
port $port
dir /tmp
sentinel announce-ip 127.0.0.1
sentinel announce-port $port
sentinel monitor $NAME 127.0.0.1 $MASTER $QUORUM
sentinel down-after-milliseconds $NAME 2000
sentinel failover-timeout $NAME 10000
sentinel parallel-syncs $NAME 1
sentinel resolve-hostnames no
CONF
  redis-server "/tmp/sentinel-$port.conf" --sentinel --protected-mode no --logfile "/tmp/sentinel-$port.log" \
    --daemonize yes
done

# wait_for <shell condition> <failure message>: polls up to 30s, then prints the logs and fails the container
wait_for() {
  i=0
  until eval "$1" >/dev/null 2>&1; do
    i=$((i + 1))
    if [ $i -gt 300 ]; then
      echo "$2" >&2
      cat /tmp/*.log >&2
      exit 1
    fi
    sleep 0.1
  done
}

# value <sentinel-port> <field>: a field of SENTINEL MASTER $NAME (flat key/value lines)
value() {
  redis-cli -p "$1" sentinel master "$NAME" | grep -A1 "^$2\$" | tail -1
}

wait_for "redis-cli -p $MASTER ping | grep -q PONG" "The master on port $MASTER did not start"
wait_for "redis-cli -p $REPLICA info replication | grep -q master_link_status:up" "The replica on port $REPLICA did not sync"
for port in $SENTINELS; do
  wait_for "redis-cli -p $port ping | grep -q PONG" "The sentinel on port $port did not start"
  wait_for "[ \"\$(value $port num-slaves)\" = 1 ]" "The sentinel on port $port does not see the replica"
  wait_for "[ \"\$(value $port num-other-sentinels)\" = $OTHERS ]" "The sentinel on port $port does not see the other sentinels"
done

echo "SENTINEL READY"
exec tail -f /dev/null
