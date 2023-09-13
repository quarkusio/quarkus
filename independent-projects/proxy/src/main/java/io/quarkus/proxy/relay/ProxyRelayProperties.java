package io.quarkus.proxy.relay;

import io.quarkus.proxy.common.Ssl;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.proxy.relay")
public interface ProxyRelayProperties {
    String listenAddress();

    Ssl.Server listenSsl();

    String upstreamAddress();

    Ssl upstreamSsl();
}
