package io.quarkus.mongodb.runtime;

import static io.quarkus.mongodb.runtime.dns.MongoDnsClient.DNS_LOG_ACTIVITY;
import static io.quarkus.mongodb.runtime.dns.MongoDnsClient.DNS_LOOKUP_TIMEOUT;
import static io.quarkus.mongodb.runtime.dns.MongoDnsClient.DNS_SERVER;
import static io.quarkus.mongodb.runtime.dns.MongoDnsClient.DNS_SERVER_PORT;
import static io.quarkus.mongodb.runtime.dns.MongoDnsClient.NATIVE_DNS_LOG_ACTIVITY;
import static io.quarkus.mongodb.runtime.dns.MongoDnsClient.NATIVE_DNS_LOOKUP_TIMEOUT;
import static io.quarkus.mongodb.runtime.dns.MongoDnsClient.NATIVE_DNS_SERVER;
import static io.quarkus.mongodb.runtime.dns.MongoDnsClient.NATIVE_DNS_SERVER_PORT;

import java.util.Map;

import io.smallrye.config.RelocateConfigSourceInterceptor;

public class MongoConfigSourceInterceptor extends RelocateConfigSourceInterceptor {
    public MongoConfigSourceInterceptor() {
        super(Map.of(DNS_LOG_ACTIVITY, NATIVE_DNS_LOG_ACTIVITY, DNS_LOOKUP_TIMEOUT, NATIVE_DNS_LOOKUP_TIMEOUT,
                DNS_SERVER, NATIVE_DNS_SERVER, DNS_SERVER_PORT, NATIVE_DNS_SERVER_PORT));
    }
}
