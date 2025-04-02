#!/bin/bash

while [ ! -f /tmp/ports ]; do
  sleep 0.1;
done;

sleep 0.1;

source /tmp/ports;
export KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:$PORT_9092;
/work/kafka
