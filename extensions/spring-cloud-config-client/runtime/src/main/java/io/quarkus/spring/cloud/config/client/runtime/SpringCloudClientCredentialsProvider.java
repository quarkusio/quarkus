package io.quarkus.spring.cloud.config.client.runtime;

import io.vertx.mutiny.ext.web.client.HttpRequest;

public interface SpringCloudClientCredentialsProvider {

    void addAuthenticationInfo(HttpRequest<?> request);

}
