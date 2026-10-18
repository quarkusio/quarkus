#!/bin/bash

while [ ! -f /etc/kafka/docker/ports ]; do
  sleep 0.1;
done;

sleep 0.1;

source /etc/kafka/docker/ports;
export KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:$PORT_9092;
/etc/kafka/docker/run
