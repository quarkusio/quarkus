# JAX-RS example using Graylog central log management

## Running the tests

By default, the tests of this module are disabled.

To run them, you first need to start a Graylog server and it's needed dependencies.

The following commands will launch MongoDB, Elasticsearch and Graylog using Docker Compose then create a UDP input.

```
# Launch Graylog and it's components (MongoDB and Elasticsearch)
docker-compose -f src/test/resources/docker-compose-graylog.yml up

# Create a GELF UDP input
curl -H "Content-Type: application/json" -H "Authorization: Basic YWRtaW46YWRtaW4=" -H "X-Requested-By: curl" -X POST -v -d \
'{"title":"udp input","configuration":{"recv_buffer_size":262144,"bind_address":"0.0.0.0","port":12201,"decompress_size_limit":8388608},"type":"org.graylog2.inputs.gelf.udp.GELFUDPInput","global":true}' \
http://localhost:9000/api/system/inputs
```

When everything is launched and ready, you can run the tests in a standard JVM with the following command:

```
mvn clean test -Dtest-gelf
```

Additionally, you can generate a native image and run the tests for this native image by adding `-Dnative`:

```
mvn clean integration-test -Dtest-gelf -Dnative
```

## Testing with ELK (Elasticsearch, Logstash, Kibana) aka the Elastic Stack

You can also run the test with an ELK cluster, in this case, the test will fail as it will try to access Graylog to validate 
that the events are correctly sent. So you should assert yourself that it works.

First, you need to create a Logstash pipeline file with a GELF input, we will put it inside ` $HOME/pipelines/gelf.conf`

```
input {
  gelf {
    port => 12201
  }
}
output {
  stdout {}
  elasticsearch {
    hosts => ["http://elasticsearch:9200"]
  }
}

```

Then you can use the following commands to run an ELK cluster using the provided docker compose:

```
# Launch ELK (Elasticsearch, Logstash, Kibana)
docker-compose -f src/test/resources/docker-compose-elk.yml up
```

Finally, run the test via `mvn clean install -Dtest-gelf -Dmaven.test.failure.ignore` and manually verify that the log
events has been pushed to ELK. You can use Kibana on http://localhost:5601/ to access those logs.


## Testing with EFK (Elasticsearch, Fluentd, Kibana)

You can also run the test with an EFK cluster, in this case, the test will fail as it will try to access Graylog to validate 
that the events are correctly sent. So you should assert yourself that it works.

First, you need to create a Fluentd image with the needed plugins (`fluent-plugin-elasticsearch` and `fluent-plugin-input-gelf`).
There is a Dockerfile inside `src/test/resources/fluentd` that can be used to create such image.
The `docker-compose-efk.yml` file used in this section uses this file to create the Fluentd container.

Then, you need to create a configuration file that will defines an input of GELF UDP and an output to Elasticsearch

```
<source>
  type gelf
  tag example.gelf
  bind 0.0.0.0
  port 12201
</source>

<match example.gelf>
  @type elasticsearch
  host elasticsearch
  port 9200
  logstash_format true
</match>
```

Then you can use the following commands to run an EFK cluster using the provided docker compose:

```
# Launch EFK (Elasticsearch, Fluentd, Kibana)
docker-compose -f src/test/resources/docker-compose-efk.yml up
```

Finally, run the test via `mvn clean install -Dtest-gelf -Dmaven.test.failure.ignore` and manually verify that the log
events has been pushed to EFK. You can use Kibana on http://localhost:5601/ to access those logs.