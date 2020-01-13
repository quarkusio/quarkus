package io.quarkus.spring.cloud.config.client.runtime;

import java.io.IOException;

interface SpringCloudConfigClientGateway {

    Response exchange(String applicationName, String profile) throws IOException;
}
