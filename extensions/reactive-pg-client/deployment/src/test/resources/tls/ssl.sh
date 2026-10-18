#!/bin/bash

# Borrowed from https://github.com/muccg/docker-postgres-ssl/blob/master/9.6/docker-entrypoint-initdb.d/devssl.sh

cp /server.crt "${PGDATA}"/server.crt
cp /server.key "${PGDATA}"/server.key
chmod og-rwx "${PGDATA}"/server.key
chown -R postgres:postgres "${PGDATA}"

# turn on ssl
sed -ri "s/^#?(ssl\s*=\s*)\S+/\1'on'/" "$PGDATA/postgresql.conf"
