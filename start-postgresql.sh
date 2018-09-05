#!/usr/bin/env bash

# Starts a PostgreSQL instance with configuration matching the needs of the Hibernate ORM demo.
# - not starting as a daemon: quit with CTRL+C
# - self-destruct on quit
# - not persisting any data
# - requires port 5432 to be available
#
# Running PostgreSQL v.10 on RHEL7; supposedly matching what's being used in the Openshift catalogue

docker run --ulimit memlock=-1:-1 -it --net=host --rm=true --memory-swappiness=0 --name postgres-protean -e POSTGRESQL_USER=hibernate_orm_test -e POSTGRESQL_PASSWORD=hibernate_orm_test -e POSTGRESQL_DATABASE=hibernate_orm_test -p 5432:5432 rhscl/postgresql-10-rhel7