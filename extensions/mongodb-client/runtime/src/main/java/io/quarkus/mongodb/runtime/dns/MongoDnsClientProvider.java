package io.quarkus.mongodb.runtime.dns;

import com.mongodb.spi.dns.DnsClient;
import com.mongodb.spi.dns.DnsClientProvider;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class MongoDnsClientProvider implements DnsClientProvider {
    @Override
    public DnsClient create() {
        return new MongoDnsClient();
    }
}
