package io.quarkus.spring.cloud.config.client.runtime;

import io.smallrye.mutiny.Uni;

interface SpringCloudConfigClientGateway {

    Uni<Response> exchange(String applicationName, String profile) throws Exception;

    void close();
}
