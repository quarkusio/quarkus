#!/bin/bash

PASSWORD=Oracle23
echo "database_password=${PASSWORD}"
echo "connection_string_suffix=localhost:1521/freepdb1"
docker run --name oracle -d -p 1521:1521 -e ORACLE_PASSWORD=Oracle23Admin \
  -e APP_USER=quarkus_test \
  -e APP_USER_PASSWORD=$PASSWORD \
  --health-cmd healthcheck.sh \
  --health-interval 5s \
  --health-timeout 5s \
  --health-retries 10 \
  docker.io/gvenzl/oracle-free:23
HEALTHSTATUS=""
TIMEOUT=300
ELAPSED=0
until [ "$HEALTHSTATUS" == "healthy" ];
do
  if [ $ELAPSED -ge $TIMEOUT ]; then
    echo "Timeout waiting for Oracle Free to start after ${TIMEOUT}s"
    echo "Container logs:"
    docker logs oracle
    exit 1
  fi
  echo "Waiting for Oracle Free to start... (${ELAPSED}s/${TIMEOUT}s)"
  sleep 5
  ELAPSED=$((ELAPSED + 5))
  HEALTHSTATUS=$(docker inspect --format='' oracle 2>/dev/null || echo "starting")
  HEALTHSTATUS="`docker inspect -f '{{.State.Healthcheck.Status}}' oracle`"
  HEALTHSTATUS=${HEALTHSTATUS##+( )} #Remove longest matching series of spaces from the front
  HEALTHSTATUS=${HEALTHSTATUS%%+( )} #Remove longest matching series of spaces from the back
done
sleep 2;
echo "Oracle successfully started"
