#!/bin/bash
while [ ! -f /testcontainers_env ]; do sleep 0.1; done;
source /testcontainers_env
add_env_setting_to_conf "dbms.connector.bolt.advertised_address" "${NEO4J_dbms_connector_bolt_advertised__address}" "${NEO4J_HOME}"
