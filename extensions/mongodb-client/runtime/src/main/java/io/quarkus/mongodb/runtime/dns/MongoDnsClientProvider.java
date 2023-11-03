package io.quarkus.mongodb.runtime.dns;

import com.mongodb.spi.dns.DnsClient;
import com.mongodb.spi.dns.DnsClientProvider;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.core.Vertx;

@RegisterForReflection
public class MongoDnsClientProvider implements DnsClientProvider {

    public static volatile Vertx vertx;

    @Override
    public DnsClient create() {
        return new MongoDnsClient(vertx);
    }
}
