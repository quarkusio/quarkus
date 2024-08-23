#!/bin/bash -e

MVN=../../../mvnw
DB_CONTAINER_NAME=mariadb-database-gen
# These should be the same as the username/password used by docker-maven-plugin in ./pom.xml
DB_NAME=hibernate_orm_test
DB_PORT=3308
DB_USER=hibernate_orm_test
DB_PASSWORD=hibernate_orm_test
DUMP_LOCATION=src/test/resources/db/migration/V1.0.0__orm5-6.sql

# Start container
$MVN docker:stop docker:build docker:start -Dstart-containers -Ddocker.containerNamePattern=$DB_CONTAINER_NAME
trap '$MVN docker:stop -Dstart-containers -Ddocker.containerNamePattern=$DB_CONTAINER_NAME' EXIT

# Generate database
$MVN -f ../database-generator clean install -Ddb-kind=mariadb
# We use features in Hibernate ORM that are timezone-sensitive, e.g. ZoneOffsetDateTime normalization
TZ=Europe/Paris
QUARKUS_DATASOURCE_JDBC_URL="jdbc:mariadb://localhost:$DB_PORT/$DB_NAME" \
QUARKUS_DATASOURCE_USERNAME="$DB_USER" \
QUARKUS_DATASOURCE_PASSWORD="$DB_PASSWORD" \
    java -jar ../database-generator/target/quarkus-app/quarkus-run.jar

# Update the dump
# https://stackoverflow.com/a/32611542/6692043
echo "Updating dump at '$DUMP_LOCATION'"
docker exec "$DB_CONTAINER_NAME" \
   sh -c 'exec mysqldump -hlocalhost -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" --compact --hex-blob' \
   > "$DUMP_LOCATION"