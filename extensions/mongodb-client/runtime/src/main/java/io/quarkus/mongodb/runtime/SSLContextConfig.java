package io.quarkus.mongodb.runtime;

import javax.net.ssl.SSLContext;

public interface SSLContextConfig {
    SSLContext getSSLContext();
}
